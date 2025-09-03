FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

# Copy gradle configuration
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
COPY frontend frontend
COPY bin bin

# Make gradlew executable
RUN chmod +x ./gradlew

# Build a release artifact
RUN ./gradlew bootJar -x test

# Extract the JAR layers for better caching
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*.jar)

# Runtime stage
FROM eclipse-temurin:17-jre
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/build/dependency

# Copy extracted application layers
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Expose the application port
EXPOSE 8088

# The application properties will be overridden by the environment variables in docker-compose.yml

# JVM settings optimized for Reactor Netty
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=100", \
    "-XX:+UseStringDeduplication", \
    "-Dio.netty.leakDetection.level=disabled", \
    "-Dio.netty.recycler.maxCapacity=0", \
    "-Dio.netty.allocator.numHeapArenas=0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-cp", "app:app/lib/*", \
    "com.guesshole.GuessholeApplication"]