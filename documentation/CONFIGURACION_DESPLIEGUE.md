# Configuración para desplegar Alexa Beauty

**Destino:** Supabase (PostgreSQL e imágenes), Render (API) y Vercel (web). El pago es manual, sin pasarela; solo ADMIN u OPERADOR confirma un abono real por el importe total durante una reserva vigente.

> Estos valores son una plantilla, no credenciales. Sustituya todos los campos `<...>` antes de desplegar. Las correcciones validadas están todavía en el equipo local: GitHub debe recibirlas mediante un commit/push autorizado antes de desplegar `main`. No publique una versión antigua por conectar el repositorio.

## Orden de preparación

1. Cree Supabase y anote sus datos de conexión y Storage.
2. Prepare el servicio Render y el proyecto Vercel para obtener sus nombres y URLs definitivas. Mantenga el lanzamiento comercial pendiente.
3. Configure Render con el origen exacto de Vercel y Vercel con la URL exacta de Render. No use nombres de ejemplo ni suponga que un hostname está disponible.
4. Publique el código actualizado, despliegue la API y después la web. Complete los controles de lanzamiento al final de esta guía.

## 1. Supabase

| Ajuste | Configuración |
|---|---|
| Proyecto | Proyecto dedicado a Alexa Beauty; staging separado de producción |
| Región | La más cercana y compatible con la región elegida en Render |
| Contraseña PostgreSQL | Nueva contraseña segura; guárdela en un gestor de secretos |
| Conexión | Copie **Session pooler** desde **Connect**; puerto `5432`, compatible con IPv4 |
| Usuario session pooler | `postgres.<PROJECT_REF>`; confirme el usuario exacto mostrado |
| Base | `postgres` |
| TLS JDBC | `sslmode=require` |
| Data API | Desactivada: la aplicación accede mediante JDBC y usa autenticación propia |
| Supabase Auth | No requiere configurar usuarios ni redirects para esta aplicación |
| Bucket | `alexa-beauty-public`, público, solo imágenes comerciales |
| Límites bucket | 5 MB; MIME `image/jpeg` e `image/png` |
| Escrituras Storage | Sin políticas de INSERT/UPDATE/DELETE para `anon` o `authenticated`; backend con `service_role` |

**Conexión:** no utilice el pooler de transacciones `6543`. La URL JDBC no lleva usuario ni contraseña embebidos: se suministran por separado. Copie el host real del panel; no reconstruya región o prefijo.

**Clave Storage:** utilice la clave JWT legacy `service_role` del proyecto en `SUPABASE_SERVICE_ROLE_KEY`, exclusivamente en Render. El cliente actual envía esa clave tanto en `apikey` como en `Authorization: Bearer`; no sustituya por una clave `sb_secret_...` sin adaptar y verificar su compatibilidad. La contraseña PostgreSQL, `service_role` y `JWT_SECRET` son tres secretos distintos.

Un bucket público permite descargar objetos por su URL pública sin una política SELECT adicional. No otorgue lectura/listado de toda la base para lograr esto. No almacene comprobantes, reportes Excel o información de clientes en el bucket público.

Flyway aplica automáticamente las migraciones al iniciar la API. Antes del lanzamiento confirme que las tablas nuevas no están expuestas por Data API; si decide activarla en el futuro, requiere permisos y RLS revisados por tabla. El acceso JDBC con privilegios administrativos no queda protegido automáticamente por RLS.

## 2. Render: API Spring Boot

| Campo | Valor |
|---|---|
| Tipo | Web Service |
| Repositorio | `https://github.com/Sebasr0311/Alexa-Beauty` |
| Rama | `main`, después de publicar las correcciones locales |
| Nombre sugerido | `alexa-beauty-api` (use el hostname realmente asignado) |
| Root Directory | `backend` |
| Runtime | Docker |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context | `.` |
| Docker Command | Sin override; utiliza el ENTRYPOINT del Dockerfile |
| Build/Start manual | No configure Maven ni un Start Command por separado |
| Health Check Path | `/api/v1/health/public` |
| Instancias iniciales | Una; revise memoria, CPU y tiempos de arranque |
| Región | Cercana a Supabase |
| Plan | Instancia siempre activa recomendada para producción |

Con Root Directory `backend`, **no** use `./backend/Dockerfile`: las rutas son relativas a esa raíz. El backend escucha `${PORT:8080}`. Render proporciona `PORT`; conserve ese valor sin fijarlo manualmente.

El plan gratuito se suspende tras 15 minutos sin actividad entrante. Durante la suspensión no corre el proceso programado que libera reservas cada 60 segundos. Es útil para pruebas, no para garantizar reservas operativas continuas. El plan siempre activo tiene costo; confirme tarifa, recursos y presupuesto en Render antes de contratar.

### Variables de Render (16)

```dotenv
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://<HOST_SESSION_POOLER>:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres.<PROJECT_REF>
DATABASE_PASSWORD=<CONTRASENA_POSTGRESQL>
JWT_SECRET=<SECRETO_ALEATORIO_INDEPENDIENTE>
FRONTEND_ORIGIN=https://<HOST_WEB_REAL>
ADMIN_INITIAL_EMAIL=<CORREO_ADMIN_REAL>
ADMIN_INITIAL_PASSWORD=<CONTRASENA_ADMIN_INDEPENDIENTE_16_A_72_CARACTERES>
SUPABASE_URL=https://<PROJECT_REF>.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<JWT_LEGACY_SERVICE_ROLE>
SUPABASE_STORAGE_BUCKET=alexa-beauty-public
WHATSAPP_BUSINESS_NUMBER=<NUMERO_REAL_CON_CODIGO_PAIS_SOLO_DIGITOS>
RESERVATION_MINUTES=45
DB_POOL_SIZE=5
LOGIN_BUDGET_PER_MINUTE=30
CHECKOUT_BUDGET_PER_MINUTE=120
```

