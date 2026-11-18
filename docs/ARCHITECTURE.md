# Arquitectura

Una API por capas, simple y explícita. No es hexagonal ni pretende serlo: cada paquete tiene un
papel, y las dependencias van en un solo sentido. Es el mismo restaurante que la plantilla de
Spring Boot, con otra cocina: Quarkus.

## Tipo de componente

API REST con Quarkus 3.40 (la línea LTS) y Quarkus REST, empaquetada como `fast-jar`
(`target/quarkus-app/`). Java 25, Maven. Corre en la máquina virtual de Java (JVM); la misma
plantilla se construye también como imagen nativa (`./mvnw package -Dnative`). El modo de la
plantilla es JVM, y el nativo es opcional: la razón y las medidas están en el README, en
«Arranque y memoria».

## Las capas y hacia dónde apuntan las flechas

```
api ──► facade ──► service ──► model/repository
 │         │          │
 │         └──► client ◄──┘
 └──────────────► service        (el atajo: cuando no hay nada que coordinar)
```

| Paquete | Puede llamar a | Lo pueden llamar |
|---|---|---|
| `api` | `facade`, `service` | nadie |
| `facade` | `service`, `client` | `api` |
| `service` | `model/repository`, `client` | `api`, `facade` |
| `model/repository` | — | `service` |
| `client` | — | `facade`, `service` |

Los papeles que pasan de mano en mano —`request`, `response`, `entity` y `vo`— viven juntos en
`model/`, y ahí mismo vive la despensa, `model/repository`, junto a la ficha que guarda. Al lado de
las capas, y usables desde cualquiera: `vo`, `utils`, `exception` y `mapper`. `request` y
`response` solo los conocen `api`, `facade` y `mapper`. `entity` la conocen `service`,
`model/repository`, `facade` y `mapper`; por la puerta de `api` nunca entra ni sale.

Estas reglas no son un acuerdo de palabra: las comprueba
`src/test/java/mx/habil/pedidos/arquitectura/CapasTest.java` en cada `./mvnw verify`, y
`herramientas/probar-frontera.sh` demuestra que esa prueba sabe fallar.

## Cuándo hay facade

Cuando una operación coordina a **dos o más** servicios o clientes. Crear un pedido consulta al
proveedor de inventario (`client`) y después aplica las reglas y guarda (`service`): pasa por
`PedidoFacade`. Consultar un pedido lo atiende un solo servicio: `PedidoResource` llama directo a
`PedidoService`.

Una facade cuyo método solo llama a otro método no coordina nada. Se borra, y `api` llama al
servicio.

## Decisiones

| Decisión | Por qué |
|---|---|
| Quarkus 3.40, la línea LTS más nueva | Una LTS recibe parches 12 meses; con Java 25 compila, pasa las pruebas y corre (medido). La versión vive en una sola propiedad, `quarkus.platform.version` |
| Paquetes por capa, no por funcionalidad | Con una sola funcionalidad (pedidos), la capa es lo que enseña la plantilla. Quarkus no exige ninguna de las dos formas |
| Panache en estilo repositorio (`PanacheRepositoryBase`), nunca `extends PanacheEntity` | Con la entidad activa, la ficha sabría guardarse sola y cualquier capa podría escribir en la base. Con el repositorio, solo la despensa guarda |
| H2 en memoria | Corre a la primera, sin instalar una base ni levantar un contenedor, en JVM y en nativo (medido). Los datos se pierden al apagar |
| REST Client con SmallRye Fault Tolerance en la interfaz | La llamada, su tiempo límite (2 s por intento) y su reintento (2 más, solo si no hubo respuesta) se leen juntos, en `InventarioApi` |
| Errores con `quarkus-http-problem` | Ya contesta con Problem Details (RFC 9457) los errores del marco: validación, JSON mal escrito, ruta que no existe. Un `ExceptionMapper` de dominio atiende `PedidosException` |
| `CatalogoDeQuejas` reescribe SIEMPRE tipo, título y detalle de los errores del marco | La extensión los deja en inglés y sin `error_code`. El catálogo de `CodigoError` es el único lugar que arma un error |
| Salud y métricas en el puerto de administración (9000) | El puerto público (8080) solo sirve la API; quien vigila entra por el otro |
| `application.yaml`, con la extensión `quarkus-config-yaml` | Quarkus no lee YAML de fábrica. El formato jerárquico se lee igual que el de la plantilla de Spring Boot |
| Hilos virtuales por método (`@RunOnVirtualThread`) | Se declara en cada método del resource que espera a la base o al proveedor, y se ve al leerlo |
| Sin autenticación | Fuera del ejemplo. Una API real no se publica así: mira «Cómo crece» en el README |
| Sin telemetría avanzada (trazas OTLP) | Fuera del ejemplo: hay `request_id` en cada línea de la bitácora y métricas en `/q/metrics`. Cuándo agregarla está en «Cómo crece» |
