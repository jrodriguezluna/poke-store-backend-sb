# Plan de migración: PokéStore Backend (FastAPI → Spring Boot)

**Estado:** no hay una base desplegada con datos; el objetivo es probar el frontend y el backend como proyecto local. El esquema y seed de demostración se ejecutan en MySQL 8.4 en Docker. Los endpoints de fases 2–5 están implementados; faltan ampliar pruebas automatizadas y validar Google con credenciales reales.
**Origen:** `/home/sol/Repos/poke-store-backend`  
**Destino:** `/home/sol/Repos/poke-store-backend-sb`  
**Objetivo:** sustituir la implementación FastAPI por una aplicación Java/Spring Boot, conservando el contrato consumido por el frontend y permitiendo conectar la nueva aplicación a la base MySQL existente sin perder datos.

Este documento será la referencia para ejecutar la migración. Al avanzar, se actualizarán aquí las casillas y decisiones, junto con los resultados de validación.

## 1. Alcance y criterios rectores

### Incluido

- Aplicación Spring Boot con API REST, persistencia MySQL, validaciones, configuración por entorno y documentación OpenAPI.
- Migración de los recursos actuales: productos, usuarios, pedidos y contacto.
- Conservación del formato de respuestas y los códigos HTTP que utiliza el frontend; la base de desarrollo se crea localmente desde cero.
- Compatibilidad con imágenes locales servidas bajo `/uploads` y URLs remotas de imágenes.
- Migración segura del esquema existente y documentación para preparar una base vacía.
- Pruebas que comprueben contratos, reglas de negocio y operaciones transaccionales.

### Fuera de alcance salvo decisión posterior

- Cambios del frontend, nuevos endpoints o cambios de UX.
- Rediseño de permisos/autorización, sesiones o tokens. La implementación actual no exige autenticación en las rutas CRUD.
- Cambio de proveedor o motor de base de datos, almacenamiento remoto de imágenes o rediseño del modelo.
- Borrado o recreación de datos de una base ya utilizada.

### Reglas para la migración

1. Mantener compatibilidad observable antes de mejorar o rediseñar comportamientos.
2. No ejecutar inicializadores destructivos sobre una base de datos existente.
3. Las operaciones de pedido y stock deben ser atómicas.
4. No incluir credenciales ni datos de `.env` en el repositorio.
5. Si una inconsistencia del origen requiere una decisión funcional, documentarla y no resolverla silenciosamente.

## 2. Inventario funcional de origen

| Área | Contrato y comportamiento que se debe preservar |
|---|---|
| Sistema | `GET /api/health` consulta la base (`SELECT 1`) y responde `{"ok":true}` solo si la conexión funciona. |
| Productos | `GET /api/productos` lista productos activos ordenados por ID; `GET /api/productos/{id}` obtiene un activo; `POST` crea (201); `PUT /{id}` reemplaza los campos editables; `DELETE /{id}` hace borrado lógico (204). La salida incluye `id`, `nombre`, `tipo`, `categoria`, `precio`, `imagen`, `descripcion` y `stock`; no expone `activo`. Precio y stock no pueden ser negativos, nombre tiene máximo 100 y URL de imagen debe ser HTTP(S). |
| Registro/login | `POST /api/registro` (201) crea usuario cliente; `POST /api/login` devuelve el usuario, con error 401 genérico para email o contraseña incorrectos. Email se normaliza a minúsculas; registro controla colisiones con 409. |
| Google | `POST /api/auth/google` valida el ID token con el client ID configurado, exige email verificado y dominio permitido, crea usuario cliente en primer acceso y devuelve el usuario. Configuración ausente: 500; token inválido: 401; error de validación con Google: 503; dominio rechazado: 403. |
| Usuarios | `GET /api/usuarios` lista; `PUT /api/usuarios/{id}` modifica perfil y acepta cambio de rol; `DELETE /api/usuarios/{id}` elimina (204), salvo que tenga pedidos asociados (409). Actualmente estas rutas no aplican control de acceso. |
| Pedidos | `POST /api/pedidos` recibe `{usuarioId, items:[{productoId,cantidad}]}` y responde `{id,total}` (201). Consolida líneas repetidas, exige usuario y productos activos, valida stock y descuenta existencias dentro de la misma transacción. `GET /api/pedidos?usuarioId=...` devuelve historial descendente con `fecha` e items `{nombre,cantidad,precioUnit}`. |
| Contacto | `POST /api/contacto` valida y persiste nombre, email y mensaje, y responde `{"ok":true}` (201). |
| Errores/validación | Los errores HTTP y de validación tienen forma `{"error":"..."}`. Validación devuelve 422 y lista los nombres de campos inválidos en el mensaje `Datos inválidos: ...`. |
| CORS/archivos | Orígenes CORS configurables (`CORS_ORIGINS`, por defecto `http://localhost:5173`). `/uploads/**` sirve archivos del directorio configurable. Al actualizar producto, se intenta borrar la imagen anterior solo si era una ruta local `/uploads/`. |

