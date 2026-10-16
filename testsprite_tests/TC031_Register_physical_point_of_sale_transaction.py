import asyncio
from playwright import async_api
from playwright.async_api import expect

async def run_test():
    pw = None
    browser = None
    context = None

    try:
        pw = await async_api.async_playwright().start()
        browser = await pw.chromium.launch(
            headless=True,
            args=[
                "--window-size=1440,900",
                "--disable-dev-shm-usage",
                "--ipc=host"
            ],
        )
        context = await browser.new_context()
        context.set_default_timeout(15000)
        page = await context.new_page()

        # 1. Authenticate as Admin
        await page.goto("http://localhost:3000/admin", wait_until="networkidle")
        email_input = page.locator('input[type="email"]')
        if await email_input.is_visible():
            await email_input.fill("admin@alexabeauty.com")
            await page.locator('input[type="password"]').fill("Admin123*Alexa")
            await page.get_by_role("button", name="Ingresar al panel").click()
            await page.wait_for_timeout(1500)

        # 2. Navigate to /admin/ventas and open physical sale modal
        await page.goto("http://localhost:3000/admin/ventas", wait_until="networkidle")
        open_pos_btn = page.get_by_role("button", name="Registrar Venta Física")
        await expect(open_pos_btn).to_be_visible()
        await open_pos_btn.click()

        dialog = page.locator('[role="dialog"][aria-labelledby="pos-modal-title"]')
        await expect(dialog).to_be_visible()

        # 3. Select available variant, set quantity=1, choose Efectivo, enter customer name and confirm
        variant_select = dialog.locator("#pos-product-0")
        await expect(variant_select).to_be_visible()
        await dialog.locator("#pos-qty-0").fill("1")
        await dialog.get_by_role("button", name="Efectivo", exact=True).click()
        await dialog.locator("#pos-customer-name").fill("Cliente TestSprite POS")
        await dialog.locator("#pos-transfer-ref").fill("EFECTIVO-TS-031")
        await dialog.get_by_role("button", name="Confirmar Venta Física").click()

        # 4. Assert dialog closes and row with Punto Físico badge appears in table
        await expect(dialog).to_be_hidden()
        first_row = page.locator("table tbody tr").first
        await expect(first_row).to_contain_text("Cliente TestSprite POS")
        await expect(first_row).to_contain_text("Punto Físico")
        await expect(first_row).to_contain_text("Efectivo")

        # 5. Navigate to /admin/inventario and verify Kardex movement VENTA
        await page.goto("http://localhost:3000/admin/inventario", wait_until="networkidle")
        await page.get_by_role("button", name="Libro Kardex de Movimientos").click()
        kardex_body = page.locator("table tbody")
        await expect(kardex_body).to_contain_text("VENTA")
        await expect(kardex_body).to_contain_text("EFECTIVO-TS-031")

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

if __name__ == "__main__":
    asyncio.run(run_test())
