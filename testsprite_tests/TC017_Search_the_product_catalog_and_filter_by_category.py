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

        # -> Click the 'Productos' link in the left navigation to open the product catalog page.
        # Productos link
        elem = page.get_by_role("link", name="Productos", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the 'Labios' category button to filter the catalog to the Labios category.
        # Labios button
        elem = page.get_by_role("button", name="Labios")
        await elem.click(timeout=10000)
        
        # -> Click the 'Labios' category button to filter the catalog to the Labios category.
        # Buscar por nombre o SKU... text field
        elem = page.get_by_role("textbox", name="Buscar por nombre o SKU...")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("LAB-MAT-NUD01")
        
        # --> Assertions to verify final state
        
        # --> Searching for 'LAB-MAT-NUD01' shows the matching product SKU in the catalog.
        # Assert-outcome: passed
        # Assert: The search input contains the searched SKU.
        await expect(page.get_by_role("textbox", name="Buscar por nombre o SKU...").nth(0)).to_have_value("LAB-MAT-NUD01", timeout=15000), "The search input contains the searched SKU."
        # Assert-outcome: passed
        # Assert: The catalog displays a row with the SKU LAB-MAT-NUD01.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[3]/div/table/tbody/tr/td[2]").nth(0)).to_have_text("LAB-MAT-NUD01", timeout=15000), "The catalog displays a row with the SKU LAB-MAT-NUD01."
        
        # --> The displayed product row is in the selected category 'Labios'.
        # Assert-outcome: passed
        # Assert: The product row's category is Labios.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[3]/div/table/tbody/tr/td[3]").nth(0)).to_have_text("Labios", timeout=15000), "The product row's category is Labios."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    