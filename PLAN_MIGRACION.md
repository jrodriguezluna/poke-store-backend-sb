# Plan de migración: PokéStore Backend (FastAPI → Spring Boot)

**Estado:** migración en curso; fase 1 implementada y validada sin MySQL. La inspección del esquema y la validación positiva de conexión siguen pendientes por inaccesibilidad de la DB desde este entorno.  
**Origen:** `/home/sol/Repos/poke-store-backend`  
**Destino:** `/home/sol/Repos/poke-store-backend-sb`  
**Objetivo:** sustituir la implementación FastAPI por una aplicación Java/Spring Boot, conservando el contrato consumido por el frontend y permitiendo conectar la nueva aplicación a la base MySQL existente sin perder datos.

Este documento será la referencia para ejecutar la migración. Al avanzar, se actualizarán aquí las casillas y decisiones, junto con los resultados de validación.

## 1. Alcance y criterios rectores

### Incluido

- Aplicación Spring Boot con API REST, persistencia MySQL, validaciones, configuración por entorno y documentación OpenAPI.
- Migración de los recursos actuales: productos, usuarios, pedidos y contacto.
- Conservación de la base de datos, el formato de respuestas y los códigos HTTP que utiliza el frontend.
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

Usar las tablas existentes, preservando nombres de tabla/columna, tipos, claves y relaciones. Confirmar el esquema real de MySQL antes de definir las anotaciones/migraciones definitivas.

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
- [x] Revisar estructura, dependencias y contratos del backend de origen. No se encontró frontend en los directorios disponibles.
- [ ] Inspeccionar metadata de MySQL en modo solo lectura. Intento realizado, pero el host configurado no resuelve desde este entorno; no se modificó la DB. Esta limitación bloquea la fase 2 hasta obtener acceso o un DDL/export seguro.
- [x] Registrar Java 21.0.13 y Maven 3.9.12; seleccionar Spring Boot 4.1.2 y Springdoc 3.2.0. Java y Maven quedaron instalados para compilar el proyecto.
- [x] Definir paquete base `cl.pokestore.backend` y comandos Maven; no se encontró infraestructura de despliegue que requiera un perfil adicional.

**Salida / puerta:** plataforma definida y origen intacto. La falta de acceso a MySQL quedó registrada; se puede ejecutar la fase 1, pero no comenzar la fase 2 sin verificar el esquema.

### Fase 1 — Esqueleto ejecutable y configuración

- [x] Crear proyecto Maven/Spring Boot 4.1.2 para Java 21 y la aplicación principal en `cl.pokestore.backend`.
- [x] Configurar variables de entorno, conexión MySQL y pool; desactivar cambios automáticos de esquema y configurar el dialecto para permitir que el servidor arranque sin DB disponible.
- [ ] Verificar conexión a MySQL y contraseña con caracteres especiales cuando el host sea accesible.
- [x] Implementar CORS, estáticos `/uploads/**`, OpenAPI y `GET /api/health` con comprobación efectiva de DB.
- [x] Añadir `.gitignore`, plantilla de variables sin secretos y README con instalación/ejecución.
- [x] Ejecutar `mvn -f /home/sol/Repos/poke-store-backend-sb/pom.xml test`: compilación y dos pruebas de health correctas.
- [x] Arrancar sin DB y comprobar que `GET /api/health` responde 500 con `{"error":"No se pudo completar la operación de base de datos"}`.
- [x] Empaquetar con `mvn -f /home/sol/Repos/poke-store-backend-sb/pom.xml clean package`; smoke tests HTTP verifican `/docs` (redirige y carga), `/v3/api-docs` (200) y `/uploads/**` (200).
- [ ] Comprobar el health positivo (`{"ok":true}`) contra un MySQL de pruebas.

**Salida / puerta:** el esqueleto compila, arranca sin conectividad a MySQL y no incluye secretos. La prueba positiva de conexión queda pendiente; completar antes de considerar cerrada esta fase.

### Fase 2 — Esquema, entidades y compatibilidad de datos

- [ ] Implementar las cinco entidades y sus claves/relaciones, usando nombres exactos de las tablas existentes.
- [ ] Comparar el mapeo JPA con el esquema MySQL existente; comprobar nullable, defaults, índices, collation, timestamps y generación de IDs.
- [ ] Crear migraciones Flyway aditivas para columnas ausentes y backfill de categoría, sin borrar datos.
- [ ] Definir procedimiento de baseline controlado para una BD ya inicializada; bloquear/evitar migraciones destructivas en el perfil normal.
- [ ] Probar lectura/escritura y relaciones contra MySQL compatible con producción.

