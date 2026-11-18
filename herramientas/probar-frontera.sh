#!/usr/bin/env bash
# La frontera que sabe fallar: comprueba que la prueba de arquitectura de verdad
# detecta una flecha hacia afuera, y que la detecta por la razón exacta.
#
# Qué hace, en una COPIA temporal del proyecto (tu carpeta no se toca):
#
#   Corre la misma prueba, CapasTest, cinco veces. Entre una y otra solo cambia
#   un método sembrado en una clase.
#
#   1. CONTROL. Con el proyecto tal como está. CapasTest tiene que correr y
#      pasar: al menos una regla, cero fallas. Si no, el experimento está mal
#      montado (falta Java 25, no hay red para bajar las dependencias) o tu
#      proyecto ya trae una flecha hacia afuera, y lo demás no probaría nada:
#      se para en rojo.
#   2. VIOLACIÓN. Se siembra en PedidoService (el cocinero) un método que recibe
#      un PedidoResource (el mesero). Tiene que COMPILAR: para Java no hay nada
#      malo. Y CapasTest tiene que FALLAR, en una sola regla y por la razón
#      exacta.
#   3, 4 y 5. FUGAS. Se siembra en PedidoResource (el mesero) un método que
#      entrega la ficha del archivo (PedidoEntity): primero tal cual, después
#      escondida en una List<PedidoEntity> y al final en una
#      RestResponse<PedidoEntity>. Las tres compilan, y en las tres CapasTest
#      tiene que fallar en una sola regla y por la razón exacta: la ficha nunca
#      sale por la puerta.
#
#   Si una siembra pasa, esa parte de la frontera no existe. Si no compila, o
#   falla por otra cosa, tampoco demuestra nada. En los tres casos sale en rojo.
#
# Uso:  bash herramientas/probar-frontera.sh   → sale 0 si la frontera se sostiene
#
# Por qué así: un experimento que solo espera un rojo se pone «verde» también
# cuando falla por la razón equivocada. Lo impiden tres cosas: el control
# demuestra que la prueba corre y pasa con el proyecto limpio; cada siembra
# exige que el código compile; y el resultado se lee del informe de Surefire
# (cuántas reglas corrieron, cuántas fallaron y con qué mensaje), no del código
# de salida de Maven.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
prueba="mx.habil.pedidos.arquitectura.CapasTest"
cocinero="src/main/java/mx/habil/pedidos/service/PedidoService.java"
mesero_java="src/main/java/mx/habil/pedidos/api/PedidoResource.java"
mesero="mx.habil.pedidos.api.PedidoResource"
ficha="mx.habil.pedidos.model.entity.PedidoEntity"

tmp="$(cd "$(mktemp -d)" && pwd -P)"
# Al salir se borra la copia, y el código de salida se conserva. Con el bash
# 3.2 que trae macOS, si el guion se corta por una variable sin definir
# (set -u), la trampa recibe un 0 en «$?»: sin la bandera «terminado», un
# guion cortado a la mitad saldría 0, un falso verde. Por eso, si no llegó al
# final, sale 1 aunque «$?» diga 0.
salir() {
  local codigo=$?
  rm -rf "$tmp"
  if [ "$codigo" -eq 0 ] && [ "${terminado:-}" != 1 ]; then
    echo "❌ el guion se cortó antes de terminar" >&2
    codigo=1
  fi
  exit "$codigo"
}
trap salir EXIT

if [ ! -f "$raiz/mvnw" ] || [ ! -f "$raiz/$cocinero" ] || [ ! -f "$raiz/$mesero_java" ]; then
  echo "❌ no encuentro mvnw, ${cocinero} o ${mesero_java}: corre el guion desde la plantilla"
  exit 1
fi

# La copia lleva todo menos lo compilado y la historia de git.
copia="$tmp/copia"
mkdir -p "$copia"
(cd "$raiz" && tar --exclude=./target --exclude=./.git -cf - .) | (cd "$copia" && tar -xf -)

informe="$copia/target/surefire-reports/TEST-${prueba}.xml"
detalle="$copia/target/surefire-reports/${prueba}.txt"

# Compila el código y las pruebas, sin correrlas. Deja la salida en $1.
compilar() { (cd "$copia" && sh ./mvnw -q -B test-compile >"$1" 2>&1); }
# Corre solo la prueba de arquitectura. Deja la salida en $1. El código de
# salida no se usa: lo que cuenta es el informe.
probar() { (cd "$copia" && rm -rf target/surefire-reports && sh ./mvnw -q -B test -Dtest=CapasTest -Djacoco.skip=true >"$1" 2>&1) || true; }
# Lee un número del encabezado del informe: tests, failures, errors o skipped.
cuenta() { sed -n "s/.*<testsuite [^>]* $1=\"\([0-9][0-9]*\)\".*/\1/p" "$informe" | head -n 1; }
# Muestra un registro sin la ruta de la carpeta temporal, que cambia en cada corrida.
mostrar() { sed -e "s#$copia#…#g" -e 's/^/     /' "$1" | head -n 25; }

echo "1/5 control: el proyecto tal como está; el cocinero no conoce al mesero y el mesero no entrega la ficha"
if ! compilar "$tmp/control-compila.log"; then
  echo "❌ el control NO compiló; el experimento está mal montado:"
  mostrar "$tmp/control-compila.log"
  exit 1
