# CHANGELOG — ALEXA BEAUTY

Todas las modificaciones notables a este proyecto serán documentadas en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

### Added - 2026-10-12
- **Épica 1: Datos, Migraciones y Seguridad**
  - **Migraciones Flyway (`V1`, `V2`, `V3`):**
    - `V1__initial_schema.sql`: Esquema de catálogo, productos, variantes con bloqueo optimista (`version`), imágenes y parámetros con restricciones `CHECK` de stock no negativo y `stock_reserved <= stock_on_hand`.
    - `V2__inventory_and_orders.sql`: Libro de movimientos (`inventory_movements`), recepciones (`stock_receipts`), órdenes, reservas temporales de stock, pagos auditados, historial de estados y tablas de diagnóstico para importación Excel.
    - `V3__seed_data.sql`: Semilla con parámetros de negocio, 4 categorías iniciales (`Rostro`, `Labios`, `Ojos`, `Accesorios`), 3 marcas y usuario admin con contraseña hasheada en BCrypt.
  - **Entidades y Repositorios JPA:**
    - Modelo relacional inmutable completo para los 9 módulos funcionales.
  - **Seguridad y Autenticación:**
    - Spring Security 6 con `JwtTokenProvider`, filtro `JwtAuthenticationFilter`, soporte para Bearer Token y cookies `HttpOnly`.
    - Endpoints de autenticación: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`.
    - Manejadores de excepción RFC 7807 (`JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`).
  - **Pruebas Automatizadas:**
    - `FlywayMigrationTest` validando las 3 migraciones y datos semilla en base de datos.
    - `AuthIntegrationTest` validando logins exitosos, rechazo por credenciales inválidas, extracción de perfil, protección de rutas y logout.
    - 13 pruebas unitarias y de integración ejecutadas y aprobadas (`BUILD SUCCESS`).

### Added - 2026-10-10
- **Épica 0: Scaffold y Fundaciones**
  - **Backend (Spring Boot 3.3.5 / Java 21 LTS):**
    - Configuración Maven con Flyway, Postgres, Spring Security, JPA, Validation, Actuator, Apache POI 5.3.0, Springdoc OpenAPI 2.6.0.
    - Manejador global de excepciones (`GlobalExceptionHandler`) conforme a RFC 7807 (`ProblemDetail`).
    - Envoltorios de respuesta estándar `ApiResponse` y `PagedResponse`.
    - Endpoint público de verificación `/api/v1/health/public`.
    - Configuración de perfiles `application.yml`, `application-dev.yml` (Postgres local) y `application-test.yml` (H2).
    - Multi-stage `Dockerfile` optimizado con usuario no-root.
    - Pruebas unitarias de contexto y controlador de salud ejecutadas y aprobadas (`BUILD SUCCESS`).
  - **Frontend (Next.js 15+ / React 19 / TypeScript / Tailwind CSS):**
    - Configuración de tokens de diseño (`wine: #711A38`, `wine-dark: #4C1126`, `mauve: #B7748C`, `cream: #FCF8F2`, `ink: #2B2025`).
    - Layout responsive mobile-first (360px–430px) con cabecera fija, barra de anuncios y navegación inferior en móvil.
    - Vista de inicio (`/`) con banner hero, badges de beneficios y productos destacados.
    - Vista de catálogo (`/catalogo`) con buscador, filtros de categorías y productos con precios COP.
    - Vista de carrito (`/carrito`) persistido en `localStorage` con formulario express y aviso de reserva de stock por WhatsApp.
    - Vista previa de administración (`/admin`) con tarjetas de KPI y tabla de pedidos.
    - Formateador de moneda colombiana `formatCOP`.
    - Build de producción verificado sin errores de tipado ni advertencias (`npm run build`).
  - **CI/CD & Documentación:**
    - Workflows de GitHub Actions `.github/workflows/ci-backend.yml` y `ci-frontend.yml`.
    - Documentación viva: `TODO_IMPLEMENTACION.md`, `DECISIONES.md`, `README.md`, `.env.example`, `docker-compose.yml`.
