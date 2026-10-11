# Alexa Beauty 💄✨

> Plataforma de comercio electrónico para cosméticos y maquillaje con catálogo mobile-first, inventario transaccional, pedidos asistidos por WhatsApp Business y panel administrativo de ventas y recepción de mercancía.

---

## 🏛 Arquitectura y Stack Tecnológico

- **Backend:** Java 21 LTS + Spring Boot 3.3.x / 3.4.x (Spring Web, Spring Security, Spring Data JPA, Flyway, Apache POI, OpenAPI 3 / Swagger).
- **Frontend:** Next.js 15+ (App Router), React 19, TypeScript, Tailwind CSS, Lucide Icons.
- **Base de Datos:** PostgreSQL 16 (Supabase / local en Docker), con migraciones versionadas con Flyway.
- **Almacenamiento:** Supabase Storage (imágenes comerciales públicas y comprobantes privados) con adaptador testeable.
- **Canal de Ventas:** WhatsApp Business asistido (`wa.me` para MVP, preparado para Cloud API).
- **Despliegue:** API en Render / Frontend en Vercel.

---

## 📁 Estructura del Repositorio

```text
alexa-beauty/
├── .github/workflows/          # CI/CD para backend y frontend
├── backend/                    # API REST Spring Boot (Java 21)
│   ├── src/main/java/          # Código fuente modular por funcionalidad
│   ├── src/main/resources/     # Configuración y migraciones Flyway (db/migration/)
│   ├── src/test/               # Pruebas unitarias, de integración y Testcontainers
│   ├── Dockerfile              # Empaquetado de producción
│   └── pom.xml                 # Gestión de dependencias Maven
├── frontend/                   # Aplicación Web Next.js (TypeScript)
│   ├── src/app/                # Rutas públicas (tienda) y privadas (/admin)
│   ├── src/components/         # Componentes UI (mobile-first, WCAG AA)
│   ├── src/features/           # Lógica por dominio (catálogo, carrito, órdenes)
│   ├── public/branding/        # Logotipos e identidad visual
│   └── package.json            # Dependencias Node.js
├── branding/                   # Activos gráficos originales y propuesta refinada
├── documentation/              # Plan maestro, contratos y guías de operación
├── examples/xlsx/              # Plantillas de productos y reportes de muestra
├── TODO_IMPLEMENTACION.md      # Épicas, tareas atómicas y matriz de pruebas
├── DECISIONES.md               # Supuestos reversibles y registros de decisiones
├── .env.example                # Variables de entorno de referencia
└── docker-compose.yml          # Entorno local de base de datos
```

---

## 🚀 Requisitos Previos

1. **Java Development Kit (JDK):** Versión 21 LTS (se recomienda Microsoft OpenJDK o Eclipse Temurin).
2. **Apache Maven:** 3.9+ (o usar `./mvnw` incluido).
3. **Node.js:** v20.x o v22.x LTS (o superior) y `npm` / `pnpm`.
4. **Docker Desktop:** Para ejecutar PostgreSQL local o correr pruebas de Testcontainers.

---

## 🛠 Puesta en Marcha Local

### 1. Clonar el repositorio
```bash
git clone https://github.com/Sebasr0311/Alexa-Beauty.git
cd Alexa-Beauty
```

### 2. Configurar variables de entorno
Copiar `.env.example` a `.env` tanto en la raíz como en los subdirectorios correspondientes si se ejecutan de manera aislada:
```bash
cp .env.example .env
```

### 3. Iniciar la base de datos local
```bash
docker compose up -d postgres
```

### 4. Compilar y ejecutar el Backend
Asegurarse de que `JAVA_HOME` apunte al JDK 21:
```bash
cd backend
mvn clean spring-boot:run
```
La API estará disponible en `http://localhost:8080` y la documentación interactiva Swagger en `http://localhost:8080/swagger-ui.html`.

### 5. Compilar y ejecutar el Frontend
En otra terminal:
```bash
cd frontend
npm install
npm run dev
```
La aplicación web estará disponible en `http://localhost:3000`.

---

## 🧪 Pruebas y Verificación

- **Pruebas unitarias y de integración del Backend:**
  ```bash
  cd backend
  mvn test
  ```
- **Pruebas de Frontend (Linter & Typecheck):**
  ```bash
  cd frontend
  npm run lint
  npm run build
  ```
- **Pruebas E2E (Playwright):**
  ```bash
  cd frontend
  npx playwright test
  ```

---

## 📜 Licencia y Propiedad
Proyecto comercial desarrollado para **Alexa Beauty**. Todos los derechos reservados © 2026.
