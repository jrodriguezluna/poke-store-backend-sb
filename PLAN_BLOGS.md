# Plan: blogs administrables desde la base de datos

**Estado:** implementado y validado localmente; se conservan debajo el diseño y las decisiones acordadas.  
**Repositorios:** backend Spring Boot (`poke-store-backend-sb`) y frontend React/Vite (`poke-store`).

**Decisiones acordadas:** acceso de escritura controlado solo desde la GUI para cuentas admin, consistente con los paneles existentes; imágenes por URL HTTP(S) o ruta existente, sin carga de archivos.

## 1. Estado actual

- `src/Pages/Blogs.jsx` contiene dos artículos escritos directamente en un arreglo JavaScript. La página muestra la lista y abre el detalle dentro del mismo componente, sin ruta individual.
- Los artículos tienen etiqueta, título, resumen, autor, imagen, texto alternativo, introducción, secciones ordenadas y consejo.
- El frontend ya tiene una administración lateral con pestañas de Productos y Usuarios. Los componentes administrables viven en `src/components/admin/` y utilizan servicios de `src/services/api.js`.
- El backend tiene Flyway, JPA, servicios y controladores por recurso. La migración actual crea las tablas de usuarios, productos, pedidos, ítems y contacto.
- El panel actual oculta sus controles para usuarios que no sean admin, pero el backend no autentica ni autoriza esas operaciones. El login no emite un token que permita al servidor verificar el rol.

## 2. Objetivo y alcance propuesto

Guardar los artículos en MySQL y permitir que los usuarios visiten los blogs desde la página pública. Agregar al panel de administración una sección para crear, editar y eliminar artículos.

Incluye:

- Persistencia, migración de esquema y carga inicial no destructiva de los dos artículos actuales.
- API pública de consulta y API de administración para altas, cambios y bajas.
- Reemplazar los datos hardcodeados en el frontend por datos de la API.
- Editor en el panel existente, validaciones, mensajes de error/éxito y confirmación de eliminación.
- Pruebas automatizadas y una verificación integrada con MySQL local.

Fuera de alcance inicial:

- Borradores, publicación programada, categorías múltiples, comentarios, búsqueda, paginación y editor de texto enriquecido.
- Carga de imágenes al servidor, salvo que se elija explícitamente esa opción.

## 3. Modelo de datos propuesto

Crear una migración Flyway nueva (`V2`); no modificar `V1` ya aplicada en la base local.

### `blogs`

- `id`: entero autoincremental.
- `etiqueta`: categoría o rótulo visible.
- `titulo`: título.
- `resumen`: texto para la tarjeta.
- `autor`: nombre visible.
- `imagen`: URL o ruta de imagen.
- `texto_alternativo`: texto accesible de la imagen.
- `introduccion`: contenido inicial del artículo.
- `consejo`: contenido opcional para el bloque “Consejo PokeStore”.
- `creado_en` y `actualizado_en`: fechas de auditoría.

### `blog_secciones`

- `id`, `blog_id`, `orden`, `titulo` y `texto`.
- Clave foránea a `blogs`; borrar un blog elimina sus secciones.
- La columna `orden` conserva la secuencia mostrada por el editor y la página pública.

La respuesta JSON mantendrá los nombres que consume el componente actual (`textoAlternativo`, `introduccion`, `secciones`, etc.). El `id` pasará a ser el identificador numérico generado por la base; actualmente el componente no lo utiliza como ruta ni como enlace persistente.

## 4. API propuesta

| Método y ruta | Acceso | Resultado |
|---|---|---|
| `GET /api/blogs` | Público | Lista ordenada de artículos |
| `GET /api/blogs/{id}` | Público | Detalle; 404 si no existe |
| `POST /api/blogs` | GUI admin; API sin autenticación | Crea y responde 201 |
| `PUT /api/blogs/{id}` | GUI admin; API sin autenticación | Reemplaza campos y secciones; responde 200 |
| `DELETE /api/blogs/{id}` | GUI admin; API sin autenticación | Elimina artículo y secciones; responde 204 |

El DTO de escritura contendrá los campos visibles del modelo y una lista `secciones`. Se validarán campos obligatorios, longitudes, URL/ruta de imagen y los datos de cada sección. Los errores conservarán el contrato común `{ "error": "..." }`.

## 5. Frontend