### Contratos de validación conocidos

- Producto: nombre 1–100; precio y stock ≥ 0; imagen ≤ 255; categoría ≤ 50.
- Registro: nombre 1–100; email válido y dominio permitido; teléfono ≤ 20; dirección 5–200; región y comuna 1–100; contraseña 6–64 caracteres.
- Login: email válido; contraseña ≤ 64.
- Actualización de usuario: campos de perfil con límites equivalentes; contraseña opcional (6–64).
- Pedido: al menos un item; cantidad ≥ 1.
- Contacto: nombre 1–100; email válido; mensaje 1–2000.

## 3. Modelo de datos a conservar

Modelar las tablas del backend Python, preservando nombres de tabla/columna, tipos, claves y relaciones. No hay un esquema desplegado con datos que se deba conservar; Flyway crea la base local de demostración.

| Tabla | Columnas / relación relevante |
|---|---|
| `usuarios` | `id`, `nombre`, `email` único, `password_hash`, `telefono`, `direccion`, `region`, `comuna`, `rol`, `creado_en`. |
| `productos` | `id`, `nombre`, `tipo`, `categoria`, `precio`, `imagen`, `descripcion`, `stock`, `activo`. |
| `pedidos` | `id`, `usuario_id` → `usuarios.id`, `total`, `creado_en`. |
| `pedido_items` | `id`, `pedido_id` → `pedidos.id`, `producto_id` → `productos.id`, `cantidad`, `precio_unit`. |
| `mensajes_contacto` | `id`, `nombre`, `email`, `mensaje`, `creado_en`. |

El origen tiene una migración aditiva que agrega columnas de perfil y `productos.categoria`, y rellena categorías derivadas del nombre. Reproducir su resultado mediante una migración versionada revisada contra la base real; no copiar `drop_all/create_all` a la ejecución normal.

## 4. Arquitectura destino propuesta

Estructura por capas y por recurso, evitando lógica de negocio en controladores:

```text
src/main/java/<paquete-base>/
  config/          configuración, CORS y almacenamiento estático
  common/          manejo de errores y respuestas compartidas
  health/          comprobación de salud
  producto/        controller, service, repository, entity y DTO
  usuario/         controller, service, repository, entity y DTO
  pedido/          controller, service, repository, entities y DTO
  contacto/        controller, service, repository, entity y DTO
```

- **Tecnología:** Maven; Spring Boot; Spring Web; Spring Data JPA; Bean Validation; conector MySQL; Springdoc OpenAPI; pruebas Spring Boot/JUnit. Elegir versiones compatibles entre sí y con la versión de Java instalada al iniciar la fase 1.
- **Seguridad de contraseñas:** verificar compatibilidad con los hashes bcrypt existentes y usar un encoder bcrypt; nunca cambiar hashes por un algoritmo distinto sin estrategia de transición comprobada.
- **Google:** usar una biblioteca oficial/compatible para validar firma, audiencia, vencimiento y emisor del ID token; no confiar en claims sin validación criptográfica.
- **Configuración:** `application.yml` con variables de entorno para host/puerto/usuario/clave/nombre de DB, `UPLOAD_DIR`, límite de carga y orígenes CORS. Mantener valores sensibles fuera del repositorio.
- **Persistencia:** nombres explícitos en entidades JPA; DTO separados para request/response; relaciones lazy/queries explícitas para evitar respuestas incompletas o N+1.
- **Errores:** `@RestControllerAdvice` central para conservar estructura `{error: ...}`, códigos y mensajes funcionales existentes.
- **Migraciones:** Flyway para cambios versionados y no destructivos. Revisar estrategia de baseline al conectar bases preexistentes; no activar un baseline automático a ciegas ni ejecutar `clean`.
- **Archivos:** publicar `UPLOAD_DIR` como `/uploads/**`; validar y limitar archivos solo si se incorpora una ruta de carga (hoy no existe endpoint de carga). Mantener limpieza acotada a archivos locales bajo ese directorio.
- **Operaciones transaccionales:** encapsular creación de pedido, consolidación de items, validación/descuento de stock y persistencia de items en un servicio con `@Transactional`. Revisar concurrencia para impedir sobreventa.

