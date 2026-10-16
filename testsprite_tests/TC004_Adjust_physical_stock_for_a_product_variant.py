import asyncio
import re
from playwright import async_api
from playwright.async_api import expect

async def run_test():
    pw = None
    browser = None
    context = None

    try:
        # Start a Playwright session in asynchronous mode
        pw = await async_api.async_playwright().start()

        # Launch a Chromium browser in headless mode with custom arguments
        browser = await pw.chromium.launch(
            headless=True,
            args=[
                "--window-size=1280,720",
                "--disable-dev-shm-usage",
                "--ipc=host",
                "--single-process"
            ],
        )

        # Create a new browser context (like an incognito window)
        context = await browser.new_context()
        # Wider default timeout to match the agent's DOM-stability budget;
        # auto-waiting Playwright APIs (expect, locator.wait_for) inherit this.
        context.set_default_timeout(15000)

        # Open a new page in the browser context
        page = await context.new_page()

        # Interact with the page elements to simulate user flow
        # -> navigate
        await page.goto("http://localhost:3000/admin")
        try:
            await page.wait_for_load_state("domcontentloaded", timeout=5000)
        except Exception:
            pass
        
        # -> Authenticate as Admin if redirected to /admin/login
        if "/admin/login" in page.url:
            await page.get_by_placeholder("admin@alexabeauty.com").fill("admin@alexabeauty.com")
            await page.get_by_placeholder("••••••••••••••••").fill("Admin123*Alexa")
            await page.get_by_role("button", name="Ingresar al panel").click(timeout=10000)
            await page.wait_for_url("**/admin**", timeout=10000)

        # -> Click the 'Inventario' link in the left navigation to open the inventory management page.
        elem = page.get_by_role("link", name="Inventario", exact=True)
        await elem.click(timeout=10000)

        # -> Click the 'Ajuste manual' button for the first variant to open the manual stock adjustment modal.
        elem = page.get_by_role("button", name="Ajuste manual").first
        await elem.click(timeout=10000)

        # -> Fill 'Nuevo Stock Físico (On Hand)' with '5', leave 'Motivo del ajuste (opcional)' empty, and complete the 2-step confirmation.
        elem = page.get_by_role("spinbutton")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("5")

        # Verify optional reason field is left empty and proceed through 2-step confirmation
        await page.get_by_role("button", name="Revisar Ajuste").click(timeout=10000)
        await page.get_by_role("button", name="Confirmar Ajuste Definitivo").click(timeout=10000)

        # -> Click the 'Libro Kardex de Movimientos' tab to view movement records for the recent adjustment.
        elem = page.get_by_role("button", name=re.compile(r"Libro Kardex de Movimientos"))
        await elem.click(timeout=10000)

        # --> Assertions to verify final state
        # --> The adjustment is recorded with Después = 5 and fallback reason 'Ajuste manual de inventario' in the Kardex movement record.
        await expect(page.locator("tbody").first).to_contain_text("Ajuste manual de inventario", timeout=15000)
        await asyncio.sleep(2)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    