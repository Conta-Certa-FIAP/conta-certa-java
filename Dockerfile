# ---------------------------------------------------------------------------
# Dockerfile multi-stage
#   Estágio 1 (build): imagem com Maven + JDK, só para compilar e gerar o .jar
#   Estágio 2 (execução): imagem pequena só com o JRE, que roda o .jar
# Vantagem: a imagem final não carrega o Maven nem o código-fonte.
# ---------------------------------------------------------------------------

# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia primeiro só o pom.xml e baixa as dependências.
# Assim o Docker reaproveita essa camada enquanto o pom não mudar (build mais rápido).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os scripts do banco entram no .jar (ver pom.xml e MigracaoBanco)
COPY database ./database
RUN mvn -B -q clean package -DskipTests

# ---------- Estágio 2: execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/conta-certa-api.jar app.jar

# A porta real vem da variável PORT (exigência do Render); 8080 é o padrão local.
EXPOSE 8080

# Fuso de Brasília: as datas das transações usam o "hoje" do servidor.
ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
