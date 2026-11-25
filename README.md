# PokéStore Backend (Spring Boot)

Backend REST de PokéStore, migrado desde FastAPI a Java 21 y Spring Boot. Incluye productos, contacto, usuarios/autenticación, pedidos, health, OpenAPI y archivos estáticos.

## Requisitos

- Java 21.
- Maven 3.9 o superior.
- MySQL 8.4 o compatible.
- Docker Engine + Docker Compose v2 solo si quieres ejecutar MySQL en un contenedor.

La aplicación Spring Boot se ejecuta en el host con Maven. Docker Compose, cuando se usa, levanta únicamente MySQL.

## Preparación

Desde la raíz del repositorio, copia la plantilla de variables:

### Linux

```bash
cp .env.example .env
```

### Windows (PowerShell)

```powershell
Copy-Item .env.example .env
```

Edita `.env` antes de iniciar. Los valores incluidos son solo para desarrollo local; no uses esas credenciales en producción.

`JWT_SECRET` firma los tokens de sesión. El valor de ejemplo es solo local; en otros entornos configura una clave aleatoria de al menos 32 bytes y no la subas al repositorio.

## Opción A: MySQL en Docker Compose

Instala e inicia Docker Desktop en Windows o Docker Engine + Compose v2 en Linux. Desde la raíz del backend:

### Linux

```bash
docker compose up -d
docker compose ps
```

Si tu usuario no tiene acceso al daemon de Docker, ejecuta esos comandos con los permisos requeridos por tu distribución (por ejemplo, `sudo docker compose ...`).

### Windows (PowerShell o CMD)

```powershell
docker compose up -d
docker compose ps
```

Compose publica MySQL solo en `127.0.0.1` del host, en el puerto indicado por `DB_PORT` (por defecto `3306`). Como Spring Boot corre fuera del contenedor, `DB_HOST=localhost` es correcto.

El volumen nombrado conserva los datos al reiniciar o ejecutar `docker compose down`. **No uses `docker compose down -v`** salvo que quieras eliminar la base de datos local.

## Opción B: MySQL instalado en el host (sin Docker)

Instala MySQL 8.4 o compatible y asegúrate de que el servicio esté iniciado.

En Linux, según la distribución:

```bash
sudo systemctl start mysql
sudo systemctl status mysql
```

En algunas distribuciones el servicio se llama `mariadb`. En Windows, encuentra el servicio instalado y usa su nombre real:

```powershell
Get-Service *mysql*
Start-Service MySQL84
```

Conéctate como administrador de MySQL (`mysql -u root -p`; en Linux también puede funcionar `sudo mysql`) y crea la base y el usuario configurados en `.env`:

```sql
CREATE DATABASE pokestore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'pokestore'@'localhost' IDENTIFIED BY 'cambia_esta_clave';
GRANT ALL PRIVILEGES ON pokestore.* TO 'pokestore'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

En `.env`, configura `DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=pokestore`, `DB_USER=pokestore` y la misma contraseña que asignaste al usuario. Si MySQL está en otro equipo, usa en `DB_HOST` su hostname/IP accesible desde el host donde se ejecuta Spring Boot, y permite la conexión en la red/firewall y en los permisos de MySQL.

## Ejecutar el backend

Verifica Java y Maven:

```bash
java -version
mvn -version
```

Al ejecutar con el perfil `local`, Flyway aplica el esquema y carga datos de demostración sin borrar filas existentes.

### Linux

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### Windows (PowerShell)

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

### Windows (CMD)

```bat
set SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

El servidor queda en <http://localhost:8000>. `GET /api/health` responde `{"ok":true}` cuando MySQL está accesible; Swagger UI está en <http://localhost:8000/docs>. Detén el servidor con `Ctrl+C`.

El login normal y Google mantienen los campos del usuario y agregan `token` a la respuesta. El JWT firmado con HS256 expira en 12 horas; el frontend lo envía como `Authorization: Bearer ...`. Cambiar `JWT_SECRET` invalida los tokens emitidos con el valor anterior.

Los endpoints de escritura de productos y blogs, la administración de usuarios y las operaciones de pedidos requieren JWT. Los clientes solo pueden consultar y modificar su propio perfil y sus pedidos; el rol `admin` puede gestionar esos recursos. El rol `vendedor`, asignado por un administrador desde el panel de usuarios, puede consultar todos los pedidos y sus detalles, además del catálogo público; no puede crear pedidos ni modificar productos, blogs o usuarios. La vista del vendedor no muestra esos otros accesos y no expone datos personales del comprador en las órdenes. Después de un cambio de rol, la persona debe iniciar sesión nuevamente para recibir un token con los permisos actualizados; los tokens anteriores siguen válidos hasta expirar (máximo 12 horas). El catálogo, la lectura de blogs, contacto y health siguen siendo públicos.

Para compilar y ejecutar las pruebas:

```bash
mvn clean package
```

## Ejecutar backend y frontend juntos (Linux, Bash 4.3+)

Con ambos repositorios en carpetas hermanas:

```text
repos/
├── poke-store-backend-sb/
└── poke-store/
```

Primero inicia MySQL (con Docker Compose o instalado localmente), configura `.env`, e instala las dependencias del frontend según su README. Después, desde la raíz de este backend:

```bash
chmod +x ./dev.sh
./dev.sh
```

Para indicar otra ubicación del frontend:

```bash
./dev.sh /ruta/al/poke-store
```

El script inicia Spring Boot con el perfil `local` y Vite en una sola terminal. No inicia MySQL, no instala dependencias ni ejecuta `docker compose`. Presiona `Ctrl+C` para detener ambos servidores. En Windows, ejecútalos en dos terminales siguiendo las instrucciones de cada repositorio.

## Google Sign-In

Para habilitarlo, crea un OAuth Client ID tipo aplicación web en Google Cloud, agrega `http://localhost:5173` como origen JavaScript autorizado y configura el mismo Client ID en `GOOGLE_CLIENT_ID` del backend y `VITE_GOOGLE_CLIENT_ID` del frontend. Reinicia ambos servidores después de cambiar las variables. El backend exige correo verificado y permite solo los dominios configurados en el servicio de usuarios.

## Administrador de demostración y datos

El perfil `local` crea el administrador solo si aún no existe:

- Email: `admin@pokestore.cl`
- Contraseña inicial: `admin123`

El seed no cambia la contraseña de una cuenta existente. No uses estas credenciales en un entorno compartido o desplegado.

## Blogs

Con el perfil `local`, Flyway crea las tablas de blogs y el seed agrega artículos de demostración solo cuando la tabla está vacía. La GUI permite gestionar artículos y sus secciones.

Las rutas `POST`, `PUT` y `DELETE` de blogs requieren un JWT con rol `admin`; ocultar los controles en la GUI no sustituye esta comprobación del backend.
