# ─────────────────────────────────────────────
# STAGE 1: BUILD — compile the Spring Boot JAR
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

# (1) Set working directory inside the build container
WORKDIR /build

# (2) Copy ONLY pom.xml first — Docker layer caching trick.
# If pom.xml hasn't changed, Docker reuses the cached dependency
# download layer instead of re-downloading every dependency on
# every code change. This dramatically speeds up rebuilds.
COPY pom.xml .

# (3) Download dependencies (cached layer if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# (4) Now copy the actual source code
COPY src ./src

# (5) Build the fat JAR, skip tests for faster image builds
# (tests should run in CI, not during image build)
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# STAGE 2: RUNTIME — slim JRE-only image
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# (6) Set working directory for the runtime container
WORKDIR /app

# (7) Create directories for the SQLite DB and dataset
# (these will be mounted as volumes for persistence)
RUN mkdir -p /app/data /app/dataset

# (8) Copy ONLY the built JAR from the build stage — none of
# Maven, the JDK, or source code is present in the final image
COPY --from=build /build/target/*.jar app.jar

# (9) Copy the dataset into the image (also could be a mounted volume)
COPY dataset/queries.csv /app/dataset/queries.csv
COPY dataset/sample_queries.csv /app/dataset/sample_queries.csv

# (10) Document which port this container listens on
EXPOSE 8080

# (11) Health check — Docker can report container health status
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/trending || exit 1

# (12) Run the JAR when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]