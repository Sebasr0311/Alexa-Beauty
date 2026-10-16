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

        # Mock admin auth and sales
        await page.route('**/api/backend/auth/me', lambda route: route.fulfill(
            status=200,
            content_type='application/json',
            body='{"data":{"id":"00000000-0000-0000-0000-000000000099","email":"admin@alexabeauty.com","fullName":"Admin","role":"ADMIN"}}'
        ))
        await page.route('**/api/backend/admin/sales*', lambda route: route.fulfill(
            status=200,
            content_type='application/json',
            body='{"data":{"summary":{"totalSales":0,"orderCount":0,"averageTicket":0,"totalItemsSold":0,"physicalSalesCount":0,"physicalSalesTotal":0,"onlineSalesCount":0,"onlineSalesTotal":0,"totalProfit":0,"totalCost":0,"profitMarginPercentage":0},"orders":[],"page":0,"size":10,"totalElements":0,"totalPages":0}}'
        ))

        # Generate 200 mock variants to test searching across large product catalogue
        variants_json = []
        for i in range(1, 201):
            brand = "Samy" if i % 2 == 0 else "Trendy"
            variants_json.append(
                f'{{"variantId":"var-{i}","productId":"prod-{i}","sku":"ABV-SKU-{1000+i}",'
                f'"productName":"Corrector {brand} #{i}","shadeName":"Tono {i}",'
                f'"shadeHex":"#E1AF78","presentation":"15ml","brandName":"{brand}",'
                f'"salePrice":18000,"referenceCost":10000,"availableStock":10,'
                f'"stockOnHand":10,"stockReserved":0,"active":true,'
                f'"imageUrl":"https://rshrwewedceqqcagetwl.supabase.co/storage/v1/object/public/alexa-beauty-public/products/sample.jpg"}}'
            )
        inventory_body = '{"data":{"content":[' + ','.join(variants_json) + '],"page":0,"size":1000,"totalElements":200,"totalPages":1}}'

        await page.route('**/api/backend/admin/inventory/summary*', lambda route: route.fulfill(
            status=200,
            content_type='application/json',
            body=inventory_body
        ))

        # 1. Navigate to /admin/ventas and open physical sale modal
        await page.goto("http://localhost:3000/admin/ventas", wait_until="networkidle")
        open_pos_btn = page.get_by_role("button", name="Registrar Venta Física")
        await expect(open_pos_btn).to_be_visible()
        await open_pos_btn.click()

        dialog = page.locator('[role="dialog"][aria-labelledby="pos-modal-title"]')
        await expect(dialog).to_be_visible()

        # 2. Verify quick search bar is visible and displays available stock count
        quick_search_input = dialog.locator("#pos-quick-search-input")
        await expect(quick_search_input).to_be_visible()
        await expect(dialog.get_by_text("200 disponibles")).to_be_visible()

        # 3. Type search query in quick search and verify results dropdown appears
        await quick_search_input.fill("Samy")
        await page.wait_for_timeout(300)
        samy_results = dialog.locator('button:has-text("Corrector Samy")')
        expect(await samy_results.count()).toBeGreaterThan(0)

        # 4. Click result to quick add and verify input is cleared and line is added
        first_samy = samy_results.first
        await first_samy.click()
        await page.wait_for_timeout(300)
        expect(await quick_search_input.input_value()).toBe("")

        # 5. Open line picker search, verify autofocus, type and verify clear button
        await dialog.locator("#pos-product-0").click()
        picker_search = dialog.locator("#pos-picker-search-0")
        await expect(picker_search).to_be_visible()
        await picker_search.fill("Trendy")
        await page.wait_for_timeout(200)

        clear_btn = dialog.get_by_role("button", name="Limpiar filtro")
        await expect(clear_btn).to_be_visible()
        await clear_btn.click()
        expect(await picker_search.input_value()).toBe("")

    finally:
        if context:
            await context.close()
        if browser:
            await browser.close()
        if pw:
            await pw.stop()

if __name__ == "__main__":
    asyncio.run(run_test())
