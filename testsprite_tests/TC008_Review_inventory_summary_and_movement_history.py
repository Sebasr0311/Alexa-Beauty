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
        
        # -> Click the 'Inventario' link in the left navigation to open the inventory page.
        # Inventario link
        elem = page.get_by_role("link", name="Inventario", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the 'Libro Kardex de Movimientos' tab to open the movement history view.
        # Libro Kardex de Movimientos ( 3 ) button
        elem = page.get_by_role("button", name="Libro Kardex de Movimientos (")
        await elem.click(timeout=10000)
        
        # --> Assertions to verify final state
        
        # --> Stock summary cards are shown on the inventory page (Stock Físico: 101 unidades; Stock Reservado: 11 unidades; Stock Disponible Real: 90 unidades; Alertas de Stock Bajo: 2 variantes).
        await page.get_by_role("paragraph").filter(has_text="101 unidades").locator("span").nth(0).scroll_into_view_if_needed()
        # Assert-outcome: passed
        # Assert: A stock summary card unit label ('unidades') is visible on the page.
        await expect(page.get_by_role("paragraph").filter(has_text="101 unidades").locator("span").nth(0)).to_be_visible(timeout=15000), "A stock summary card unit label ('unidades') is visible on the page."
        
        # --> Movement history (Libro Kardex de Movimientos) table with records is displayed (example row: 13 de oct, 01:07 a. m. — VENTA — SKU LAB-MAT-NUD01).
        # Assert-outcome: passed
        # Assert: The movement history table header ('Fecha') is present, indicating the table of records is visible.
        await expect(page.locator("thead").nth(0)).to_contain_text("Fecha", timeout=15000), "The movement history table header ('Fecha') is present, indicating the table of records is visible."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    