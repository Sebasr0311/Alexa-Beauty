# GUÍA DE DESPLIEGUE EN PRODUCCIÓN — ALEXA BEAUTY

Esta guía describe el procedimiento oficial y reproducible para desplegar la plataforma **Alexa Beauty** en entornos de staging y producción utilizando **Supabase (PostgreSQL + Storage)**, **Render (Backend Spring Boot)** y **Vercel (Frontend Next.js)**.

---

## 1. Arquitectura de Despliegue

```
┌─────────────────────────────────────────────────────────────────┐
│                       ARQUITECTURA CLOUD                        │
└─────────────────────────────────────────────────────────────────┘

   [Cliente Web / Móvil]
           │
           ▼ (HTTPS)
   ┌───────────────────────┐
   │    Vercel (Edge)      │  Next.js 15+ App Router
   │    Frontend PWA       │  Palette: #711A38 / #FCF8F2
   └──────────┬────────────┘
              │ (REST / JSON)
              ▼
   ┌───────────────────────┐
   │    Render (Docker)    │  Spring Boot 3.3.5 / Java 21 LTS
   │    Backend Web Svc    │  Multi-stage container (no-root)
   │  /api/v1/health/public│  Cron de expiración de reservas (60s)
   └─────┬───────────┬─────┘
         │           │
         │ (JDBC SSL)│ (REST HTTPS + Service Role)
         ▼           ▼
   ┌───────────┐ ┌──────────────┐
   │ PostgreSQL│ │   Storage    │
   │  Supabase │ │   Supabase   │
   └───────────┘ └──────────────┘
```

---

## 2. Paso 1: Configuración de Supabase (Base de Datos y Almacenamiento)

### 2.1. Base de Datos PostgreSQL
1. Crea un proyecto en [Supabase](https://supabase.com).
2. En **Project Settings > Database**, copia la URL de conexión directa:
   ```
   postgresql://postgres.[REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:5432/postgres
   ```
3. Las migraciones Flyway (`V1`, `V2`, `V3`) se aplicarán **automáticamente** al arrancar el backend Spring Boot.

### 2.2. Supabase Storage (Imágenes de Catálogo)
1. Ve a **Storage** en el panel de Supabase.
2. Crea un nuevo bucket llamado: `alexa-beauty-public`.
3. Activa la opción **Public bucket** para permitir lectura directa de imágenes vía CDN.
4. En **Policies**, asegúrate de que:
   - Lectura pública: `SELECT` permitido para rol `anon` y `authenticated`.
   - Inserción y eliminación: Permitido únicamente mediante la clave `service_role` (utilizada exclusivamente por el backend).

---

## 3. Paso 2: Despliegue del Backend en Render

### 3.1. Creación del Servicio Web
1. En [Render Dashboard](https://dashboard.render.com), selecciona **New > Web Service**.
2. Conecta el repositorio de GitHub: `https://github.com/Sebasr0311/Alexa-Beauty.git`.
3. Configuración del servicio:
   - **Name:** `alexa-beauty-api`
   - **Region:** Ohio (US East) o Frankfurt (según proximidad a Supabase).
   - **Branch:** `main`
   - **Root Directory:** `backend` (o déjalo en blanco si indicas la ruta del Dockerfile).
   - **Runtime:** `Docker`
   - **Dockerfile Path:** `./backend/Dockerfile`
   - **Plan:** `Starter` (mínimo recomendado para evitar suspensión en frío y garantizar la ejecución del worker de expiración de reservas cada 60s).

### 3.2. Variables de Entorno en Render
En la pestaña **Environment**, define las siguientes variables:

| Variable | Valor de Ejemplo / Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://db.[REF].supabase.co:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | `[Tu contraseña de Supabase]` |
| `SUPABASE_URL` | `https://[REF].supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | `[Clave secreta service_role de Supabase]` |
| `SUPABASE_STORAGE_BUCKET` | `alexa-beauty-public` |
| `WHATSAPP_BUSINESS_NUMBER` | `573001234567` (Número verificado con código país) |
| `FRONTEND_ORIGIN` | `https://alexabeauty.com` (o URL de Vercel) |
| `JWT_SECRET` | `[Clave segura de al menos 256 bits generada con openssl rand -base64 64]` |
| `RESERVATION_MINUTES` | `45` |

### 3.3. Configuración de Salud (Health Check)
- **Health Check Path:** `/api/v1/health/public`
- **Port:** `8080` (expuesto por el Dockerfile)

---

## 4. Paso 3: Despliegue del Frontend en Vercel

### 4.1. Importación del Proyecto
1. En [Vercel Dashboard](https://vercel.com/dashboard), selecciona **Add New > Project**.
2. Importa el repositorio: `Sebasr0311/Alexa-Beauty`.
3. Ajustes del proyecto:
   - **Framework Preset:** `Next.js`
   - **Root Directory:** `frontend`
   - **Build Command:** `next build`
   - **Output Directory:** `.next`

### 4.2. Variables de Entorno en Vercel
En la pestaña **Environment Variables**:

| Variable | Valor |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `https://alexa-beauty-api.onrender.com/api/v1` |
| `NEXT_PUBLIC_SITE_URL` | `https://alexabeauty.com` |

4. Presiona **Deploy**. El build generará las 12 rutas estáticas y dinámicas.

---

## 5. Paso 4: Checklist de Verificación Post-Despliegue

- [ ] **Health Check:** `curl https://alexa-beauty-api.onrender.com/api/v1/health/public` devuelve HTTP 200 con `status: "UP"`.
- [ ] **CORS:** Petición OPTIONS desde el dominio de Vercel devuelve cabeceras `Access-Control-Allow-Origin` correctas.
- [ ] **Semilla de Administrador:** Ingreso a `/admin` con credenciales iniciales (`admin@alexabeauty.com` / `Admin123*Alexa`).
- [ ] **Cambio de Contraseña:** Inmediata actualización de la clave del usuario `admin` en base de datos.
- [ ] **Catálogo Público:** Carga fluida de `/` y `/catalogo` con imágenes servidas desde Supabase Storage.
- [ ] **Flujo de Checkout:** Creación de un pedido de prueba y verificación de la redirección correcta a WhatsApp con referencia `AB-YYYY-NNNNNN`.
- [ ] **Expiración:** Comprobar en logs de Render que `ReservationExpirationJob` se ejecuta periódicamente sin errores.
