# ===========================
# 1️⃣ BUILD STAGE
# ===========================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copia Maven wrapper e pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Permessi di esecuzione
RUN chmod +x mvnw

# Scarica le dipendenze
RUN ./mvnw dependency:go-offline -B

# Copia il codice sorgente
COPY src src

# Compila l'app Spring Boot (senza test)
RUN ./mvnw clean package -DskipTests

# ===========================
# 2️⃣ RUNTIME STAGE
# ===========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia il JAR generato
COPY --from=build /app/target/*.jar app.jar

# Espone la porta (Vaadin usa 8080)
EXPOSE 8080

# Imposta variabili opzionali
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando di avvio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
