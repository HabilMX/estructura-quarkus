# La imagen, en dos pasos: uno construye, el otro solo lleva lo que corre.

# --- 1. Construcción: con el JDK y el Maven de la plantilla ---
FROM eclipse-temurin:25-jdk AS construccion
WORKDIR /obra
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/
# Las pruebas ya corrieron antes de llegar aquí (./mvnw verify); la imagen no las repite.
RUN sh ./mvnw -q -B -DskipTests package

# --- 2. Lo que corre: solo el JRE y la aplicación ---
FROM eclipse-temurin:25-jre
# Un usuario sin privilegios: la aplicación no corre como root.
RUN useradd --system --uid 10001 --no-create-home pedidos
WORKDIR /app
# Quarkus deja la aplicación ya separada en capas (fast-jar): las bibliotecas, que casi no cambian,
# van primero; lo que cambia en cada versión, al final.
COPY --from=construccion /obra/target/quarkus-app/lib/ ./lib/
COPY --from=construccion /obra/target/quarkus-app/quarkus/ ./quarkus/
COPY --from=construccion /obra/target/quarkus-app/app/ ./app/
COPY --from=construccion /obra/target/quarkus-app/quarkus-run.jar ./
USER 10001
# 8080 es la API; 9000, la salud y las métricas.
EXPOSE 8080 9000
# La memoria sigue al límite del contenedor: un porcentaje, nunca un tamaño fijo.
ENV JVM_OPTS="-XX:MaxRAMPercentage=55 -XX:InitialRAMPercentage=55 -XX:+ExitOnOutOfMemoryError"
# «exec» hace que java sea el proceso 1 del contenedor: así recibe la señal de apagado.
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar /app/quarkus-run.jar"]