`FRONTEND_ORIGIN` debe ser exactamente la URL HTTPS canónica de la web: sin ruta, wildcard ni barra final. `SUPABASE_URL` también es solo el origen HTTPS. No deje marcadores o contactos de demostración: el perfil `prod` los rechaza.

Genere `JWT_SECRET` localmente en PowerShell y guárdelo como secreto; no lo publique en chats, Git o capturas:

```powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes); [Convert]::ToBase64String($bytes) }
finally { $rng.Dispose() }
```

El administrador se inicializa con las credenciales configuradas, no con las de demostración. Reiniciar no restablece su contraseña. Mantenga las variables bootstrap protegidas y disponibles mientras sean requeridas por la validación de arranque.

## 3. Vercel: web Next.js

| Campo | Valor |
|---|---|
| Repositorio | `Sebasr0311/Alexa-Beauty` |
| Production Branch | `main`, con las correcciones publicadas |
| Root Directory | `frontend` |
| Framework Preset | Next.js |
| Node.js Version | `22.x` |
| Install Command | `npm ci` |
| Build Command | `npm run build` |
| Output Directory | Automático de Next.js; no configure exportación estática |

### Variables de Vercel (3)

```dotenv
API_BACKEND_URL=https://<HOST_RENDER_REAL>/api/v1
NEXT_PUBLIC_SITE_URL=https://<HOST_WEB_REAL>
NEXT_PUBLIC_WHATSAPP_BUSINESS_NUMBER=<MISMO_NUMERO_REAL_DEL_BACKEND>
```

Sin barra final en las URLs. `API_BACKEND_URL` es server-only, pero debe estar disponible en el build: `next.config.ts` genera el proxy `/api/backend/*`. Las dos `NEXT_PUBLIC_*` son públicas. **No** copie `JWT_SECRET`, credenciales PostgreSQL ni `service_role` a Vercel.

Configure el entorno Production. Un cambio de estas variables exige un nuevo despliegue/build. Para Preview use API, base y origen de staging independientes; no abra CORS con `*` ni conecte ramas de prueba a datos de clientes.

Si cambia a un dominio propio, actualice `FRONTEND_ORIGIN` en Render y `NEXT_PUBLIC_SITE_URL` en Vercel, redespliegue y repita las pruebas de sesión desde el dominio canónico. No mantenga dos orígenes intercambiables sin verificar el contrato de autenticación.

## Controles antes de abrir la tienda

La evidencia local existente registra 61 pruebas con PostgreSQL 14.24, recorrido de navegador y restauración comparada de 19 tablas. No equivale a validación del despliegue cloud. Consulte [el informe local](PRODUCTION_READINESS_2026-10-13.md).

- [ ] API HTTPS devuelve HTTP 200 y `UP` en `/api/v1/health/public`; Flyway inicia sin errores.
- [ ] Login desde la web HTTPS usa cookie HttpOnly, Secure y SameSite=Lax; logout invalida la sesión del navegador. No hay token en localStorage.
- [ ] Escrituras legítimas funcionan por `/api/backend/*`; solicitudes sin origen permitido no acceden a operaciones autenticadas.
- [ ] Catálogo, precios, stock y número WhatsApp son reales; no hay productos ficticios.
- [ ] Subir/eliminar JPEG y PNG mediante panel funciona; URL pública sirve la imagen. Anónimos no pueden escribir en Storage.
- [ ] Pedido de prueba → WhatsApp → confirmación manual → inventario → Excel correcto; importe parcial/cero y reserva vencida no confirman.
- [ ] Reserva expira y libera stock con el servicio activo, incluso sin nuevas visitas.
- [ ] Backup del entorno nuevo restaurado en una base aislada y verificado; incluya copia de imágenes, no solo PostgreSQL.
- [ ] Alertas de salud/logs y responsable operativo definidos. Pruebe recuperación de despliegue anterior y respaldos antes de migraciones.
- [ ] Envíos y condiciones comerciales acordados: actualmente `shippingFee` es cero; el transporte coordinado por fuera no figura como cobrado en el total.

**No abrir ventas mientras falte algún control.** No haga pruebas destructivas ni restaure respaldos sobre la base de producción.

## Referencias oficiales

- [Supabase: conexión PostgreSQL](https://supabase.com/docs/guides/database/connecting-to-postgres), [seguridad Data API](https://supabase.com/docs/guides/api/securing-your-api), [Storage y control de acceso](https://supabase.com/docs/guides/storage/security/access-control), [claves API](https://supabase.com/docs/guides/getting-started/api-keys).
- [Render: monorepos y rutas relativas](https://render.com/docs/monorepo-support), [limitaciones del plan gratuito](https://render.com/docs/free).
- [Vercel: configuración del build](https://vercel.com/docs/builds/configure-a-build), [variables de entorno](https://vercel.com/docs/environment-variables), [versiones Node.js](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions).