## 5. Fases de ejecución

### Fase 0 — Preparación y línea base

- [x] Confirmar el estado del destino: al inicio contenía solo este plan; no había código que preservar.
- [x] Revisar estructura, dependencias y contratos del backend Python, y localizar el frontend React/Vite en `/home/sol/Repos/poke-store`.
- [x] Confirmar con el usuario que no existe una base desplegada con datos; el destino de esta migración es la base local de demostración.
- [x] Registrar Java 21.0.13 y Maven 3.9.12; seleccionar Spring Boot 4.1.2 y Springdoc 3.2.0. Java y Maven quedaron instalados para compilar el proyecto.
- [x] Definir paquete base `cl.pokestore.backend` y comandos Maven; no se encontró infraestructura de despliegue que requiera un perfil adicional.

**Salida / puerta:** plataforma y frontend definidos. No hay migración de datos productivos en el alcance actual; si aparece una base existente, se debe verificar su esquema antes de conectarla.

### Fase 1 — Esqueleto ejecutable y configuración

- [x] Crear proyecto Maven/Spring Boot 4.1.2 para Java 21 y la aplicación principal en `cl.pokestore.backend`.
- [x] Configurar variables de entorno, conexión MySQL y pool; desactivar cambios automáticos de esquema y configurar el dialecto para permitir que el servidor arranque sin DB disponible.
- [x] Verificar conexión a MySQL 8.4 local y manejo de contraseña con `@`, `/`, `#`, `?` mediante el perfil local.
- [x] Implementar CORS, estáticos `/uploads/**`, OpenAPI y `GET /api/health` con comprobación efectiva de DB.
- [x] Añadir `.gitignore`, plantilla de variables sin secretos y README con instalación/ejecución.
- [x] Ejecutar `mvn -f /home/sol/Repos/poke-store-backend-sb/pom.xml test`: compilación y dos pruebas de health correctas.
- [x] Arrancar sin DB y comprobar que `GET /api/health` responde 500 con `{"error":"No se pudo completar la operación de base de datos"}`.
- [x] Empaquetar con `mvn -f /home/sol/Repos/poke-store-backend-sb/pom.xml clean package`; smoke tests HTTP verifican `/docs` (redirige y carga), `/v3/api-docs` (200) y `/uploads/**` (200).
- [x] Comprobar el health positivo (`{"ok":true}`) contra MySQL 8.4 en el contenedor local.

**Salida / puerta:** el esqueleto compila, arranca con y sin DB, se conecta a MySQL local y no incluye credenciales locales en Git.

### Fase 2 — Esquema, entidades y compatibilidad de datos

- [x] Añadir Docker Compose con MySQL 8.4, puerto enlazado solo a localhost, volumen persistente y una migración Flyway local derivada de los modelos Python conocidos.
- [x] Implementar las cinco entidades y sus claves/relaciones, usando nombres de tabla/columna derivados del modelo Python.
- [x] Confirmar que no existe esquema desplegado que requiera comparación, migraciones aditivas o baseline; si se conecta una BD existente en el futuro, revisar DDL antes de habilitar Flyway.
- [x] Configurar Flyway para crear el esquema de una base vacía en el perfil local, sin ejecutar migraciones en el perfil predeterminado.
- [x] Validar arranque, validación JPA/Flyway y lectura/escritura de los flujos migrados contra MySQL 8.4 local.

**Salida / puerta:** el esquema local deriva de los modelos Python y pasa validación JPA en una base vacía; no hay una base existente que migrar.

### Fase 3 — Catálogo y contacto

- [x] Migrar DTO, validaciones y endpoints de productos, incluido filtro de activos, orden, soft delete y respuesta 204.
- [x] Preservar el valor `tipo` al serializar aunque actualmente no sea editable en `ProductoIn`.
- [x] Migrar `/api/contacto` y respuesta 201 `{ok:true}`.
- [x] Implementar errores de no encontrado, validación y URL de imagen en el formato acordado.
- [ ] Ampliar las pruebas automatizadas de integración para códigos, JSON, filtros, orden y soft delete; hay pruebas focalizadas y verificaciones HTTP manuales, no cobertura persistente de todos esos casos.

