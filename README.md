# PokéStore Backend (Spring Boot)

Migración del backend FastAPI a Java y Spring Boot. La API y la base de datos se están migrando por fases; por ahora están implementados la configuración inicial, los archivos estáticos y `GET /api/health`.

## Requisitos

- Java 21
- Maven 3.9+
- MySQL compatible con el backend de origen

## Configuración

Copiar `.env.example` a `.env` y configurar la conexión a MySQL. Spring Boot carga ese archivo de forma opcional al iniciar; las variables de entorno del sistema tienen prioridad. No guardar credenciales reales en el repositorio.

| Variable | Predeterminado | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | Host MySQL |
| `DB_PORT` | `3306` | Puerto MySQL |
| `DB_USER` | `root` | Usuario MySQL |
| `DB_PASSWORD` | vacío | Contraseña MySQL |
| `DB_NAME` | `pokestore` | Base de datos |
| `SERVER_PORT` | `8000` | Puerto HTTP |
| `UPLOAD_DIR` | `uploads` | Directorio de archivos publicados en `/uploads/**` |
| `MAX_UPLOAD_BYTES` | `2097152` | Límite multipart en bytes |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes CORS separados por coma |

La aplicación no crea ni modifica tablas automáticamente (`ddl-auto=none`). No conectarla a una base de producción hasta que se complete la fase de inspección/migración de esquema y se tenga un respaldo.

## Ejecutar y probar

```bash
mvn test
mvn spring-boot:run
```

`GET http://localhost:8000/api/health` responde `{"ok":true}` solo cuando MySQL responde a `SELECT 1`. Swagger UI queda en `http://localhost:8000/docs`.

## Estado

Consultar [PLAN_MIGRACION.md](./PLAN_MIGRACION.md) para el alcance, el seguimiento por fases y las decisiones pendientes. En este inicio todavía no se han migrado los endpoints de negocio.
