# ==========================================
# ETAPA 1 - BUILD DO PROJETO
# ==========================================

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia as configurações do Maven
COPY pom.xml .

# Baixa as dependências
RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY src ./src

# Compila e empacota a aplicação
RUN mvn clean package -DskipTests


# ==========================================
# ETAPA 2 - EXECUÇÃO DO BACKEND
# ==========================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o JAR gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Documenta a porta utilizada pelo Spring
EXPOSE 8080

# Inicia o TV Score
ENTRYPOINT ["java", "-jar", "app.jar"]
