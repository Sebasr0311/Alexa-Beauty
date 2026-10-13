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
        
        # -> Click the 'Productos' link in the left navigation to open the product catalog page.
        # Productos link
        elem = page.get_by_role("link", name="Productos", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the 'Nuevo Producto' button to open the new product form.
        # Nuevo Producto button
        elem = page.get_by_role("button", name="Nuevo Producto")
        await elem.click(timeout=10000)
        
        # -> Fill the form fields (Nombre del producto, SKU Base, Precio Venta (COP), Stock Inicial, Stock Mínimo Alerta) and click the 'Crear Producto' button to submit the new product.
        # Ej. Labial Matte Velvet Red text field
        elem = page.get_by_role("textbox", name="Ej. Labial Matte Velvet Red")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("Prueba QA Producto")
        
        # -> Fill the form fields (Nombre del producto, SKU Base, Precio Venta (COP), Stock Inicial, Stock Mínimo Alerta) and click the 'Crear Producto' button to submit the new product.
        # Ej. LAB-RED-01 text field
        elem = page.get_by_role("textbox", name="Ej. LAB-RED-")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("QA-SKU-20261013")
        
        # -> Fill the form fields (Nombre del producto, SKU Base, Precio Venta (COP), Stock Inicial, Stock Mínimo Alerta) and click the 'Crear Producto' button to submit the new product.
        # 25000 number field
        elem = page.get_by_placeholder("25000")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("30000")
        
        # -> Fill the form fields (Nombre del producto, SKU Base, Precio Venta (COP), Stock Inicial, Stock Mínimo Alerta) and click the 'Crear Producto' button to submit the new product.
        # number field
        elem = page.locator("div").filter(has_text=re.compile(r"^Stock Inicial$")).get_by_role("spinbutton")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("15")
        
        # -> Fill the form fields (Nombre del producto, SKU Base, Precio Venta (COP), Stock Inicial, Stock Mínimo Alerta) and click the 'Crear Producto' button to submit the new product.
        # number field
        elem = page.locator("div").filter(has_text=re.compile(r"^Stock Mínimo Alerta$")).get_by_role("spinbutton")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("5")
        
        # -> Click the 'Crear Producto' button to submit and save the new product.
        # Crear Producto button
        elem = page.get_by_role("button", name="Crear Producto")
        await elem.click(timeout=10000)
        
        # --> Assertions to verify final state
        
        # --> The new product appears in the catalog with SKU QA-SKU-20261013.
        # Assert-outcome: passed
        # Assert: The catalog row displays the created product's SKU.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[4]/div/table/tbody/tr[1]/td[2]").nth(0)).to_have_text("QA-SKU-20261013", timeout=15000), "The catalog row displays the created product's SKU."
        
        # --> Product creation succeeded and the catalog shows the entered sale price $ 30.000 for the new product.
        # Assert-outcome: passed
        # Assert: The product row shows the entered sale price formatted as '$ 30.000'.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[4]/div/table/tbody/tr[1]/td[4]").nth(0)).to_have_text("$ 30.000", timeout=15000), "The product row shows the entered sale price formatted as '$ 30.000'."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    