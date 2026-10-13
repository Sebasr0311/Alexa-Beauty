# GUÍA DE DESPLIEGUE EN PRODUCCIÓN — ALEXA BEAUTY

Esta guía describe el procedimiento oficial y reproducible para desplegar la plataforma **Alexa Beauty** en entornos de staging y producción utilizando **Supabase (PostgreSQL + Storage)**, **Render (Backend Spring Boot)** y **Vercel (Frontend Next.js)**.

## Controles obligatorios antes de publicar (13 de octubre de 2026)

- No hay pasarela de pago: el cliente reserva el pedido y coordina por WhatsApp. Solo ADMIN u OPERADOR registra un pago manual comprobado en el panel. El importe debe coincidir exactamente con el total y la reserva debe seguir vigente. No confirmar por una captura de pantalla sin comprobar el abono real.
- La web usa `/api/backend/*`, reenviado por Next.js al `API_BACKEND_URL` HTTPS del servidor. La cookie de sesión es HttpOnly, Secure en `prod`, sin Domain, SameSite=Lax. No usar el antiguo `NEXT_PUBLIC_API_BASE_URL`, `SESSION_SECRET` ni `STORAGE_BUCKET_PUBLIC`: las variables reales están en `.env.example`.
- Configurar el origen exacto HTTPS de la web, sin barra final. Las solicitudes de escritura deben conservar `Origin` y `X-Requested-With: XMLHttpRequest` a través del proxy.
- Las variables de backend se inyectan en Render; Spring Boot no lee automáticamente el archivo `.env` de la raíz. Las variables `NEXT_PUBLIC_*` se fijan al compilar la web. No compartir claves JWT o service_role con Vercel ni con el navegador.
- El primer arranque `prod` desactiva el administrador de demostración y crea/configura el administrador indicado. Los reinicios posteriores no restablecen la contraseña. Mantener protegidas las credenciales de bootstrap; cualquier recuperación posterior debe ser explícita.
- El backend tiene presupuestos globales por instancia para login (30/min) y checkout (120/min), configurables. Son protección básica, no límites distribuidos por cliente. Configurar límites adicionales en el borde confiable y probarlos en staging; no confiar en X-Forwarded-For suministrado por el cliente.
- Crear y comprobar el bucket público solo para imágenes del catálogo. No colocar comprobantes de pago ni datos privados en ese bucket. La carga permite JPEG/PNG decodificados, hasta 5 MB y 16 megapíxeles.
- Respaldar PostgreSQL antes de aplicar V4. Es una migración aditiva; volver a la imagen anterior no elimina sus columnas. Probar restauración en una base aislada antes del lanzamiento.
- Verificar flujo real en staging sobre PostgreSQL: catálogo → pedido → pago manual → inventario → Excel; además, confirmaciones simultáneas y expiración de reservas. No publicar si estos controles faltan.
- Acordar costos y cobertura de envío: actualmente `shippingFee` es cero. No presentar como cobrado un transporte coordinado fuera del total del sistema.
- Verificar dominio, TLS, número WhatsApp real, catálogo/precios/stock iniciales, alertas y responsable operativo. Esta guía no acredita que esos recursos ya existan.

La evidencia local vigente está en `documentation/PRODUCTION_READINESS_2026-10-13.md`; el informe anterior no constituye autorización de lanzamiento.

---

## Configuración y procedimiento vigentes

Use [CONFIGURACION_DESPLIEGUE.md](CONFIGURACION_DESPLIEGUE.md) como referencia única para los campos del panel, variables y orden de despliegue de Supabase, Render y Vercel. Corrige las rutas Docker, elimina credenciales de demostración y separa secretos por servicio.

### Resumen operativo

1. Supabase: conexión Session pooler IPv4 en puerto 5432, JDBC con TLS; Data API desactivada y bucket público `alexa-beauty-public` solo para JPEG/PNG comerciales.
2. Render: Web Service Docker, raíz `backend`, Dockerfile `./Dockerfile`, contexto `.`, sin override de ENTRYPOINT, health `/api/v1/health/public`. Preferir instancia siempre activa: la gratuita se suspende y detiene la expiración programada.
3. Vercel: Next.js, raíz `frontend`, Node 22.x, `npm ci`, `npm run build`, salida automática. Solo las tres variables frontend de la referencia.
4. Configurar URLs canónicas HTTPS cruzadas, publicar las correcciones locales autorizadas y desplegar API antes de web. No sustituir claves por valores de demostración.
5. Completar todos los controles cloud de la referencia antes de abrir ventas. Las pruebas locales aprobadas no acreditan HTTPS, cookies o Storage desplegados.

No se instala pasarela: el cliente coordina por WhatsApp y el administrador comprueba el abono real antes de confirmarlo. No restaurar respaldos ni ejecutar pruebas destructivas sobre producción.