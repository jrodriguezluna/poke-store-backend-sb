# PokéStore Backend (Spring Boot)

Migración del backend FastAPI a Java 21 y Spring Boot. Están implementados los endpoints de productos, contacto, usuarios/autenticación y pedidos, además de health, OpenAPI y archivos estáticos. Consulta [PLAN_MIGRACION.md](./PLAN_MIGRACION.md) para el avance, las verificaciones y los bloqueos pendientes.

## Requisitos

- Java 21
- Maven 3.9+
- Docker Engine y Docker Compose v2

## Configuración

Copiar `.env.example` a `.env`. Sus credenciales son solo para desarrollo local. Spring Boot carga ese archivo de forma opcional al iniciar; las variables de entorno del sistema tienen prioridad. No guardar credenciales reales en el repositorio.

| Variable | Predeterminado | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | Host MySQL visto desde la aplicación ejecutada en el host |
| `DB_PORT` | `3306` | Puerto MySQL |
| `DB_USER` | `pokestore` | Usuario de la aplicación |
| `DB_PASSWORD` | `pokestore_local_only` | Contraseña local de la aplicación |
| `MYSQL_ROOT_PASSWORD` | `pokestore_root_local_only` | Contraseña root del contenedor |
| `DB_NAME` | `pokestore` | Base de datos |
| `SERVER_PORT` | `8000` | Puerto HTTP |
| `UPLOAD_DIR` | `uploads` | Directorio de archivos publicados en `/uploads/**` |
| `MAX_UPLOAD_BYTES` | `2097152` | Límite multipart en bytes |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes CORS separados por coma |
| `GOOGLE_CLIENT_ID` | vacío | Audiencia esperada para validar Google ID tokens |

El perfil predeterminado no ejecuta migraciones ni crea tablas (`ddl-auto=none`). El perfil `local` activa Flyway y carga datos de demostración de forma no destructiva: inserta los 8 productos solo si el catálogo está vacío y crea el administrador solo si aún no existe. No elimina ni reemplaza filas. No usar ese perfil contra una base con datos existentes/productivos; el DDL local refleja los modelos SQLAlchemy conocidos.

Para habilitar el login Google, crea un OAuth Client ID de tipo aplicación web en Google Cloud, agrega `http://localhost:5173` como origen JavaScript autorizado y configura el mismo Client ID en `GOOGLE_CLIENT_ID` del `.env` del backend y `VITE_GOOGLE_CLIENT_ID` del `.env.local` del frontend. Reinicia ambos servidores después de cambiar las variables. El Client ID es público y no debe confundirse con un client secret. El backend exige correo verificado y permite solo los dominios configurados en `UsuarioService`.

## Ejecutar backend y base local

```bash
test -f .env || cp .env.example .env
sudo docker compose up -d
sudo docker compose ps
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Si el usuario actual no tiene acceso al socket Docker, usar `sudo docker compose` también para `ps` y `down`. La primera ejecución descarga MySQL 8.4; el volumen nombrado conserva los datos al reiniciar o ejecutar `docker compose down`. **No usar `docker compose down -v`** salvo que se quiera borrar la base local.

Con el servidor iniciado, `GET http://localhost:8000/api/health` responde `{"ok":true}` cuando MySQL responde a `SELECT 1`. Swagger UI queda en `http://localhost:8000/docs`. Para compilar el artefacto y ejecutar las pruebas: `mvn clean package`. Para detener MySQL sin borrar datos: `sudo docker compose down`.

El esquema Flyway y el seed se ejecutan al arrancar Spring Boot con el perfil local.

### Administrador de demostración

El seed crea esta cuenta solo si todavía no existe:

- **Email:** `admin@pokestore.cl`
- **Contraseña inicial:** `admin123`

Es una cuenta exclusiva para desarrollo local. La contraseña se guarda con BCrypt; el seed no cambia la contraseña si la cuenta ya existía. No uses estas credenciales en un entorno compartido o desplegado.

### Blogs

Con el perfil `local`, Flyway crea las tablas de blogs y el seed agrega los dos artículos de demostración actuales solo cuando la tabla está vacía. Los cambios hechos desde la GUI se guardan en MySQL y no son reemplazados en los siguientes arranques.

La página pública obtiene los artículos desde la API. El panel de administración permite crear, editar y eliminar artículos, incluidas sus secciones ordenadas. Las imágenes se indican como URL HTTP(S) o como una ruta accesible desde el navegador; el backend no almacena archivos de imagen.

Endpoints: `GET /api/blogs`, `GET /api/blogs/{id}`, `POST /api/blogs`, `PUT /api/blogs/{id}` y `DELETE /api/blogs/{id}`. **Importante:** por el alcance acordado, la GUI oculta las acciones a usuarios no-admin, pero el backend no autoriza las rutas `POST`, `PUT` y `DELETE`. No exponer esta API en un entorno público hasta implementar autorización verificable en servidor.

## Ejecutar el frontend

En otra terminal:

```bash
cd /home/sol/Repos/poke-store
npm run dev
```

Abrir `http://localhost:5173`. El frontend consulta el backend en `http://localhost:8000`; usa `localhost` en ambos, ya que `127.0.0.1` es otro origen y no coincide con el CORS local predeterminado. Para detener cada servidor, usar `Ctrl+C` en su terminal.

## Estado

La implementación local incluye los endpoints y reglas de negocio descritos en el plan, incluida la gestión de blogs. El frontend React/Vite está en `/home/sol/Repos/poke-store`; su catálogo cargó productos desde Spring Boot y el login del administrador demo funcionó en navegador. Para las verificaciones, `mvn clean package`, `npm run build` y `npm run test:coverage` pasan. Quedan pendientes probar más flujos de compra. No hay una base desplegada con datos; si en el futuro se conecta una base existente, revisar su esquema antes de habilitar Flyway.
