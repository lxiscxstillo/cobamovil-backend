# Fase 1: Construcción del proyecto
FROM openjdk:17-jdk-slim AS build

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x mvnw

# Copiar código fuente
COPY src src

# Construir el proyecto (descargar dependencias y compilar)
RUN ./mvnw clean package -DskipTests

# Fase 2: Imagen de ejecución (más liviana)
FROM openjdk:17-jre-slim AS runtime

# Crear usuario no-root para seguridad
RUN addgroup --system spring && adduser --system spring --ingroup spring

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR construido desde la fase anterior
COPY --from=build /app/target/*.jar app.jar

# Cambiar propietario del archivo JAR
RUN chown spring:spring app.jar

# Cambiar al usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8080

# Configurar variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de ejecución
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]