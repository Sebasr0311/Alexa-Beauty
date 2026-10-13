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
        
        # -> Click the 'Solo Stock Bajo' button to filter the product list to only low-stock items.
        # Solo Stock Bajo button
        elem = page.get_by_role("button", name="Solo Stock Bajo")
        await elem.click(timeout=10000)
        
        # -> Click the 'Ajustar' button for the low-stock product 'BASE-03' to open the manual adjustment form.
        # Ajustar button
        elem = page.get_by_role("row", name="BASE-03 Base Líquida").get_by_role("button")
        await elem.click(timeout=10000)
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '18', enter a reason in 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button.
        # number field
        elem = page.get_by_role("spinbutton")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("18")
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '18', enter a reason in 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button.
        # Ej. Conteo físico de inventario, merma por daño... text area
        elem = page.get_by_role("textbox", name="Ej. Conteo físico de")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("Recepci\u00f3n de mercanc\u00eda: ingreso de 3 unidades")
        
        # -> Fill 'Nuevo Stock Físico (On Hand)' with '18', enter a reason in 'Motivo obligatorio del ajuste', and click the 'Confirmar Ajuste' button.
        # Confirmar Ajuste button
        elem = page.get_by_role("button", name="Confirmar Ajuste")
        await elem.click(timeout=10000)
        
        # -> Open the 'Libro Kardex de Movimientos' tab and verify the movement list contains an entry for BASE-03 with the adjustment reason or a +3 units 'Ajuste' movement.
        # Libro Kardex de Movimientos ( 4 ) button
        elem = page.get_by_role("button", name="Libro Kardex de Movimientos (")
        await elem.click(timeout=10000)
        
        # --> Assertions to verify final state
        
        # --> The movement history (Libro Kardex de Movimientos) contains an AJUSTE row for SKU BASE-03 showing a +3 delta and the 15 ➔ 18 change.
        # Assert-outcome: passed
        # Assert: The first kardex row is for SKU BASE-03.
        await expect(page.locator("tbody").nth(0)).to_contain_text("BASE-03", timeout=15000), "The first kardex row is for SKU BASE-03."
        # Assert-outcome: passed
        # Assert: The kardex row shows a +3 delta for the adjustment.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[5]/div[2]/table/tbody/tr[1]/td[4]/span").nth(0)).to_have_text("+3", timeout=15000), "The kardex row shows a +3 delta for the adjustment."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    