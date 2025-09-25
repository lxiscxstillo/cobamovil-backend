# Cobamovil Backend

Backend para la aplicación Cobamovil desarrollado con Spring Boot 3.5.5 y Java 17.

## Características Implementadas

### 🔐 Seguridad
- **Autenticación JWT**: Implementación completa de autenticación basada en tokens JWT
- **Encriptación de contraseñas**: Uso de BCrypt para encriptar contraseñas
- **Validación de contraseñas**: Contraseñas deben contener al menos:
  - 8 caracteres mínimo
  - Una letra minúscula
  - Una letra mayúscula
  - Un número
  - Un carácter especial
- **Configuración de CORS**: Configuración segura para permitir requests desde frontend
- **Spring Security**: Configuración completa de seguridad con roles y permisos

### 📊 Gestión de Usuarios
- **CRUD completo**: Crear, leer, actualizar y eliminar usuarios
- **Paginación**: Listado de usuarios con paginación y ordenamiento
- **Búsqueda**: Búsqueda de usuarios por username
- **Validación**: Validación robusta de datos de entrada
- **Roles**: Sistema de roles (USER, ADMIN)

### 🛡️ Buenas Prácticas de Seguridad
- **Validación de entrada**: Validación exhaustiva de todos los datos de entrada
- **Manejo de excepciones**: Manejo centralizado de excepciones con logging
- **Logging**: Sistema de logging configurado para desarrollo y producción
- **Configuración por perfiles**: Configuraciones separadas para desarrollo y producción
- **Variables de entorno**: Uso de variables de entorno para datos sensibles

### 🗄️ Base de Datos
- **PostgreSQL**: Base de datos principal
- **Flyway**: Migraciones de base de datos versionadas
- **JPA/Hibernate**: ORM para mapeo objeto-relacional
- **Índices**: Índices optimizados para consultas frecuentes

### 📚 Documentación API
- **OpenAPI/Swagger**: Documentación automática de la API
- **Endpoints documentados**: Todos los endpoints con documentación completa
- **Ejemplos de uso**: Ejemplos de requests y responses

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/cobamovil/backend/
│   │   ├── config/          # Configuraciones
│   │   ├── controller/      # Controladores REST
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # Entidades JPA
│   │   ├── repository/     # Repositorios de datos
│   │   ├── security/       # Configuración de seguridad
│   │   └── service/        # Lógica de negocio
│   └── resources/
│       ├── db/migration/   # Migraciones de base de datos
│       └── application.yml # Configuración de la aplicación
└── test/                   # Tests unitarios e integración
```

## Endpoints Disponibles

### Autenticación
- `POST /api/auth/login` - Iniciar sesión
- `POST /api/auth/register` - Registrar usuario
- `POST /api/auth/logout` - Cerrar sesión

### Usuarios
- `GET /api/users` - Listar usuarios (paginado)
- `GET /api/users/{id}` - Obtener usuario por ID
- `POST /api/users` - Crear usuario
- `PUT /api/users/{id}` - Actualizar usuario
- `DELETE /api/users/{id}` - Eliminar usuario
- `GET /api/users/{id}/exists` - Verificar existencia

### Health Check
- `GET /health` - Verificar estado de la aplicación

### Documentación
- `GET /swagger-ui.html` - Interfaz de Swagger UI
- `GET /v3/api-docs` - Documentación OpenAPI en JSON

## Configuración

### Variables de Entorno

```bash
# Base de datos
DATABASE_URL=jdbc:postgresql://localhost:5432/cobamovil_db
DATABASE_USERNAME=cobamovil_user
DATABASE_PASSWORD=cobamovil_pass

# JWT
JWT_SECRET=tu_clave_secreta_muy_larga_y_segura
JWT_EXPIRATION=86400

# Puerto
PORT=8080
```

### Perfiles de Configuración

- **Desarrollo**: `application.yml` (por defecto)
- **Producción**: `application-prod.yml`

## Instalación y Ejecución

### Prerrequisitos
- Java 17+
- Maven 3.6+
- PostgreSQL 12+

### Pasos de Instalación

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd cobamovil-backend
```

2. **Configurar base de datos**
```bash
# Crear base de datos
createdb cobamovil_db

# Crear usuario
psql -c "CREATE USER cobamovil_user WITH PASSWORD 'cobamovil_pass';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE cobamovil_db TO cobamovil_user;"
```

3. **Ejecutar migraciones**
```bash
mvn flyway:migrate
```

4. **Compilar y ejecutar**
```bash
mvn clean compile
mvn spring-boot:run
```

### Con Docker

```bash
# Ejecutar con Docker Compose
docker-compose up -d
```

## Testing

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con cobertura
mvn test jacoco:report
```

## Seguridad

### Contraseñas
- Mínimo 8 caracteres
- Debe contener al menos una letra minúscula, una mayúscula, un número y un carácter especial
- Se almacenan encriptadas con BCrypt

### Tokens JWT
- Válidos por 24 horas por defecto
- Se pueden configurar mediante variables de entorno
- Incluyen información del usuario y roles

### CORS
- Configurado para permitir requests desde el frontend
- Se puede configurar para dominios específicos en producción

## Logging

El sistema de logging está configurado para:
- **Desarrollo**: Nivel DEBUG para debugging
- **Producción**: Nivel INFO para performance
- **Seguridad**: Logs de autenticación y autorización

## Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## Contacto

- Email: admin@cobamovil.com
- Proyecto: [Cobamovil Backend](https://github.com/cobamovil/backend)
