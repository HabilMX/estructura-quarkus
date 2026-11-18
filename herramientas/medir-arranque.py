"""Mide arranque hasta la primera respuesta real y RSS.

Uso: python3 -I medir.py <etiqueta> <corridas> -- <comando...>

Cada corrida: lanza el comando, y desde el instante del lanzamiento intenta cada 5 ms un
POST /v1/pedidos (que obliga a una llamada saliente al proveedor de inventario en 9090).
La primera respuesta 201 marca el tiempo. Luego lee el RSS del proceso con ps y lo apaga.
"""
import http.client
import json
import statistics
import subprocess
import sys
import time

etiqueta = sys.argv[1]
corridas = int(sys.argv[2])
comando = sys.argv[sys.argv.index("--") + 1:]
cuerpo = json.dumps({"correo": "ana@tienda.mx",
                     "lineas": [{"sku": "CAFE-250", "cantidad": 2}, {"sku": "TAZA-01", "cantidad": 1}]})


def rss_kb(pid):
    salida = subprocess.run(["ps", "-o", "rss=", "-p", str(pid)], capture_output=True, text=True).stdout
    return int(salida.strip())


tiempos, memorias = [], []
for i in range(corridas):
    t0 = time.perf_counter()
    proc = subprocess.Popen(comando, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    estado = None
    while True:
        if proc.poll() is not None:
            raise SystemExit(f"{etiqueta}: el proceso murió con {proc.returncode}")
        try:
            c = http.client.HTTPConnection("127.0.0.1", 8080, timeout=5)
            c.request("POST", "/v1/pedidos", body=cuerpo, headers={"Content-Type": "application/json"})
            r = c.getresponse()
            r.read()
            estado = r.status
            c.close()
            break
        except (ConnectionRefusedError, ConnectionResetError, http.client.RemoteDisconnected, OSError):
            time.sleep(0.005)
    t = (time.perf_counter() - t0) * 1000
    if estado != 201:
        proc.kill()
        raise SystemExit(f"{etiqueta}: la primera respuesta fue {estado}, no 201")
    m = rss_kb(proc.pid) / 1024
    proc.terminate()
    proc.wait(timeout=30)
    tiempos.append(t)
    memorias.append(m)
    print(f"{etiqueta} corrida {i + 1}: primera respuesta {t:.0f} ms (HTTP {estado}), RSS {m:.1f} MiB", flush=True)
    time.sleep(1)

print(f"{etiqueta} MEDIANA de {corridas}: {statistics.median(tiempos):.0f} ms · RSS {statistics.median(memorias):.1f} MiB")
print(f"{etiqueta} tiempos={[round(x) for x in tiempos]} rss={[round(x, 1) for x in memorias]}")
