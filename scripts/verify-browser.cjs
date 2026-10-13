// Local-only browser rehearsal. Never runs against a public store or sends WhatsApp messages.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const modulePath = process.env.PLAYWRIGHT_MODULE_PATH || path.join(os.homedir(), '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const { chromium } = require(modulePath);
const base = process.env.E2E_BASE_URL || 'http://localhost:3009';
assert(['localhost', '127.0.0.1'].includes(new URL(base).hostname), 'Local verification only');
assert(process.env.E2E_PASSWORD, 'Provide a synthetic verification-account password');
const artifacts = process.env.E2E_ARTIFACT_DIR || path.join(os.homedir(), '.codex/verification/alexa-beauty-20261013/browser');
fs.mkdirSync(artifacts, { recursive: true });
(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const evidence = [];
  try {
    for (const [name, viewport] of [['desktop', { width: 1440, height: 1000 }], ['mobile', { width: 390, height: 844 }]]) {
      const context = await browser.newContext({ viewport, acceptDownloads: true });
      const page = await context.newPage();
      page.setDefaultTimeout(30000);
      page.setDefaultNavigationTimeout(60000);
      await page.goto(`${base}/carrito`);
      await page.getByText(/Aún no tienes productos en tu carrito/i).first().waitFor();
      await page.reload();
      await page.getByText(/Aún no tienes productos en tu carrito/i).first().waitFor();
      await page.goto(`${base}/admin`);
      await page.getByLabel('Correo', { exact: false }).fill(process.env.E2E_EMAIL || 'admin@alexabeauty.com');
      await page.getByLabel('Contraseña', { exact: false }).fill(process.env.E2E_PASSWORD);
      await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
      await page.getByRole('button', { name: 'Cerrar sesión' }).waitFor();
      const cookie = (await context.cookies()).find(c => c.name === 'token');
      assert(cookie && cookie.httpOnly && cookie.sameSite === 'Lax');
      assert.equal(await page.evaluate(() => localStorage.getItem('alexa_auth_token')), null);
      const headers = { Origin: base, 'X-Requested-With': 'XMLHttpRequest' };
      const api = async (endpoint, body) => {
        const response = body ? await context.request.post(`${base}/api/backend${endpoint}`, { data: body, headers })
          : await context.request.get(`${base}/api/backend${endpoint}`);
        assert(response.ok(), `API ${endpoint}: ${response.status()}`);
        return (await response.json()).data;
      };
      const categories = await api('/catalog/categories');
      const brands = await api('/catalog/brands');
      const suffix = `${name}-${Date.now()}`;
      const product = await api('/admin/products', { name: `Verification ${suffix}`, sku: `E2E-${suffix}`,
        brandId: brands[0].id, categoryId: categories[0].id, salePrice: 12000, stockOnHand: 5, minStock: 0,
        active: true, variants: [{ sku: `VAR-${suffix}`, shadeName: 'Verification', salePrice: 12000, stockOnHand: 5, minStock: 0, active: true }] });
      await page.goto(`${base}/producto/${product.slug}`);
      await page.getByRole('button', { name: /Agregar al carrito/ }).click();
      await page.goto(`${base}/carrito`);
      await page.getByText(product.name, { exact: false }).first().waitFor();
      await page.reload();
      await page.getByText(product.name, { exact: false }).first().waitFor();
      await page.goto(`${base}/pedido`);
      await page.getByLabel('Nombre y apellido', { exact: false }).fill('Verification Customer');
      await page.getByLabel('Número de WhatsApp', { exact: false }).fill('3001234567');
      await page.getByRole('button', { name: /Retiro|Recoger/i }).click();
      const created = page.waitForResponse(r => r.url().endsWith('/api/backend/orders') && r.request().method() === 'POST');
      await page.getByRole('button', { name: /Confirmar pedido y reservar/ }).click();
      const orderResponse = await created;
      assert.equal(orderResponse.status(), 201);
      const order = (await orderResponse.json()).data;
      assert.equal(order.items[0].variantId, product.variants[0].id);
      await page.waitForURL('**/pedido/confirmado/**');
      await page.getByText(`Referencia:`, { exact: false }).first().waitFor();
      await page.goto(`${base}/carrito`); await page.reload();
      await page.getByText(/Aún no tienes productos en tu carrito/i).first().waitFor();
      await page.goto(`${base}/admin/pedidos`);
      await page.getByPlaceholder('Buscar por referencia, cliente o tel...').fill(order.reference);
      await page.getByTitle('Verificar Pago', { exact: true }).first().click();
      await page.locator('select').last().selectOption(name === 'desktop' ? 'EFECTIVO' : 'NEQUI');
      await page.getByPlaceholder('Ej: M12345678 o 987654321').fill(`MANUAL-${suffix}`);
      const confirmed = page.waitForResponse(r => r.url().endsWith('/confirm-payment') && r.request().method() === 'POST');
      await page.getByRole('button', { name: 'Confirmar Pago y Descontar Stock', exact: true }).click();
      assert.equal((await confirmed).status(), 200);
      const paid = await api(`/admin/orders/${order.orderId}`); assert.equal(paid.status, 'PAGADO');
      const actual = await api(`/catalog/products/${product.slug}`);
      assert.equal(actual.variants[0].stockOnHand, 4); assert.equal(actual.variants[0].stockReserved, 0);
      await page.goto(`${base}/admin/ventas`);
      const downloading = page.waitForEvent('download');
      await page.getByRole('button', { name: /Descargar Reporte Excel/ }).click();
      const download = await downloading; const filename = path.join(artifacts, `${name}-sales.xlsx`);
      await download.saveAs(filename); assert(fs.statSync(filename).size > 1000);
      await page.screenshot({ path: path.join(artifacts, `${name}-sales.png`), fullPage: true });
      await page.getByRole('button', { name: 'Cerrar sesión' }).click();
      await page.getByRole('button', { name: 'Ingresar', exact: true }).waitFor();
      assert(!(await context.cookies()).some(c => c.name === 'token'));
      await page.route('**/api/backend/catalog/products?**', route => route.fulfill({ status: 503, contentType: 'application/json', body: '{"detail":"Verification outage"}' }));
      await page.goto(`${base}/catalogo`);
      await page.getByText(/No pudimos cargar el catálogo/).waitFor();
      assert.equal(await page.locator('article').count(), 0);
      await page.screenshot({ path: path.join(artifacts, `${name}-catalog-error.png`), fullPage: true });
      evidence.push({ viewport: name, result: 'PASS', cookieHttpOnly: true, sameSite: cookie.sameSite,
        cookieSecure: cookie.secure, checkoutUuid: true, paid: true, onHand: 4, reserved: 0, xlsxDownloaded: true,
        emptyCartReload: true, apiOutageNotMocked: true, logout: true, transport: 'local HTTP (not HTTPS production)' });
      await context.close();
    }
    fs.writeFileSync(path.join(artifacts, 'result.json'), JSON.stringify(evidence, null, 2));
    console.log(JSON.stringify(evidence));
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