fi
probar "$tmp/control-prueba.log"
if [ ! -f "$informe" ]; then
  echo "❌ el control no dejó informe: CapasTest no corrió. El experimento está mal montado:"
  mostrar "$tmp/control-prueba.log"
  exit 1
fi
corrieron="$(cuenta tests)"
if [ "${corrieron:-0}" -lt 1 ] || [ "$(cuenta failures)" != 0 ] || [ "$(cuenta errors)" != 0 ] ||
  [ "$(cuenta skipped)" != 0 ]; then
  echo "❌ el control NO pasó: tu proyecto, tal como está, ya rompe una regla de CapasTest:"
  mostrar "$detalle"
  exit 1
fi
if grep -F -q "$mesero" "$copia/$cocinero" || grep -F -q "$ficha" "$copia/$mesero_java"; then
  echo "❌ el cocinero ya menciona al mesero, o el mesero a la ficha; el experimento está mal montado"
  exit 1
fi
echo "   ✅ compiló y CapasTest pasó: ${corrieron} reglas, 0 fallas"

# Siembra un método al final de una clase, en la copia, y exige el rojo exacto.
#   $1 número del paso · $2 qué se siembra (para leerlo) · $3 archivo · $4 el método
#   $5 la regla que tiene que fallar · $6 el mensaje exacto de ArchUnit
sembrar() {
  local paso="$1" que="$2" archivo="$3" metodo="$4" regla="$5" esperado="$6"
  echo "${paso}/5 ${que}"
  # Cada siembra parte del original: la anterior no se arrastra.
  cp "$raiz/$cocinero" "$copia/$cocinero"
  cp "$raiz/$mesero_java" "$copia/$mesero_java"
  if [ "$(tail -n 1 "$raiz/$archivo")" != "}" ]; then
    echo "❌ ${archivo} ya no termina en «}»; no se pudo sembrar. El experimento está mal montado"
    exit 1
  fi
  sed '$d' "$raiz/$archivo" >"$copia/$archivo"
  printf '\n    %s\n}\n' "$metodo" >>"$copia/$archivo"
  if ! compilar "$tmp/paso-${paso}-compila.log"; then
    echo "❌ la siembra NO compiló. Esto no demuestra la frontera:"
    mostrar "$tmp/paso-${paso}-compila.log"
    exit 1
  fi
  echo "   ✅ compiló sin una queja: para Java no hay nada malo"
  probar "$tmp/paso-${paso}-prueba.log"
  if [ ! -f "$informe" ]; then
    echo "❌ la siembra no dejó informe: CapasTest no corrió. Esto no demuestra la frontera:"
    mostrar "$tmp/paso-${paso}-prueba.log"
    exit 1
  fi
  if [ "$(cuenta failures)" = 0 ] && [ "$(cuenta errors)" = 0 ]; then
    echo "❌ PASÓ: CapasTest no vio la siembra. Esa parte de la frontera no existe."
    exit 1
  fi
  if [ "$(cuenta tests)" != "$corrieron" ] || [ "$(cuenta failures)" != 1 ] || [ "$(cuenta errors)" != 0 ] ||
    ! grep -F -q "${regla}" "$detalle" || ! grep -F -q "Architecture Violation" "$detalle" ||
    ! grep -F -q "$esperado" "$detalle"; then
    echo "❌ falló, pero NO por la siembra. Esto no demuestra la frontera:"
    mostrar "$detalle"
    exit 1
  fi
  grep -F "$esperado" "$detalle" | head -n 1 | sed -e 's/ in (.*$//' -e 's/^/     /'
  echo "   ✅ CapasTest falló en una sola regla, ${regla}"
}

sembrar 2 "violación: se siembra en PedidoService un método que recibe un PedidoResource" \
  "$cocinero" \
  "public void avisar(${mesero} mesero) { }" \
  "lasFlechasSoloVanHaciaAdentro" \
  "Method <mx.habil.pedidos.service.PedidoService.avisar(${mesero})> has parameter of type <${mesero}>"

sembrar 3 "fuga: se siembra en PedidoResource un método que entrega la PedidoEntity tal cual" \
  "$mesero_java" \
  "public ${ficha} ficha(java.util.UUID id) { return service.buscar(id); }" \
  "elMeseroNoConoceLaFichaDelArchivo" \
  "Method <${mesero}.ficha(java.util.UUID)> has return type <${ficha}>"

sembrar 4 "fuga: la misma PedidoEntity, escondida en una List<PedidoEntity>" \
  "$mesero_java" \
  "public java.util.List<${ficha}> fichas(java.util.UUID id) { return java.util.List.of(service.buscar(id)); }" \
  "elMeseroNoConoceLaFichaDelArchivo" \
  "Method <${mesero}.fichas(java.util.UUID)> has generic return type <java.util.List<${ficha}>> with type argument depending on <${ficha}>"

sembrar 5 "fuga: la misma PedidoEntity, escondida en una RestResponse<PedidoEntity>" \
  "$mesero_java" \
  "public org.jboss.resteasy.reactive.RestResponse<${ficha}> respuesta(java.util.UUID id) { return org.jboss.resteasy.reactive.RestResponse.ok(service.buscar(id)); }" \
  "elMeseroNoConoceLaFichaDelArchivo" \
  "Method <${mesero}.respuesta(java.util.UUID)> has generic return type <org.jboss.resteasy.reactive.RestResponse<${ficha}>> with type argument depending on <${ficha}>"

echo "✅ frontera: se sostiene (la violación y las tres fugas salieron en rojo, cada una por su razón)"
terminado=1