1. Añadir métodos `listarBlogs`, `obtenerBlog`, `crearBlog`, `actualizarBlog` y `eliminarBlog` en `src/services/api.js`.
2. Cambiar `src/Pages/Blogs.jsx` para cargar la lista desde la API y mostrar estados de carga, error y lista vacía. Mantener el diseño de tarjetas y detalle, renderizando las secciones recibidas.
3. Agregar “Blogs” a las secciones del panel lateral de administración.
4. Crear `src/components/admin/PanelBlogs.jsx`, siguiendo el patrón de `PanelProductos`: listado, modal de alta/edición, confirmación al borrar, indicador de guardado y mensajes de error/éxito.
5. El editor permitirá agregar, quitar y reordenar secciones. Al guardar, la página pública vuelve a cargar los datos.
6. La opción de administración se mostrará solo a cuentas con rol `admin`, igual que el panel actual; esto solo controla la interfaz y no sustituye la autorización del servidor.

## 6. Seed y compatibilidad

- Migrar los dos artículos actuales a registros de seed.
- Insertarlos solo cuando la tabla de blogs esté vacía; no reinsertarlos en cada arranque ni sobrescribir cambios hechos por un administrador.
- Mantener su texto, orden, imágenes y alt text actuales.
- El seed y la migración seguirán ejecutándose únicamente en el perfil `local`.

## 7. Seguridad: decisión necesaria antes de implementar escrituras

Los controladores actuales no tienen autenticación del backend. El frontend conserva el usuario en `localStorage`, y cualquiera podría llamar directamente a la API aunque la pestaña de administración no aparezca. Para esta primera versión se acordó mantener el patrón existente: la GUI solo muestra la gestión a admins, pero las mutaciones no quedarán realmente protegidas en el servidor.

Una mejora futura para restringir también la API requerirá autenticación verificable (por ejemplo, emitir y validar un token en login, registro/login Google y solicitudes del frontend). No se deben simular credenciales de administrador mediante un header o un rol enviado por el navegador: el servidor no podría confiar en ellos.

## 8. Decisión de imágenes

La página actual referencia imágenes estáticas del frontend y el backend no tiene un endpoint de carga. Se acordó que el formulario acepte una URL HTTP(S) o una ruta de imagen accesible, conservando las rutas actuales de los artículos sembrados. La carga multipart y almacenamiento de imágenes queda fuera de este alcance.

## 9. Orden de implementación

1. Crear migración Flyway V2 y entidades/repositorios, incluyendo el orden de secciones.
2. Implementar DTOs, validaciones, servicio transaccional y endpoints.
3. Migrar los dos artículos actuales al seed local idempotente.
4. Conectar la página pública a la API.
5. Añadir el panel y el editor de blogs, visible solo para admins.
6. Añadir pruebas backend y frontend, y verificar CRUD completo en navegador contra MySQL local.
7. Actualizar README y este plan con resultados y cualquier limitación. **Completado.**

## 10. Criterios de aceptación

- [x] Flyway aplica V2 en MySQL local y Hibernate valida el esquema.
- [x] El seed local inserta los dos artículos actuales solo cuando no hay blogs.
- [x] Los artículos públicos se consultan desde MySQL; la vista conserva tarjeta, detalle, secciones y consejos.
- [x] El panel admin permite crear, editar y eliminar artículos y secciones.
- [x] Un blog inexistente devuelve 404; validaciones y errores mantienen el formato común.
- [x] La relación de secciones usa borrado en cascada y `orphanRemoval`.
- [x] La GUI oculta la gestión a cuentas no-admin. Las rutas mutables no tienen autorización backend; esta limitación se documenta explícitamente.
- [x] Pasan el paquete backend y las pruebas/build del frontend.

## 11. Resultado de implementación y verificación

- La migración `V2__create_blogs_schema.sql` se aplicó correctamente en MySQL 8.4 y Hibernate validó las entidades contra el esquema.
- El perfil `local` incorpora los dos artículos existentes si `blogs` está vacía; no reescribe los registros existentes.
- `mvn clean package` pasa. En el frontend pasan `npm run build` y `npm run test:coverage` (35/35 pruebas).
- La API está implementada en `GET /api/blogs`, `GET /api/blogs/{id}`, `POST /api/blogs`, `PUT /api/blogs/{id}` y `DELETE /api/blogs/{id}`.
- El backend fue iniciado en el puerto alternativo 18000 para el smoke test porque otro proceso ya ocupaba el puerto habitual 8000. El seed y las operaciones CRUD se verifican contra la base local.
- Las rutas mutables no comprueban identidad/rol en el backend. El acceso solo se oculta en la GUI y no debe considerarse autorización para un despliegue expuesto a Internet.
