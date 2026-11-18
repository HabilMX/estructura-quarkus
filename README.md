> El código es MIT; el texto y las explicaciones son CC BY-SA 4.0.

# estructura-quarkus

**Una plantilla de una API con Quarkus que se clona, corre a la primera y te dice qué va en cada
paquete**: un paquete por papel, las dependencias en un solo sentido y una prueba que se pone roja
cuando alguien las voltea.

Es **el mismo restaurante, con otra cocina**. La entrega anterior de la serie armó esta misma API
con Spring Boot: los mismos pedidos, las mismas reglas, los mismos errores y los mismos paquetes.
Aquí cambia el marco, Quarkus, y con él cambian algunas piezas: el mesero se llama *resource*, la
despensa se escribe con Panache, la salud se mudó a otro puerto. La sección
[«Lo que cambió respecto de Spring Boot»](#lo-que-cambió-respecto-de-spring-boot) las junta.

Es una **semilla**, no un molde completo: trae una funcionalidad real de punta a punta (recibir y
consultar los pedidos de una tienda), sus pruebas y un CI que comprueba una frontera que el
compilador de Java no vigila: quién puede conocer a quién. Lo demás crece cuando duela, y la
sección [«Cómo crece»](#cómo-crece-cuándo-agregar-cada-cosa) dice cuándo.

Es parte de la serie «La estructura de un proyecto», de Hábil. Si Java es nuevo para ti, empieza
por el [curso de Java](https://www.habil.mx/es/cursos/java/).

---

## Córrelo

Necesitas Java 25 (`java -version`), Git, `bash` y `curl`. Maven no hace falta: lo trae el
proyecto (`./mvnw`).

- **Linux Mint 22, Ubuntu o macOS**: instala Java con [SDKMAN](https://sdkman.io/), que no toca el
  Java del sistema.

  ```bash
  sudo apt install curl zip unzip        # solo en Linux
  curl -s "https://get.sdkman.io" | bash
  ```

  Abre otra terminal, entra a la carpeta de la plantilla y pide el Java que declara `.sdkmanrc`:

  ```bash
  sdk env install
  ```

  Abre una terminal más, vuelve a la carpeta y actívalo:

  ```bash
  sdk env
  java -version
  ```

  Debe contestar `openjdk version "25.0.5"` y `Temurin-25.0.5+8`. Cualquier Java 25 sirve. Con
  otra línea, la construcción se detiene y te lo dice. Con Java 21 contestó (medido):

  ```
  [ERROR] Esta plantilla se construye con Java 25. Instálalo con SDKMAN: «sdk env install».
  ```
- **Windows**: usa WSL; el guion de la frontera pide `bash`.

**1. Construye y corre las pruebas**:

```bash
./mvnw verify
```

La primera vez tarda: baja Maven y las bibliotecas. Compila, corre las pruebas, mide la cobertura
y exige su piso. Las líneas que importan, cerca del final:

```
[INFO] Tests run: 82, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Son 82 pruebas. Cuéntalas en esa línea o en los informes de `target/surefire-reports/`, no en el
`BUILD SUCCESS`: una clase de pruebas que no arranca deja sus pruebas como saltadas (`Skipped`), y
el total no baja.

**2. Comprueba la frontera**:

```bash
bash herramientas/probar-frontera.sh
```

Termina en `✅ frontera: se sostiene` (más abajo se explica qué hace). Tarda medio minuto.

**3. Úsalo.** Necesitas dos terminales. En la primera, el proveedor de inventario de ejemplo: una
carpeta con dos archivos JSON, servida con `jwebserver`, que ya trae el JDK:

```bash
jwebserver -b 127.0.0.1 -p 9090 -d "$PWD/ejemplo/inventario"
```

En la segunda, la API. Quarkus no deja un solo `.jar`: deja una carpeta, `target/quarkus-app/`, y
se arranca por su puerta:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

Contesta así (la hora y los milisegundos cambian en cada corrida):

```
INFO  [] [io.quarkus] pedidos 0.1.0 on JVM (powered by Quarkus 3.40.1.2) started in 1.540s. Listening on: http://0.0.0.0:8080. Management interface listening on http://0.0.0.0:9000.
```

Dos puertos: en el 8080, la API; en el 9000, la salud y las métricas. Pide un pedido desde una
tercera terminal:

```bash
curl -s -i -X POST localhost:8080/v1/pedidos \
  -H 'Content-Type: application/json' \
  -d '{"correo": "ana@tienda.mx", "lineas": [{"sku": "CAFE-250", "cantidad": 2}, {"sku": "TAZA-01", "cantidad": 1}]}'
```

Lo que contestó el 18-nov-2026 (el `id`, la hora y el `X-Request-ID` cambian en cada corrida; el
JSON sale en una sola línea, aquí va partido para leerlo):

```
HTTP/1.1 201 Created
content-length: 248
Content-Type: application/json;charset=UTF-8
Location: /v1/pedidos/42f9545b-2ee5-44a1-be64-d5f7fbe8dd61
X-Request-ID: f4bed552-2786-4939-bac6-6dc56ac2588e

{"id":"42f9545b-2ee5-44a1-be64-d5f7fbe8dd61","estado":"RECIBIDO","total_centavos":42400,
 "creado_en":"2026-11-18T06:45:16.059Z",
 "lineas":[{"sku":"CAFE-250","cantidad":2,"precio_centavos":18950},
           {"sku":"TAZA-01","cantidad":1,"precio_centavos":4500}]}
```

El precio no venía en lo que mandaste: lo puso el proveedor. Y en la bitácora de la API, el correo
sale tapado:

```
INFO  [f4bed552-2786-4939-bac6-6dc56ac2588e] [mx.ha.pe.se.PedidoService] pedido 42f9545b-… recibido de a***@tienda.mx por $424.00
```

Ahora los errores. Todos salen con la misma forma, Problem Details (`application/problem+json`):

```bash
# Un pedido que no existe
curl -s localhost:8080/v1/pedidos/00000000-0000-0000-0000-000000000000

# Más piezas de las que hay (de TAZA-01 hay 3)
curl -s -X POST localhost:8080/v1/pedidos -H 'Content-Type: application/json' \
  -d '{"correo": "ana@tienda.mx", "lineas": [{"sku": "TAZA-01", "cantidad": 4}]}'
```

```json
{"type":"https://pedidos.example/errors/pedido_no_encontrado","status":404,
 "title":"El pedido no existe",
 "detail":"No hay un pedido con el identificador 00000000-0000-0000-0000-000000000000",
 "instance":"/v1/pedidos/00000000-0000-0000-0000-000000000000",
 "error_code":"PEDIDO_NO_ENCONTRADO","request_id":"c9937ce9-6da5-4ebe-8971-a61cefa34d42"}
```

```json
{"type":"https://pedidos.example/errors/sin_existencias","status":409,
 "title":"No hay existencias suficientes","detail":"De TAZA-01 se pidieron 4 y hay 3",
 "instance":"/v1/pedidos","error_code":"SIN_EXISTENCIAS",
 "request_id":"e8cc3ea5-44a3-47ef-8e39-275e4e584861"}
```

Cada error lleva el mismo `request_id` que la cabecera `X-Request-ID` y que la bitácora: con él se
encuentra la visita.

Si mandas una comanda mal llenada (un correo que no es correo, cero piezas), la respuesta es un
`422` con la lista `violations`: qué campo falló y por qué. En una máquina en español, los
mensajes salen en español (medido):

```json
"violations":[{"field":"correo","in":"body","message":"debe ser una dirección de correo electrónico con formato correcto"},
              {"field":"lineas[0].cantidad","in":"body","message":"debe ser mayor que 0"}, …]
```

Apaga el proveedor (Ctrl+C en la primera terminal) y repite el primer `curl`: contesta `503`, con
`"error_code":"PROVEEDOR_NO_DISPONIBLE"`, después de intentarlo tres veces (medio segundo, medido:
`0.534s`).

La salud de la API **no** está en el 8080: `curl -s localhost:8080/q/health` contesta `404`. Está
en el puerto de administración:

```bash
curl -s localhost:9000/q/health/ready
```

```json
{"status":"UP","checks":[{"name":"Database connections health check","status":"UP","data":{"<default>":"UP"}}]}
```

Las métricas, en el mismo puerto: `curl -s localhost:9000/q/metrics`. Ahí se ven, por ejemplo,
cuántas veces se reintentó al proveedor (`ft_retry_retries_total`).

La base de datos es H2, en memoria: nace vacía al arrancar y se borra al apagar. Es lo más simple
que corre a la primera; cuándo cambiarla está en «Cómo crece».

---

## Qué es cada carpeta y por qué existe

```
estructura-quarkus/
├── pom.xml                          qué bibliotecas usa y cómo se construye
├── mvnw · mvnw.cmd · .mvn/          el Maven del proyecto: nadie instala Maven
├── .sdkmanrc                        el Java con que se probó
├── Dockerfile                       la imagen: construye y corre sin root
├── docs/ARCHITECTURE.md             las capas, las flechas y las decisiones
├── ejemplo/inventario/              el proveedor de ejemplo que usa «Córrelo»
├── herramientas/
│   ├── probar-frontera.sh           demuestra que la prueba de arquitectura sabe fallar
│   └── medir-arranque.py            mide el arranque y la memoria («Arranque y memoria»)
├── src/main/java/mx/habil/pedidos/
│   ├── api/                         el mesero
│   ├── facade/                      el jefe de cocina
│   ├── service/                     el cocinero
│   ├── client/                      el proveedor de afuera
│   ├── model/                       los papeles que pasan de mano en mano
│   │   ├── request/                 la comanda
│   │   ├── response/                el ticket
│   │   ├── entity/                  la ficha del archivo
│   │   ├── repository/              la despensa, junto a la ficha que guarda
│   │   └── vo/                      las medidas exactas
│   ├── mapper/                      el que pasa en limpio
│   ├── config/                      la instalación del local
│   ├── exception/                   el libro de quejas
│   ├── observability/               las cámaras y la bitácora
│   └── utils/                       el cajón de herramientas
├── src/main/resources/
│   └── application.yaml             toda la configuración, en un archivo
└── src/test/java/mx/habil/pedidos/  el mismo árbol: cada prueba vive junto a lo que prueba
    └── arquitectura/CapasTest.java  la que vigila las flechas
```

No hay una clase con `main`: Quarkus la escribe al construir. La puerta del local es
`quarkus-run.jar`.

Piensa en un restaurante. Cada paquete es un papel, y cada papel tiene algo que **nunca** hace.

| Paquete | En el restaurante | En la plantilla | Lo que NUNCA va ahí |
|---|---|---|---|
| `api` | El mesero: recibe la comanda, revisa que venga bien llenada y la pasa | `PedidoResource`: rutas, verbos, `@Valid`, el `201` con su `Location` | Una regla del negocio. Un `if` sobre un dato del pedido |
| `facade` | El jefe de cocina: coordina, solo si intervienen dos o más | `PedidoFacade.crear`: pregunta al proveedor, pasa al cocinero, entrega el ticket | Cálculos, reglas o consultas a la base. Un método que solo llama a otro |
| `service` | El cocinero: las reglas | `PedidoService`: «no se acepta si falta una pieza» y «el total es precio por cantidad»; abre la transacción | HTTP y JSON: ni `request`, ni `response`, ni códigos de estado |
| `model/repository` | La despensa: guarda y trae | `PedidoRepository`: una clase vacía que hereda de Panache `persist` y `findByIdOptional` | Decisiones. Si un pedido se puede guardar lo dice el cocinero |
| `client` | El proveedor de afuera, con tiempo límite y reintento | `InventarioApi` (la llamada y su política) e `InventarioClient` (traduce las fallas) | Reglas del pedido. Y una llamada sin tiempo límite |
| `model/request` | La comanda: lo que el cliente pide | `CrearPedidoRequest`: un `record` con `@NotBlank`, `@Email`, `@Positive` | Lógica. Y campos que el cliente no debe decidir, como el precio |
| `model/response` | El ticket: lo que el cliente recibe | `PedidoResponse`: un `record` con lo que el cliente necesita ver | Datos que nadie pidió, como el correo. Anotaciones de la base |
| `model/entity` | La ficha del archivo: lo que se guarda | `PedidoEntity` y `LineaEntity`: la forma de las tablas | Salir por la puerta: el mesero entrega el ticket, nunca la ficha. Y guardarse sola: no hereda de `PanacheEntity` |
| `model/vo` | La medida exacta | `Dinero`, `Correo`, `LineaCotizada`: si existen, ya son válidos | Dependencias de Quarkus, de la base o de otra capa |
| `mapper` | El que pasa en limpio | `PedidoMapper`: copia de la ficha al ticket | Un `if` que decida algo del negocio |
| `config` | La instalación del local | `InstalacionConfig`: el reloj | Reglas del negocio y rutas HTTP |
| `exception` | El libro de quejas | `CodigoError` (el catálogo), `PedidosException`, su `ExceptionMapper` y `CatalogoDeQuejas` | Un formato de error propio. Un error armado fuera del catálogo |
| `observability` | Las cámaras y la bitácora | `CorrelacionFilter`: un número por visita, en cada línea de la bitácora y en cada error | Datos personales en claro |
| `utils` | El cajón de herramientas | `Mascara.correo`: `a***@tienda.mx` | Algo que sepa qué es un pedido. Si lo sabe, tiene otro paquete |

`vo` quiere decir *value object*, objeto de valor: un dato pequeño que se define por lo que vale,
no por un identificador. Dos `Dinero` de 100 centavos son el mismo dinero.

### El camino de un pedido

```
POST /v1/pedidos                              GET /v1/pedidos/{id}
       │                                             │
       ▼                                             ▼
  api (mesero)   revisa la comanda              api (mesero)
       │                                             │
       ▼                                             │  el atajo: lo atiende un
  facade (jefe de cocina)                            │  solo servicio, no hay
       │                                             │  nada que coordinar
       ├──► client (proveedor)  ¿hay? ¿a cuánto?     │
       │                                             │
       ▼                                             ▼
  service (cocinero)   las reglas  ◄─────────────────┘
       │
       ▼
  model/repository (despensa)   guarda la ficha
```

De regreso, la ficha (`entity`) se pasa en limpio (`mapper`) y sale el ticket (`response`). La
pregunta al proveedor va **antes** de abrir la transacción: si el proveedor tarda, la base no se
queda esperando.

### Las tres reglas

1. **Cada quien en lo suyo.** Si no sabes dónde va una línea de código, pregunta de quién es el
   trabajo: ¿del mesero, del cocinero, de la despensa?
2. **Las flechas solo van hacia adentro.** `api → facade → service → model/repository` o `client`.
   El cocinero nunca llama al mesero.
3. **Lo que entra, lo que se guarda y lo que sale son cosas distintas.** `request`, `entity` y
   `response`: tres clases, aunque hoy se parezcan. Mañana la tabla gana una columna y el JSON no
   tiene por qué enterarse.

### ¿Cuándo sí y cuándo no la facade?

**Sí**, cuando una operación coordina a dos o más servicios o clientes. Crear un pedido necesita al
proveedor de inventario y al cocinero, en ese orden: `PedidoFacade` dice el orden, y nada más.

**No**, cuando la atiende un solo servicio. Consultar un pedido es una sola llamada:
`PedidoResource` va directo a `PedidoService`. Una facade cuyo método solo llama a otro método no
coordina nada: es un archivo más que tocar en cada cambio.

Ninguna prueba te va a avisar de una facade que sobra: las flechas quedan bien. Eso se decide
leyendo, con la regla de dos.

### Lo demás

**`pom.xml`.** Importa el BOM de Quarkus (`quarkus-bom`), que trae las versiones de las
bibliotecas ya probadas entre sí; por eso casi ninguna dependencia declara `<version>`. La versión
de Quarkus vive en una sola propiedad, `quarkus.platform.version`. Tres cuidados: el Enforcer
detiene la construcción si el Java no es 25; Surefire falla si una corrida termina con cero
pruebas; y JaCoCo exige que las pruebas cubran al menos el 84 % de las instrucciones.

**La cobertura, en dos archivos.** Las pruebas que levantan Quarkus (`@QuarkusTest`) las mide su
extensión, `quarkus-jacoco`, y las escribe en `target/jacoco-quarkus.exec`; las demás, el agente
de JaCoCo, en `target/jacoco.exec`. `./mvnw verify` junta los dos antes de exigir el piso. Sin
juntarlos, el piso se medía solo con la mitad: dio 0.78 y la construcción falló, con las 82 pruebas
en verde (medido).

**`application.yaml`.** Toda la configuración en un archivo. Quarkus lee `application.properties`
de fábrica; el YAML lo lee gracias a la extensión `quarkus-config-yaml`. Lo que cambia entre
ambientes se lee de una variable con su valor para desarrollo:
`${INVENTARIO_URL:http://localhost:9090}`. Lo que solo vale para las pruebas va bajo `"%test"`.
Un cuidado: si una llave está mal escrita, Quarkus **no** falla, la ignora (y lo avisa con un
`Unrecognized configuration key` al arrancar).

**El tiempo límite y el reintento del proveedor** se leen juntos, en `InventarioApi`:

```java
@Timeout(2000)
@Retry(maxRetries = 2, delay = 200, jitter = 50,
        retryOn = {TimeoutException.class, ProcessingException.class})
@ExponentialBackoff(factor = 2)
```

Dos segundos por intento, tres intentos en total, 200 ms de espera antes del segundo y el doble
antes del tercero, cada espera con ±50 ms al azar. Se reintenta solo si el proveedor no contestó;
un `404` es una respuesta, y no se repite. Las pruebas bajan esos tiempos en `"%test"`, y la llave
tiene una trampa: no es `InventarioApi/producto`, es `InventarioApi$$CDIWrapper/producto`. Con la
primera, la llave se ignora en silencio, sin el aviso de `Unrecognized configuration key`, porque
ese aviso solo revisa las llaves que empiezan con `quarkus.` (medido).

**El libro de quejas.** La extensión `quarkus-http-problem` contesta con Problem Details los
errores del marco (JSON mal escrito, validación, ruta que no existe), pero en inglés y sin
`error_code`. `CatalogoDeQuejas` los reescribe con el texto del catálogo, para que todo error, del
marco o del negocio, salga de `CodigoError`.

**`src/test/`.** El mismo árbol que `src/main/`. Cada capa se prueba con lo que necesita, y no más:

| Prueba | Qué levanta |
|---|---|
| `vo`, `utils`, `mapper`, `exception`, `service`, `facade`, `observability` | Nada: Java y, si hace falta, Mockito |
| `client/InventarioClientTest` | Quarkus y un servidor HTTP de verdad, el del JDK, en `127.0.0.1` |
| `model/repository/PedidoRepositoryTest` | Quarkus sobre H2; cada prueba deshace lo que escribió (`@TestTransaction`) |
| `api/PedidoResourceTest` | Quarkus, con la cocina de mentiras (`@InjectMock`) |
| `PedidosDePuntaAPuntaTest` | Todo; el único de mentiras es el proveedor |
| `arquitectura/CapasTest` | Nada: lee las clases compiladas |

Quarkus arranca una sola vez para todas las pruebas que comparten configuración. Ninguna prueba
levanta un contenedor ni sale a internet.

---

## La frontera que sabe fallar

Para Java, todas las clases de este proyecto pueden importarse entre sí. Si el cocinero importa al
mesero, compila. La regla «las flechas solo van hacia adentro» la vigila una prueba,
`CapasTest`, escrita con [ArchUnit](https://www.archunit.org/): lee las clases ya compiladas y
falla cuando una flecha apunta hacia afuera.

Pero una prueba que siempre pasa no demuestra nada. `herramientas/probar-frontera.sh` comprueba que
`CapasTest` sabe fallar. Trabaja en una copia temporal del proyecto (tu carpeta no se toca) y corre
la misma prueba cinco veces:

1. **Control**, con el proyecto tal como está. `CapasTest` tiene que correr y pasar. Si no, el
   experimento está mal montado, o tu proyecto ya trae una flecha hacia afuera, y el guion para en
   rojo.
2. **Violación**: se siembra en `PedidoService` un método que recibe un `PedidoResource`. Tiene que
   **compilar**, y `CapasTest` tiene que **fallar**, en una sola regla y por la razón exacta.
3. **Tres fugas**: se siembra en `PedidoResource` un método que entrega la ficha del archivo,
   `PedidoEntity`. Primero tal cual; después escondida en una `List<PedidoEntity>`; al final en
   una `RestResponse<PedidoEntity>`, la respuesta con tipo de Quarkus. Las tres compilan, y en las
   tres `CapasTest` tiene que fallar en una sola regla.

Ésta es la salida real:

```
1/5 control: el proyecto tal como está; el cocinero no conoce al mesero y el mesero no entrega la ficha
   ✅ compiló y CapasTest pasó: 4 reglas, 0 fallas
2/5 violación: se siembra en PedidoService un método que recibe un PedidoResource
   ✅ compiló sin una queja: para Java no hay nada malo
     Method <mx.habil.pedidos.service.PedidoService.avisar(mx.habil.pedidos.api.PedidoResource)> has parameter of type <mx.habil.pedidos.api.PedidoResource>
   ✅ CapasTest falló en una sola regla, lasFlechasSoloVanHaciaAdentro
3/5 fuga: se siembra en PedidoResource un método que entrega la PedidoEntity tal cual
   ✅ compiló sin una queja: para Java no hay nada malo
     Method <mx.habil.pedidos.api.PedidoResource.ficha(java.util.UUID)> has return type <mx.habil.pedidos.model.entity.PedidoEntity>
   ✅ CapasTest falló en una sola regla, elMeseroNoConoceLaFichaDelArchivo
4/5 fuga: la misma PedidoEntity, escondida en una List<PedidoEntity>
   ✅ compiló sin una queja: para Java no hay nada malo
     Method <mx.habil.pedidos.api.PedidoResource.fichas(java.util.UUID)> has generic return type <java.util.List<mx.habil.pedidos.model.entity.PedidoEntity>> with type argument depending on <mx.habil.pedidos.model.entity.PedidoEntity>
   ✅ CapasTest falló en una sola regla, elMeseroNoConoceLaFichaDelArchivo
5/5 fuga: la misma PedidoEntity, escondida en una RestResponse<PedidoEntity>
   ✅ compiló sin una queja: para Java no hay nada malo
     Method <mx.habil.pedidos.api.PedidoResource.respuesta(java.util.UUID)> has generic return type <org.jboss.resteasy.reactive.RestResponse<mx.habil.pedidos.model.entity.PedidoEntity>> with type argument depending on <mx.habil.pedidos.model.entity.PedidoEntity>
   ✅ CapasTest falló en una sola regla, elMeseroNoConoceLaFichaDelArchivo
✅ frontera: se sostiene (la violación y las tres fugas salieron en rojo, cada una por su razón)
```

Si algún día alguien afloja una regla, sale `❌ PASÓ: CapasTest no vio la siembra. Esa parte de la
frontera no existe.` Se probó así: con la regla de la ficha apuntando a otro paquete, el guion se
detuvo en rojo en la primera fuga; sin la regla de capas para `api`, en la violación. Si una
siembra no compila, o la prueba falla por otra razón, también sale en rojo. Y el resultado se lee
del informe de Surefire (cuántas reglas corrieron, cuántas fallaron y con qué mensaje), no del
código de salida de Maven.

**Rómpelo tú.** Siembra la flecha en tu copia del proyecto:

```bash
sed -i.bak '$d' src/main/java/mx/habil/pedidos/service/PedidoService.java
printf '    public void avisar(mx.habil.pedidos.api.PedidoResource mesero) {\n    }\n}\n' \
  >> src/main/java/mx/habil/pedidos/service/PedidoService.java
./mvnw -q test-compile
./mvnw -q test -Dtest=CapasTest
```

La primera línea quita la última llave de la clase (y deja una copia `.bak`); la segunda agrega el
método y vuelve a cerrar la clase. `test-compile` termina sin decir nada: compiló. La prueba, no.
Entre muchas líneas, la que importa:

```
Method <mx.habil.pedidos.service.PedidoService.avisar(mx.habil.pedidos.api.PedidoResource)> has parameter of type <mx.habil.pedidos.api.PedidoResource> in (PedidoService.java:0)
```

**Regresa solo lo que tocaste.** El experimento cambia un archivo. Primero mira qué cambió en él:

```bash
git diff -- src/main/java/mx/habil/pedidos/service/PedidoService.java
```

Si solo ves el método que sembraste, regresa ese archivo, y ninguno más:

```bash
git restore src/main/java/mx/habil/pedidos/service/PedidoService.java
bash herramientas/probar-frontera.sh
git status --short
```

La frontera vuelve a `✅ frontera: se sostiene`, y `git status` enseña lo único que queda: la copia
`PedidoService.java.bak` que dejó `sed`. Consérvala hasta que estés seguro de que no la necesitas
y bórrala con `rm -i src/main/java/mx/habil/pedidos/service/PedidoService.java.bak`, que pregunta
antes. No uses un comando que limpie toda la carpeta: se llevaría también tus notas. Lo más cómodo
es practicar en un clon que solo uses para esto.

---

## Arranque y memoria

Quarkus promete arrancar rápido y gastar poca memoria, y además se puede compilar a un ejecutable
**nativo**, sin máquina virtual de Java. Lo medimos, con la misma API y el mismo método para las
tres.

**Qué se midió.** Desde que se lanza el proceso hasta la **primera respuesta 201** a un
`POST /v1/pedidos`: no basta con que el puerto abra, la respuesta obliga a consultar al proveedor
de inventario (dos llamadas HTTP salientes) y a guardar en H2. En ese instante se lee la memoria
residente (RSS) del proceso con `ps`. Cinco corridas de cada una, en seguida, y la mediana.

**Dónde.** Una Mac con Apple Silicon (macOS, arm64, 10 núcleos, 32 GB), el 18-nov-2026, con el
proveedor de ejemplo en el puerto 9090.

**Cómo** (con el proveedor ya corriendo, como en «Córrelo»):

```bash
python3 herramientas/medir-arranque.py quarkus-jvm 5 -- java -jar target/quarkus-app/quarkus-run.jar
python3 herramientas/medir-arranque.py quarkus-nativo 5 -- target/pedidos-0.1.0-runner
python3 herramientas/medir-arranque.py spring-boot-jvm 5 -- java -jar ../estructura-java-spring-boot/target/pedidos.jar
```

| La misma API, en… | Primera respuesta (mediana de 5) | Memoria (RSS, mediana de 5) | Las cinco corridas |
|---|---|---|---|
| Quarkus, JVM | **1,220 ms** | **270 MiB** | 1515, 1250, 1220, 1145, 1175 ms |
| Quarkus, nativo | **46 ms** | **60 MiB** | 150, 67, 46, 38, 44 ms |
| Spring Boot (la entrega anterior), JVM | **2,241 ms** | **437 MiB** | 2181, 2241, 2377, 2301, 2207 ms |

Con Java 25 (Temurin 25.0.5+8) las dos de JVM, sin opciones de memoria. Quarkus 3.40.1.2 y
Spring Boot 4.1.2.

**El nativo no es gratis.** Construirlo pide GraalVM, tarda y gasta mucha memoria:

```bash
sdk install java 25.5.5.0+3-graalce
GRAALVM_HOME="$HOME/.sdkman/candidates/java/25.5.5.0+3-graalce" ./mvnw package -Dnative
```

En la misma Mac tardó **80 segundos** en total (`Total time: 01:19 min`; de ellos, 72 los pasó
`native-image` compilando), con un pico de **4.11 GiB** de memoria, y dejó un ejecutable de
**78 MiB** en `target/pedidos-0.1.0-runner`. `./mvnw verify`, en cambio, construye el de JVM y
corre las 82 pruebas. Y el ejecutable sirve solo para el sistema donde se construyó: el de una Mac
no corre en Linux.

El nativo se ejercitó, no solo se arrancó: creó pedidos consultando al proveedor de verdad,
contestó los `404`, `409`, `422` y `400` con el mismo Problem Details, guardó y leyó de H2, contestó
`503` después de tres intentos con el proveedor apagado, y publicó su salud y sus métricas en el
9000.

**Entonces, ¿cuál?** La plantilla corre en JVM: se construye en segundos, se depura con las
herramientas de siempre y arranca en poco más de un segundo, que para una API que vive horas no
pesa. El nativo vale cuando el arranque y la memoria sí pesan: funciones que nacen con cada
petición, muchas réplicas pequeñas, escalar desde cero. Es la misma base de código: decidir es
cambiar el comando de construcción, no la arquitectura.

---

## Lo que cambió respecto de Spring Boot

El restaurante es el mismo: los paquetes, las reglas, los errores y las pruebas de la cocina
(`vo`, `mapper`, `service`, `facade`) pasaron casi sin cambios. Cambió la cocina:

| En Spring Boot | En Quarkus | Por qué importa |
|---|---|---|
| `PedidoController`, `ResponseEntity` | `PedidoResource`, `RestResponse` | Mismo papel, otro nombre: Quarkus usa Jakarta REST |
| `repository/` con Spring Data: una interfaz | `model/repository/` con Panache: una clase que implementa `PanacheRepositoryBase` | Panache también ofrece que la ficha se guarde sola (`extends PanacheEntity`); aquí no, para que solo la despensa escriba |
| `PedidosApplication` con su `main` | Sin `main`: Quarkus la genera | La puerta es `target/quarkus-app/quarkus-run.jar`, una carpeta y no un solo `.jar` |
| `application.yml` | `application.yaml` con la extensión `quarkus-config-yaml` | Una llave mal escrita no falla: se ignora |
| Tiempo límite y reintento en `application.yml` (`InventarioProperties`) | `@Timeout` y `@Retry` en la interfaz del REST Client | La política se lee junto a la llamada |
| `@RestControllerAdvice` | `quarkus-http-problem` + un `ExceptionMapper` + `CatalogoDeQuejas` | Los errores del marco salían en inglés y sin `error_code` |
| Salud en `:8080/actuator/health` | Salud y métricas en `:9000/q/health` y `:9000/q/metrics` | El puerto de la API no enseña nada de adentro |
| `@WebMvcTest`, `@DataJpaTest` (rebanadas) | `@QuarkusTest`, con `@InjectMock` | Quarkus arranca la aplicación completa, una vez para todas |
| Un id que no es UUID: `400` | Un id que no es UUID: `404` | En Jakarta REST, si el parámetro no se convierte, la ruta no existe |
| `List<PedidoEntity>` como fuga | `List<…>` y además `RestResponse<PedidoEntity>` | La frontera prueba tres fugas, no solo la violación |

---

## Qué NO trae y por qué

| No trae | Por qué |
|---|---|
| Autenticación y permisos | Fuera del ejemplo. Una API real no se publica sin ellos: ver «Cómo crece» |
| Una base de datos de verdad, migraciones | H2 en memoria corre a la primera. Los datos se pierden al apagar |
| Contenedores para las pruebas | Quarkus levanta contenedores solo para las pruebas (*Dev Services*); aquí están apagados: las pruebas corren con Java y nada más |
| Interfaz e implementación por cada servicio (`service/impl`) | Con una sola implementación, la interfaz es un archivo más. Ver «Cómo crece» |
| Interruptor de circuito (*circuit breaker*) | El tiempo límite y el reintento alcanzan para un proveedor. Ver «Cómo crece» |
| Contrato OpenAPI | Fuera del ejemplo. Ver «Cómo crece» |
| Trazas distribuidas | Hay `request_id` en cada línea de la bitácora y en cada error, y métricas en el 9000. Ver «Cómo crece» |
| Paginación, idempotencia | El ejemplo no lista pedidos ni recibe pagos |
| Una facade para consultar | Solo interviene un servicio: `api` lo llama directo |

---

## Cómo crece: cuándo agregar cada cosa

| Cuando… | Agrega… | Y deja… |
|---|---|---|
| Una operación coordina a dos o más servicios o clientes | un método en la facade | el atajo `api → service` para lo simple |
| Aparece otra funcionalidad (clientes, pagos) | sus clases en los mismos paquetes: `ClienteResource`, `ClienteService`… | un solo árbol de capas |
| Los paquetes pasan de unas diez clases cada uno | un nivel por funcionalidad dentro de cada capa, o al revés | `CapasTest`, ajustada: la regla no cambia |
| Los datos tienen que sobrevivir al apagado | PostgreSQL u otra base, migraciones versionadas (Flyway) y `schema-management.strategy: validate` | H2 solo para las pruebas |
| La API sale de tu máquina | autenticación (OIDC) y `@RolesAllowed` en cada ruta | la salud abierta en el 9000 para quien vigila |
| Un proveedor se cae seguido | `@CircuitBreaker`, de la misma SmallRye Fault Tolerance | el reintento en una sola capa |
| Un servicio tiene dos implementaciones de verdad | la interfaz y sus implementaciones | sin interfaz donde solo hay una |
| Otros equipos consumen la API | el contrato OpenAPI (`quarkus-smallrye-openapi`) | los ejemplos con `curl` |
| Varios servicios se llaman entre sí | trazas con OpenTelemetry | el `request_id` en la bitácora |
| El arranque o la memoria pesan | el nativo: `./mvnw package -Dnative` | la misma base de código |
| Se despliega en un orquestador | `docker build -t pedidos .` | `exec java` como proceso 1 |

Regla para decidir: **un paquete o una dependencia nueva tiene que resolver un problema que ya
tienes**. Si no puedes decir cuál, todavía no lo necesitas.

---

## Cómo quitar el ejemplo

Los pedidos están para demostrar la estructura. Para empezar tu propio proyecto:

1. En `pom.xml`, cambia `groupId`, `artifactId`, `name` y `description`.
2. Renombra el paquete raíz `mx.habil.pedidos` por el tuyo, en `src/main` y en `src/test`. El
   nombre va al revés de tu dominio: si tu sitio es `tienda.mx`, el paquete es `mx.tienda.<app>`.
3. Cambia las clases de pedidos por las tuyas, paquete por paquete. Deja los paquetes que no uses
   todavía fuera: `facade` solo nace cuando hay algo que coordinar.
4. En `CodigoError`, cambia la dirección base de los errores (`https://pedidos.example/errors/`)
   por una de tu dominio, y deja solo los códigos que uses.
5. En `application.yaml`, cambia la llave del reintento de las pruebas
   (`mx.habil.pedidos.client.InventarioApi$$CDIWrapper/producto`) por la de tu cliente.
6. En `CapasTest` y en `herramientas/probar-frontera.sh`, cambia `mx.habil.pedidos` por tu paquete
   y `PedidoService`, `PedidoResource` y `PedidoEntity` por clases tuyas.
7. Corre `./mvnw verify && bash herramientas/probar-frontera.sh`.
8. Reescribe este README. El código y `LICENSE` son MIT; si conservas el texto de los `.md`, mira
   `LICENSE-CONTENIDO.md`.

---

## Versiones

Medidas el 18 de noviembre de 2026.

| Pieza | Versión |
|---|---|
| Java | 25, la LTS vigente. Probado con Temurin 25.0.5+8 en macOS arm64. Linux todavía no se ha probado a mano; el CI corre en Ubuntu. El Enforcer no deja construir con otra línea |
| Quarkus | 3.40.1.2, de la línea 3.40, [la LTS más nueva](https://quarkus.io/blog/quarkus-3-40-released/): recibe parches 12 meses. La 3.40.1.2 es [la corrección de emergencia](https://quarkus.io/blog/quarkus-3-40-1-2-released/) de esa línea |
| Extensiones | Quarkus REST, REST Client, Hibernate ORM con Panache, Hibernate Validator, SmallRye Fault Tolerance, SmallRye Health, Micrometer con Prometheus, `quarkus-config-yaml` y `quarkus-http-problem` 3.38.2 (todas por el BOM) |
| Maven | 3.9.16, por el Maven Wrapper 3.3.4 |
| Pruebas | JUnit 6.1.3, Mockito 5.21.0 y REST Assured 6.0.1, los que trae el BOM; AssertJ 3.27.7; ArchUnit 1.5.1; JaCoCo 0.8.16 |
| Nativo | GraalVM CE 25.5.5.0.3 (Java 25.0.5), por SDKMAN |
| Imagen | `eclipse-temurin:25-jdk` para construir y `eclipse-temurin:25-jre` para correr; construida y probada con Docker en Colima |
| CI | GitHub Actions: `actions/checkout@v7` y `actions/setup-java@v6` con Temurin 25, en Ubuntu y en macOS con `/bin/bash`; corre en cada cambio, a mano y cada lunes. Todavía no ha corrido: el repositorio no se ha publicado |

---

## Licencia

El código es [MIT](LICENSE). El texto y las explicaciones son
[CC BY-SA 4.0](LICENSE-CONTENIDO.md).
