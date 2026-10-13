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
        
        # -> Click the 'Pedidos' link in the left navigation to open the orders page.
        # Pedidos link
        elem = page.get_by_role("link", name="Pedidos", exact=True)
        await elem.click(timeout=10000)
        
        # -> Click the 'Pagados' status filter button to show only paid orders.
        # Pagados button
        elem = page.get_by_role("button", name="Pagados")
        await elem.click(timeout=10000)
        
        # -> Enter 'AB-2026-000102' in the 'Buscar por referencia, cliente o tel...' search field and submit the search.
        # Buscar por referencia, cliente o tel... text field
        elem = page.get_by_role("textbox", name="Buscar por referencia,")
        await elem.wait_for(state="visible", timeout=10000)
        await elem.fill("AB-2026-000102")
        
        # --> Assertions to verify final state
        
        # --> Searching for 'AB-2026-000102' with the Pagados filter applied returns the matching order AB-2026-000102 in the orders table.
        # Assert-outcome: passed
        # Assert: The search field contains the entered reference 'AB-2026-000102'.
        await expect(page.get_by_role("textbox", name="Buscar por referencia,").nth(0)).to_have_value("AB-2026-000102", timeout=15000), "The search field contains the entered reference 'AB-2026-000102'."
        # Assert-outcome: passed
        # Assert: The orders table displays the matching order reference 'AB-2026-000102'.
        await expect(page.locator("xpath=/html/body/div[2]/div/div/main/div[3]/div/table/tbody/tr/td[1]").nth(0)).to_have_text("AB-2026-000102", timeout=15000), "The orders table displays the matching order reference 'AB-2026-000102'."
        await asyncio.sleep(5)

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

asyncio.run(run_test())
    