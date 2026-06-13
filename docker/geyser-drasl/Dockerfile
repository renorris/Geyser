FROM eclipse-temurin:21-jdk AS build

WORKDIR /src
COPY . .
RUN ./gradlew :standalone:shadowJar --no-daemon \
    && set -eux; \
    jar="$(find bootstrap/standalone/build/libs -maxdepth 1 -type f -name 'Geyser-Standalone*.jar' | sort | head -n 1)"; \
    test -n "$jar"; \
    cp "$jar" /tmp/Geyser.jar

FROM eclipse-temurin:21-jre

WORKDIR /data
RUN mkdir -p /opt/geyser
COPY --from=build /tmp/Geyser.jar /opt/geyser/Geyser.jar
COPY docker/geyser-drasl/entrypoint.sh /opt/geyser/entrypoint.sh
RUN chmod +x /opt/geyser/entrypoint.sh

EXPOSE 19132/udp
ENTRYPOINT ["/opt/geyser/entrypoint.sh"]