**Salida / puerta:** endpoints implementados y flujos HTTP representativos verificados localmente; falta completar la matriz automatizada de regresión.

### Fase 4 — Usuarios y autenticación

- [x] Migrar registro, login, listado, actualización y eliminación; conservar normalización de email, límites, unicidad y manejo de usuario con pedidos.
- [x] Implementar bcrypt compatible con hashes anteriores y verificar un hash bcrypt generado por Python.
- [x] Implementar validación de dominio y Google ID token; conservar errores de configuración, proveedor y token.
- [x] Registrar la política actual: rutas de listado/edición/borrado y cambio de `rol` siguen sin control de acceso, igual que en el origen.
- [ ] Probar colisiones concurrentes de email y login federado con credenciales reales; Google está implementado, pero falta `GOOGLE_CLIENT_ID` para una verificación de extremo a extremo.
- [ ] Añadir pruebas automatizadas persistentes de registro, login, actualización, eliminación y Google.

**Salida / puerta:** flujos locales se probaron manualmente, incluido bcrypt compatible y no exposición de hashes; la validación real de Google y la cobertura automatizada siguen pendientes.

### Fase 5 — Pedidos y consistencia de stock

- [x] Migrar DTO con nombres JSON camelCase `usuarioId`, `productoId` y `precioUnit`.
- [x] Implementar consolidación de líneas repetidas, rechazo de usuario/productos inexistentes o inactivos y errores de stock.
- [x] Calcular total con los precios vigentes; persistir pedido/items y descuento de stock en una transacción.
- [x] Implementar bloqueo de filas de producto para serializar pedidos que compiten por stock.
- [x] Migrar historial ordenado descendentemente y mantener snapshots `precio_unit` y nombre de producto según la semántica actual.
- [ ] Añadir pruebas de integración persistentes para rollback, asociaciones y solicitudes concurrentes; se verificaron manualmente consolidación de líneas, descuento de stock, historial y rechazo por stock insuficiente.

**Salida / puerta:** implementación transaccional con bloqueo de filas; falta demostrar rollback/concurrencia mediante pruebas de integración.

### Fase 6 — Herramientas operativas y paridad

- [x] Proporcionar seed local no destructivo: crear ocho productos solo si el catálogo está vacío y la cuenta demo solo si no existe; no recrea tablas ni reemplaza datos.
- [ ] Documentar migración de BD existente, backup previo y verificación posterior de conteos/datos.
- [ ] Comparar OpenAPI/ejemplos de respuestas de ambas aplicaciones para cada ruta y verificar frontend/proxy con `/api` y `/uploads`.
- [ ] Completar pruebas de regresión, revisar logs/errores y validar configuración en entorno limpio. Hasta ahora pasan las pruebas focalizadas existentes y se comprobaron `/docs`, `/v3/api-docs`, `/uploads/**`, health y flujos HTTP representativos.

**Salida / puerta:** documentación ejecutable y evidencia de paridad, sin acciones de pérdida de datos por defecto.

### Fase 7 — Corte y retiro del origen (solo cuando se autorice)

- [ ] Respaldar DB y archivos; probar restauración antes del corte.
- [ ] Desplegar Spring Boot en paralelo, validar health y smoke tests con datos controlados.
- [ ] Cambiar proxy/configuración del frontend y observar errores, pedidos y stock.
- [ ] Mantener rollback al servicio Python y no retirar su repositorio hasta aceptación funcional.
- [ ] Acordar por separado ventana de corte, rollback y retiro definitivo.

**Salida / puerta:** aceptación explícita del comportamiento y operación antes de retirar FastAPI.

## 6. Matriz mínima de pruebas y aceptación

