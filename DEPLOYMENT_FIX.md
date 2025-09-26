# Solución para el Error de Despliegue en Render

## Problemas Identificados

1. **Error inicial**: `java.lang.IllegalArgumentException: 'url' must start with "jdbc"` - Las variables de entorno de la base de datos no estaban siendo configuradas correctamente para Flyway.

2. **Error secundario**: `Could not resolve placeholder 'JWT_SECRET'` - La variable de entorno JWT_SECRET no tenía un valor por defecto y no estaba siendo proporcionada por Render.

## Cambios Realizados

### 1. Archivo `application-render.yml` (NUEVO)
- Creado un perfil específico para Render
- Configuración simplificada que usa directamente las variables de entorno
- Configuración de Flyway optimizada para despliegue
- **CORREGIDO**: JWT_SECRET ahora tiene un valor por defecto para evitar errores de resolución

### 2. Archivo `Dockerfile` (MODIFICADO)
- Cambiado el perfil activo de `prod` a `render`
- Ahora usa: `--spring.profiles.active=render`

### 3. Archivo `render.yaml` (MODIFICADO)
- Configuración completa de Render con:
  - Servicio web configurado
  - Variables de entorno mapeadas desde la base de datos
  - Comandos de build y start optimizados
  - Base de datos PostgreSQL configurada
  - **CORREGIDO**: JWT_SECRET configurado con un valor fijo en lugar de generar uno automáticamente

### 4. Archivo `application-prod.yml` (MEJORADO)
- Agregadas configuraciones adicionales de Flyway:
  - `baseline-on-migrate: true`
  - `validate-on-migrate: true`
- Agregado logging de debug para Flyway

## Variables de Entorno Requeridas en Render

Asegúrate de que estas variables estén configuradas en tu servicio de Render:

- `DATABASE_URL`: URL completa de la base de datos (debe incluir `jdbc:postgresql://`)
- `DATABASE_USERNAME`: Usuario de la base de datos
- `DATABASE_PASSWORD`: Contraseña de la base de datos
- `JWT_SECRET`: Clave secreta para JWT (se puede generar automáticamente)
- `JWT_EXPIRATION`: Tiempo de expiración del token (opcional, por defecto 86400)

## Pasos para Desplegar

1. **Sube los cambios al repositorio**
2. **En Render, configura las variables de entorno** (si no están configuradas automáticamente)
3. **Redeploy el servicio**

## Verificación

El despliegue debería funcionar correctamente ahora. Si aún hay problemas:

1. Verifica que `DATABASE_URL` tenga el formato correcto: `jdbc:postgresql://host:port/database`
2. Revisa los logs de Render para confirmar que las variables de entorno se están cargando
3. Asegúrate de que la base de datos esté accesible desde Render

## Notas Adicionales

- El perfil `render` es específico para Render y no interfiere con otros entornos
- Se mantiene el perfil `prod` para otros despliegues
- La configuración de Flyway está optimizada para manejar migraciones en producción
