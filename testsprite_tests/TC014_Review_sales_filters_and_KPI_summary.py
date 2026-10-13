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
        
        # -> Click the 'Ventas' link in the left navigation to open the sales (Ventas) page.
        # Ventas link
        elem = page.get_by_role("link", name="Ventas", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the '7 días' date preset button to set the date range to the last 7 days.
        # 7 días button
        elem = page.get_by_role("button", name="7 días")
        await elem.click(timeout=10000)
        
        # -> Open the 'Método' dropdown (label reads 'Método: Todos los métodos') to reveal payment method options.
        # Todos los métodos Nequi Daviplata Bancolombia... dropdown
        elem = page.get_by_label("Método:")
        await elem.click(timeout=10000)
        
        # -> Select 'Nequi' from the 'Método' dropdown and click the 'Actualizar datos' button to apply the payment-method filter.
        # Todos los métodos Nequi Daviplata Bancolombia... dropdown
        elem = page.locator("xpath=/html/body/div[2]/div/div/main/div[2]/section[2]/div/div[2]/div/select").nth(0)
        await elem.wait_for(state="visible", timeout=10000)
        await elem.select_option("")
        
        # -> Select 'Nequi' from the 'Método' dropdown and click the 'Actualizar datos' button to apply the payment-method filter.
        # Actualizar datos button
        elem = page.get_by_role("button", name="Actualizar datos")
        await elem.click(timeout=10000)
        
        # -> Fill the search field labeled 'Buscar por referencia (ej. AB-2026-000101), cliente o teléfono...' with the reference 'AB-2026-000101' and press Enter to filter results.
        # Buscar por referencia (ej. AB-2026-000101)... text field
        elem = page.get_by_role("textbox", name="Buscar por referencia (ej. AB")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("AB-2026-000101")
        
        # --> Assertions to verify final state
        
        # --> Transactions table shows the searched reference 'AB-2026-000101' with payment method 'Nequi'.
        # Assert-outcome: passed
        # Assert: Reference column equals the searched reference 'AB-2026-000101'.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[2]/section[3]/div[2]/table/tbody/tr/td[2]").nth(0)).to_have_text("AB-2026-000101", timeout=15000), "Reference column equals the searched reference 'AB-2026-000101'."
        # Assert-outcome: passed
        # Assert: Method column shows the selected payment method 'Nequi'.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[2]/section[3]/div[2]/table/tbody/tr/td[4]").nth(0)).to_have_text("Nequi", timeout=15000), "Method column shows the selected payment method 'Nequi'."
        
        # --> Sales KPI cards are visible on the Ventas page and include the 'Total Ventas Pagadas' label.
        # Assert-outcome: passed
        # Assert: KPI area is visible and contains the 'Total Ventas Pagadas' label.
        await expect(page.get_by_label("Métricas de ventas").nth(0)).to_contain_text("Total Ventas Pagadas", timeout=15000), "KPI area is visible and contains the 'Total Ventas Pagadas' label."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    