- **Contrato HTTP:** ruta, verbo, nombres/casing de campos JSON, status, cuerpo y códigos 204 para todos los endpoints.
- **Validación:** límites de campos, email/dominio, cantidad/precio/stock y cuerpo malformado; 422 con `{error:...}`.
- **Productos:** solo activos en catálogo/detalle, orden por ID, borrado lógico y disponibilidad en historial.
- **Usuarios:** emails en minúsculas, duplicados 409 incluso con carrera, credenciales incorrectas indistinguibles, hash nunca serializado.
- **Google:** credencial inválida, email no verificado, audiencia errónea, dominio rechazado, proveedor inaccesible, cliente no configurado y primer login.
- **Pedidos:** líneas duplicadas, producto inactivo/inexistente, stock exacto/insuficiente, usuario inexistente, rollback y solicitudes concurrentes.
- **Base de datos:** migración desde esquema antiguo y desde esquema actual, preservación de filas, claves foráneas, charset y timestamps.
- **Operación:** conexión ausente hace fallar health; CORS del origen configurado; lectura estática limitada al directorio configurado. No hay endpoint de carga en el origen: no agregar ni probar uno como parte de esta migración.
- **Aceptación:** pruebas automatizadas pasan; aplicación compila y arranca con configuración documentada; frontend puede usarla sin adaptar contratos; no se pierde información de prueba preexistente.

## 7. Riesgos y decisiones pendientes

1. **Base existente futura:** no hay base desplegada con datos en el alcance actual. Si se conecta una posteriormente, revisar DDL/metadata y diseñar baseline antes de habilitar Flyway.
2. **Hash bcrypt:** confirmar longitud/formato y compatibilidad entre la biblioteca Python actual y el encoder Java.
3. **Regla de dominio inconsistente:** el set del origen permite `duocuc.cl`, pero el mensaje de error menciona solo `gmail.com`, `outlook.com` y `duoc.cl`. Por paridad se debe preservar el set efectivo salvo decisión explícita.
4. **Sin autenticación de rutas administrativas:** el origen permite listar usuarios, cambiar roles y operaciones de catálogo sin protección. Registrar el riesgo; no alterar el contrato durante la traducción sin aprobación.
5. **Usuario demo:** el inicializador usa `admin@pokestore.cl` / `admin123`. Mantenerlo solo para entorno demo/local y nunca incorporarlo como credencial de producción.
6. **Archivos locales:** los archivos deben persistir fuera del ciclo de vida del proceso/contenedor; confirmar ubicación y backup en despliegue. Actualmente no hay endpoint multipart de carga aunque exista límite configurado.
7. **Concurrencia de stock:** la comprobación previa del origen puede sufrir carrera. La migración debe reforzar atomicidad para evitar sobreventa sin cambiar las respuestas normales.
8. **Versiones/despliegue:** Java, Spring Boot, estrategia CI y hosting aún no constan en los archivos inspeccionados; definirlos al comienzo, sin actualizar infraestructura ajena al backend.

## 8. Seguimiento

Al comenzar una fase, marcarla como `En curso` en este documento; al terminar, marcar sus tareas y puerta como completas, anotar los comandos y resultados reales, y registrar bloqueos/decisiones. No avanzar a una fase que dependa de una puerta incumplida.

| Fase | Estado | Evidencia / notas |
|---|---|---|
| 0. Preparación y línea base | Completa | Java 21.0.13, Maven 3.9.12, Boot 4.1.2 y Springdoc 3.2.0. El usuario confirmó que no hay base desplegada con datos; frontend React/Vite localizado en `/home/sol/Repos/poke-store`. |
| 1. Esqueleto y configuración | Completa | Tests y `clean package` pasan; `/docs`, OpenAPI y `/uploads` verificados; health comprobado con DB ausente y con MySQL local; contraseña con caracteres especiales probada. |
| 2. Esquema y entidades | Completa para base local vacía | Cinco entidades y relaciones, Flyway V1 y validación JPA contra MySQL 8.4. No hay base existente que migrar. |
| 3. Catálogo y contacto | Implementada; cobertura de regresión pendiente | El catálogo del frontend mostró los ocho productos cargados desde Spring Boot; ampliar pruebas automatizadas. |
| 4. Usuarios y autenticación | Implementada; verificación externa pendiente | Login del administrador demo funcionó desde el frontend. Google requiere credencial real; faltan pruebas automatizadas. |
| 5. Pedidos y stock | Implementada; pruebas transaccionales pendientes | Pedido transaccional con bloqueo de stock; smoke tests de líneas repetidas, historial y stock insuficiente; falta probar rollback/concurrencia en integración. |
| 6. Operación y paridad | En curso | Perfil local inicializa ocho productos y administrador demo de forma idempotente. Build frontend y prueba visual del catálogo/login completados; faltan comparación completa de contratos y regresión. |
| 7. Corte y retiro | No iniciado; requiere autorización | |
