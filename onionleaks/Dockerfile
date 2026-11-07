# Build with nothing but the JDK, run on a minimal JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY build.sh .
COPY src ./src
RUN ./build.sh

FROM eclipse-temurin:21-jre
RUN useradd --system --home /app --shell /usr/sbin/nologin portal \
 && mkdir -p /app/data && chown -R portal:portal /app
WORKDIR /app
COPY --from=build /src/build/onionleaks.jar /app/onionleaks.jar
USER portal
ENV PORTAL_DATA_DIR=/app/data PORTAL_BIND=0.0.0.0 PORTAL_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx256m", "-jar", "/app/onionleaks.jar", "/app/.env"]
