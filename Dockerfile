# One image with the API and the dashboard: the React build is copied into Spring Boot's static resources,
# so the app answers on a single port (8080) with no proxy and no CORS.

# --- 1. dashboard ---
# same major version as the one that wrote package-lock.json: npm 10 and 11 disagree on optional dependencies
FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
# A lock file written on Windows can miss optional packages that Linux needs (npm ci then refuses it): fall back to npm install
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# --- 2. backend (the dashboard is packaged inside the jar) ---
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
# a checkout on Windows may have CRLF line endings, which break the script on Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve dependency:resolve-plugins
COPY src src
COPY --from=frontend /build/frontend/dist src/main/resources/static
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && cp target/fire-calendar-*-SNAPSHOT.jar /build/app.jar

# --- 3. runtime ---
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home firecal
WORKDIR /app
COPY --from=backend /build/app.jar app.jar
USER firecal
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
