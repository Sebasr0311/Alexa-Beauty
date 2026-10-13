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
        
        # -> Click the 'Inventario' link in the left navigation to open the inventory management page.
        # Inventario link
        elem = page.get_by_role("link", name="Inventario", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the 'Ajustar' button for the first variant (LAB-MAT-NUD01) to open the manual stock adjustment modal.
        # Ajustar button
        elem = page.get_by_role("row", name="LAB-MAT-NUD01 Labial Mate").get_by_role("button")
        await elem.click(timeout=10000)
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '52', enter a reason into 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button to save the adjustment.
        # number field
        elem = page.get_by_role("spinbutton")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("52")
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '52', enter a reason into 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button to save the adjustment.
        # Ej. Conteo físico de inventario, merma por daño... text area
        elem = page.get_by_role("textbox", name="Ej. Conteo físico de")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("Conteo f\u00edsico de inventario: ajuste +2")
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '52', enter a reason into 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button to save the adjustment.
        # Confirmar Ajuste button
        elem = page.get_by_role("button", name="Confirmar Ajuste")
        await elem.click(timeout=10000)
        
        # -> Click the 'Libro Kardex de Movimientos' tab to view movement records for the recent adjustment.
        # Libro Kardex de Movimientos ( 4 ) button
        elem = page.get_by_role("button", name="Libro Kardex de Movimientos (")
        await elem.click(timeout=10000)
        
        # -> Click the 'Existencias por Producto' tab to view the product inventory list and verify the SKU's Físico and Disponible values.
        # Existencias por Producto ( 4 ) button
        elem = page.get_by_role("button", name="Existencias por Producto (4)")
        await elem.click(timeout=10000)
        
        # -> Open the 'Libro Kardex de Movimientos' tab to view movement records and verify an AJUSTE entry for LAB-MAT-NUD01 with the +2 delta, Antes→Después values, and the adjustment reason.
        # Libro Kardex de Movimientos ( 4 ) button
        elem = page.get_by_role("button", name="Libro Kardex de Movimientos (")
        await elem.click(timeout=10000)
        
        # --> Assertions to verify final state
        
        # --> The adjustment is recorded with Después = 52 in the Kardex movement record for LAB-MAT-NUD01.
        # Assert-outcome: passed
        # Assert: Verifies the movement record's 'Después' value equals 52.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[5]/div[2]/table/tbody/tr[1]/td[5]/strong").nth(0)).to_have_text("52", timeout=15000), "Verifies the movement record's 'Despu\u00e9s' value equals 52."
        
        # --> A Kardex movement record for LAB-MAT-NUD01 shows the manual adjustment with delta +2 and the provided reason.
        # Assert-outcome: passed
        # Assert: Verifies the movement record contains the adjustment reason provided during the test.
        await expect(page.locator("tbody").nth(0)).to_contain_text("Conteo f\u00edsico de inventario: ajuste +2", timeout=15000), "Verifies the movement record contains the adjustment reason provided during the test."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    