**Salida / puerta:** una BD existente puede ser reconocida/migrada sin pérdida; el modelo coincide con datos históricos.

### Fase 3 — Catálogo y contacto

- [ ] Migrar DTO, validaciones y endpoints de productos, incluido filtro de activos, orden, soft delete y respuesta 204.
- [ ] Preservar el valor `tipo` al serializar aunque actualmente no sea editable en `ProductoIn`.
- [ ] Migrar `/api/contacto` y respuesta 201 `{ok:true}`.
- [ ] Implementar errores de no encontrado, validación y URL de imagen en el formato acordado.
- [ ] Añadir pruebas de integración de códigos, JSON, filtros, orden y soft delete.

**Salida / puerta:** contratos de catálogo/contacto verificados contra casos representativos del backend Python.

### Fase 4 — Usuarios y autenticación

- [ ] Migrar registro, login, listado, actualización y eliminación; conservar normalización de email, límites, unicidad y manejo de usuario con pedidos.
- [ ] Implementar bcrypt compatible con hashes anteriores y probar login de un hash existente.
- [ ] Implementar validación de dominio y Google ID token; conservar errores de configuración, proveedor y token.
- [ ] Confirmar la política actual de autorización: rutas de listado/edición/borrado y capacidad de cambiar `rol` no están protegidas en origen. Preservar paridad en esta migración o solicitar aprobación para endurecer seguridad en un cambio separado; no introducir filtros que bloqueen el frontend por accidente.
- [ ] Probar colisiones concurrentes de email y login federado de primer acceso.

**Salida / puerta:** flujos locales y Google comprobados, sin exponer `password_hash` ni romper contraseñas almacenadas.

### Fase 5 — Pedidos y consistencia de stock

- [ ] Migrar DTO con nombres JSON camelCase `usuarioId`, `productoId` y `precioUnit`.
- [ ] Implementar consolidación de líneas repetidas, rechazo de usuario/productos inexistentes o inactivos y errores de stock.
- [ ] Calcular total con los precios vigentes; persistir pedido/items y descuento de stock en una transacción.
- [ ] Definir y probar estrategia de concurrencia (bloqueo de filas o actualización condicional) que impida que dos pedidos consuman el mismo stock disponible.
- [ ] Migrar historial ordenado descendentemente y mantener snapshots `precio_unit` y nombre de producto según la semántica actual.
- [ ] Probar rollback completo en fallas y asociación de items con productos/pedidos.

**Salida / puerta:** no hay pedido parcial, stock negativo ni discrepancia entre total y detalle bajo errores y concurrencia.

### Fase 6 — Herramientas operativas y paridad

- [ ] Proporcionar comando/documentación para preparar una BD vacía con datos demo, separado del arranque y explícitamente destructivo si recrea tablas.
- [ ] Documentar migración de BD existente, backup previo y verificación posterior de conteos/datos.
- [ ] Comparar OpenAPI/ejemplos de respuestas de ambas aplicaciones para cada ruta y verificar frontend/proxy con `/api` y `/uploads`.
- [ ] Completar pruebas de regresión, revisar logs/errores y validar configuración en entorno limpio.

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

1. **Esquema real no inspeccionado:** el origen tiene migración aditiva, pero el esquema productivo podría diferir. Es obligatorio revisar DDL/metadata antes de Flyway.
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
| 0. Preparación y línea base | Completa con restricción | Java 21.0.13, Maven 3.9.12, Boot 4.1.2, Springdoc 3.2.0. El host MySQL no resuelve en este entorno; no se modificó la DB. |
| 1. Esqueleto y configuración | En curso | `clean package` pasa; `/docs`, OpenAPI y `/uploads` verificados; health negativo da 500 `{error:...}`. Falta comprobar conexión positiva a un MySQL accesible. |
| 2. Esquema y entidades | Bloqueada | Requiere acceso a MySQL o un DDL/export seguro; el host configurado no resuelve desde este entorno. |
| 3. Catálogo y contacto | Pendiente | |
| 4. Usuarios y autenticación | Pendiente | |
| 5. Pedidos y stock | Pendiente | |
| 6. Operación y paridad | Pendiente | |
| 7. Corte y retiro | No iniciado; requiere autorización | |
