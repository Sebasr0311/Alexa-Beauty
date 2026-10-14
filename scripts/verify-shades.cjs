// Local-only functional rehearsal against an isolated PostgreSQL-backed API.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE_PATH || path.join(os.homedir(),'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
const web=process.env.SHADE_WEB_URL||'http://127.0.0.1:3017';
const api=process.env.SHADE_API_URL||'http://127.0.0.1:18085/api/v1';
assert.equal(new URL(web).hostname,'127.0.0.1');assert.equal(new URL(api).hostname,'127.0.0.1');
assert.equal(new URL(api).port,'18085','Only the dedicated verification API is allowed');
const output=path.resolve(process.env.SHADE_EVIDENCE_DIR || 'documentation/evidence/visual-shades/20261014');fs.mkdirSync(output,{recursive:true});
// This is the public synthetic test-profile key, never a real account secret.
const config=fs.readFileSync('backend/src/main/resources/application-test.yml','utf8');
const secret=config.match(/secret: "([^"]+)"/)[1];
const encode=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
const jwtHeader=encode({alg:'HS256',typ:'JWT'});
const jwtBody=encode({sub:'admin@alexabeauty.com',userId:'00000000-0000-0000-0000-000000000099',role:'ADMIN',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600});
const token=`${jwtHeader}.${jwtBody}.${crypto.createHmac('sha256',secret).update(`${jwtHeader}.${jwtBody}`).digest('base64url')}`;
async function request(endpoint,body,method=body?'POST':'GET') {
  const response=await fetch(api+endpoint,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
  const result=await response.json();assert(response.ok,`${endpoint}: ${response.status} ${JSON.stringify(result)}`);return result.data;
}
const evidence=[];
let diagnosticPage;
async function capture(page,name){await page.evaluate(()=>window.scrollTo(0,0));await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));await page.screenshot({path:path.join(output,name),fullPage:true});}
async function record(name,fn){try{await fn();evidence.push({name,result:'PASS'});console.log(`PASS ${name}`);}catch(error){evidence.push({name,result:'FAIL',error:error.message});if(diagnosticPage){await diagnosticPage.screenshot({path:path.join(output,'failure.png'),fullPage:true});console.error((await diagnosticPage.locator('body').innerText()).slice(-5000));}throw error;}}
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const stamp=Date.now();
  const shade=(code,name,stock,hex,price=25000)=>({shadeCode:code,shadeName:name,stockOnHand:stock,shadeHex:hex,salePrice:price,minStock:1});
  const product=await request('/admin/products',{name:`Shade acceptance ${stamp}`,brandId:'20000000-0000-0000-0000-000000000001',categoryId:'10000000-0000-0000-0000-000000000001',salePrice:25000,stockOnHand:0,
   variants:[shade('107','Natural Ivory',6,'#EDD4BF'),shade('110','Classic Ivory',4,'#D8B396',30000),shade('115','Warm Beige',0,'#BB9576'),{...shade('120','Inactive hidden',5,'#805139'),active:false}]});
  const [a,b]=product.variants;
  const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jh1sAAAAASUVORK5CYII=','base64');
  async function image(variantId){const form=new FormData();form.append('file',new Blob([png],{type:'image/png'}),'shade.png');if(variantId)form.append('variantId',variantId);else form.append('isCover','true');const response=await fetch(`${api}/admin/products/${product.id}/images`,{method:'POST',headers:{Authorization:`Bearer ${token}`},body:form});const result=await response.json();assert(response.ok,JSON.stringify(result));return result.data;}
  const cover=await image();const tonePhoto=await image(a.id);
  const context=await browser.newContext({viewport:{width:390,height:844}});
  await context.addCookies([{name:'token',value:token,url:web,httpOnly:true,sameSite:'Lax'}]);
  const page=await context.newPage();diagnosticPage=page;page.on('pageerror',error=>console.error('Browser error',error.message));page.on('response',async response=>{if(response.url().includes('/api/backend/')&&response.status()>=400)console.error('API failure',response.status(),response.url(),await response.text());});page.setDefaultTimeout(20000);page.setDefaultNavigationTimeout(90000);
  await record('real admin adds and edits shade without changing existing stock',async()=>{
   await page.goto(`${web}/admin/productos`);await page.getByRole('button',{name:`Gestionar tonos de ${product.name}`,exact:true}).click();
   const dialog=page.getByRole('dialog');const form=dialog.locator('form').last();
   await form.getByLabel('Código de tono',{exact:true}).fill('117');await form.getByLabel('Nombre del tono',{exact:true}).fill('Soft Sand');
   await form.getByLabel('Color HEX',{exact:true}).fill('#C5A184');await form.getByLabel('Precio de venta COP').fill('28000');await form.getByLabel('Stock inicial').fill('3');
   await form.getByRole('button',{name:'Añadir variante',exact:true}).click();await dialog.getByText('Nuevo tono añadido.',{exact:true}).waitFor();
   const row=dialog.getByRole('region',{name:'Variante 117 · Soft Sand'}); // section has an accessible name
   await row.getByLabel('Color HEX',{exact:true}).fill('#C6A285');await row.getByRole('button',{name:'Guardar variante',exact:true}).click();await row.getByText('Variante guardada.',{exact:true}).waitFor();
   page.once('dialog',dialog=>dialog.accept());await row.getByRole('button',{name:'Desactivar',exact:true}).click();await row.getByRole('button',{name:'Activar',exact:true}).waitFor();
   page.once('dialog',dialog=>dialog.accept());await row.getByRole('button',{name:'Activar',exact:true}).click();await row.getByRole('button',{name:'Desactivar',exact:true}).waitFor();
   await row.locator('input[type="file"]').setInputFiles({name:'admin-shade.png',mimeType:'image/png',buffer:png});
   await row.getByText('Foto vinculada al tono.',{exact:true}).waitFor();
   await capture(page,'admin-variant-mobile.png');
   await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});
   const latest=await request(`/admin/products/${product.id}`);assert.equal(latest.variants.find(v=>v.id===a.id).stockOnHand,6);assert.equal(latest.variants.find(v=>v.shadeCode==='117').shadeHex,'#C6A285');
  });
  await record('real admin creates one product with multiple independently priced variants',async()=>{
   await page.getByRole('button',{name:'Nuevo Producto',exact:true}).click();const dialog=page.getByRole('dialog',{name:'Nuevo Producto'});
   await dialog.getByLabel('Nombre del producto',{exact:true}).fill(`Admin multi shade ${stamp}`);
   for(let i=0;i<2;i++){await dialog.getByRole('button',{name:'Añadir tono al nuevo producto',exact:true}).click();const fields=dialog.getByRole('group',{name:`Variante ${i+1}`,exact:true});
    await fields.getByLabel('Código de tono',{exact:true}).fill(String(501+i));await fields.getByLabel('Nombre del tono',{exact:true}).fill(`Admin shade ${i+1}`);
    await fields.getByLabel('Color HEX',{exact:true}).fill(i?'#C6A285':'#EDD4BF');await fields.getByLabel('Precio de venta COP').fill(String(15000+i*1000));await fields.getByLabel('Stock inicial',{exact:true}).fill(String(2+i));}
   const created=page.waitForResponse(response=>response.url().endsWith('/api/backend/admin/products')&&response.request().method()==='POST');
   await dialog.getByRole('button',{name:'Crear Producto',exact:true}).click();const response=await created;assert.equal(response.status(),201);const result=(await response.json()).data;
   assert.equal(result.variants.length,2);assert.equal(new Set(result.variants.map(v=>v.sku)).size,2);assert.deepEqual(result.variants.map(v=>v.salePrice).sort(),[15000,16000]);await dialog.waitFor({state:'hidden'});
   await page.goto(`${web}/producto/${result.slug}`);await page.getByRole('button',{name:'502 · Admin shade 2',exact:true}).click();await page.getByText('3 unidades disponibles',{exact:true}).waitFor();
  });
  await record('selected shade updates real price SKU quantity and photo fallback; sold out never purchases',async()=>{
   await page.goto(`${web}/producto/${product.slug}`);await page.getByRole('button',{name:'107 · Natural Ivory',exact:true}).waitFor();
   assert.equal(await page.getByTestId('shade-swatch').count(),4);await page.getByRole('button',{name:'117 · Soft Sand',exact:true}).waitFor();
   assert.equal(await page.getByRole('button',{name:'107 · Natural Ivory',exact:true}).getAttribute('aria-pressed'),'true');
   assert.equal(await page.getByRole('button',{name:/Inactive hidden/}).count(),0);
   assert((await page.locator('img').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('src')))).includes(tonePhoto.url));
   await page.getByRole('button',{name:'Aumentar cantidad',exact:true}).click();
   await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).click();
   await page.getByText('4 unidades disponibles',{exact:true}).waitFor();await page.getByText(b.sku,{exact:true}).waitFor();
   assert.match(await page.locator('body').innerText(),/30\.000/);await page.getByRole('button',{name:'Aumentar cantidad',exact:true}).locator('..').getByText('1',{exact:true}).waitFor();
   assert((await page.locator('img').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('src')))).includes(cover.url));
   assert(!(await page.locator('img').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('src')))).includes(tonePhoto.url));
   for(let i=0;i<6;i++)if(!await page.getByRole('button',{name:'Aumentar cantidad',exact:true}).isDisabled())await page.getByRole('button',{name:'Aumentar cantidad',exact:true}).click();
   assert(await page.getByRole('button',{name:'Aumentar cantidad',exact:true}).isDisabled());
   await page.getByRole('button',{name:'115 · Warm Beige, Agotado',exact:true}).click();assert(await page.getByRole('button',{name:'Agotado temporalmente',exact:true}).isDisabled());
   await capture(page,'mobile-sold-out.png');
  });
  await record('two UUID cart lines and real checkout retain shade codes names and totals',async()=>{
   await page.getByRole('button',{name:'107 · Natural Ivory',exact:true}).click();await page.getByRole('button',{name:/Agregar al carrito/}).click();
   await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).click();await page.getByRole('button',{name:/Agregar al carrito/}).click();
   await page.goto(`${web}/carrito`);await page.getByText('107 · Natural Ivory',{exact:true}).waitFor();await page.getByText('110 · Classic Ivory',{exact:true}).waitFor();
   const cart=await page.evaluate(()=>JSON.parse(localStorage.getItem('alexa_beauty_cart_v2')));assert.equal(cart.length,2);assert.notEqual(cart[0].variantId,cart[1].variantId);
   await page.goto(`${web}/pedido`);await page.getByText('Tono: 107 · Natural Ivory',{exact:true}).waitFor();
   await page.getByLabel('Nombre y apellido').fill('Local shade rehearsal');await page.getByLabel('Número de WhatsApp').fill('573001234567');
   await page.getByLabel('Dirección exacta').fill('Synthetic local test address');await page.getByLabel('Barrio en Valledupar').fill('Synthetic test district');
   await page.evaluate(()=>{window.open=()=>null;});
   const submitted=page.waitForResponse(response=>response.url().endsWith('/api/backend/orders')&&response.request().method()==='POST');
   await page.getByRole('button',{name:/Confirmar|Crear pedido|Enviar pedido|Realizar pedido|Continuar por WhatsApp/}).click();
   const submittedResponse=await submitted;assert.equal(submittedResponse.status(),201);const order=(await submittedResponse.json()).data;
   assert.equal(order.total,55000);assert.equal(order.items.length,2);assert.match(order.whatsappMessageText,/107.*Natural Ivory/);assert.match(order.whatsappMessageText,/110.*Classic Ivory/);
   assert.match(decodeURIComponent(order.whatsappUrl),/107.*Natural Ivory/);await page.waitForURL(/\/pedido\/confirmado\//);await page.getByText('107 · Natural Ivory',{exact:false}).first().waitFor();
   await capture(page,'checkout-confirmed-mobile.png');
   const confirmed=await request(`/admin/orders/${order.orderId}/confirm-payment`,{paymentMethod:'EFECTIVO',amount:55000,transferReference:`LOCAL-${stamp}`});assert.equal(confirmed.status,'VERIFICADO');
  });
  await record('six mobile/tablet/desktop widths, keyboard, no horizontal page overflow',async()=>{
   await page.goto(`${web}/producto/${product.slug}`);
   for(const width of [320,360,375,390,768,1280]){await page.setViewportSize({width,height:900});await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).click();
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),`Overflow at ${width}`);
    await capture(page,`selected-${width}.png`);}
   await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).focus();await page.keyboard.press('Enter');assert.equal(await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).getAttribute('aria-pressed'),'true');
   for(const button of await page.getByRole('group',{name:'Tonos disponibles'}).getByRole('button').all()){const box=await button.boundingBox();assert(box.width>=44&&box.height>=44,'Shade touch target below 44px');}
   await page.emulateMedia({reducedMotion:'reduce'});assert(await page.getByRole('button',{name:'110 · Classic Ivory',exact:true}).evaluate(node=>getComputedStyle(node).transitionDuration.split(',').every(value=>parseFloat(value)<=0.001)),'Reduced motion must disable meaningful transitions');
  });
  await record('quick selector requires explicit choice, traps/restores focus and supports Escape',async()=>{
   await page.goto(`${web}/catalogo`);const trigger=page.getByRole('button',{name:`Agregar o elegir tono de ${product.name}`,exact:true});await trigger.click();
   const dialog=page.getByRole('dialog');await dialog.waitFor();assert(await dialog.getByRole('button',{name:'Agregar al carrito',exact:true}).isDisabled());
   await page.keyboard.press('Shift+Tab');assert(await dialog.evaluate(node=>node.contains(document.activeElement)));
   await dialog.getByRole('button',{name:'110 · Classic Ivory',exact:true}).click();assert.equal(await dialog.getByRole('button',{name:'110 · Classic Ivory',exact:true}).getAttribute('aria-pressed'),'true');
   await capture(page,'quick-selector-mobile.png');
   await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});assert(await trigger.evaluate(node=>node===document.activeElement));
  });
  await record('20 long shades and no-color presentation/legacy defaults remain responsive',async()=>{
   const many=await request('/admin/products',{name:`Long shades ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:20000,variants:Array.from({length:20},(_,i)=>shade(String(200+i),`Long descriptive foundation shade ${i} with detailed undertone`,i%3?2:0,i%2?'#E4C9B4':undefined))});
   await page.setViewportSize({width:320,height:900});await page.goto(`${web}/producto/${many.slug}`);await page.getByRole('button',{name:/201 · Long descriptive/}).waitFor();assert.equal(await page.getByRole('group',{name:'Tonos disponibles'}).getByRole('button').count(),20);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await capture(page,'long-shades-320.png');
   const sizes=await request('/admin/products',{name:`Sizes ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:10000,variants:[{presentation:'15 ml',salePrice:10000,stockOnHand:2},{presentation:'30 ml',salePrice:15000,stockOnHand:1}]});
   await page.goto(`${web}/producto/${sizes.slug}`);await page.getByRole('button',{name:'15 ml',exact:true}).waitFor();assert.equal(await page.getByTestId('shade-swatch').count(),0);
   const plain=await request('/admin/products',{name:`No shades ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:10000,stockOnHand:1});await page.goto(`${web}/producto/${plain.slug}`);await page.getByRole('button',{name:/Agregar al carrito/}).waitFor();assert.equal(await page.getByRole('group',{name:'Tonos disponibles'}).count(),0);
   const legacy=await request('/admin/products',{name:`Legacy single shade ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:10000,variants:[{shadeName:'Legacy Ivory',salePrice:10000,stockOnHand:1}]});
   await page.goto(`${web}/producto/${legacy.slug}`);await page.getByRole('button',{name:'Legacy Ivory',exact:true}).waitFor();assert.equal(await page.getByTestId('shade-swatch').count(),0);assert.equal(await page.getByRole('button',{name:'Legacy Ivory',exact:true}).getAttribute('aria-pressed'),'true');
   await capture(page,'legacy-single-320.png');
   const empty=await request('/admin/products',{name:`All sold out ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:10000,variants:[shade('901','Empty Ivory',0,'#EDD4BF'),shade('902','Empty Beige',0,'#C6A285')]});
   await page.goto(`${web}/producto/${empty.slug}`);assert(await page.getByRole('button',{name:'Agotado temporalmente',exact:true}).isDisabled());
  });
  await record('slow error and aborted API states do not fabricate availability',async()=>{
   let release;const delay=new Promise(resolve=>{release=resolve;});await page.route('**/api/backend/catalog/products/slow-shade',async route=>{await delay;await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({data:product})});});
   await page.goto(`${web}/producto/slow-shade`);await page.getByText('Cargando detalles de belleza...',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:/Agregar al carrito/}).count(),0);release();await page.getByRole('button',{name:'107 · Natural Ivory',exact:true}).waitFor();
   await page.route('**/api/backend/catalog/products/missing-shade',route=>route.fulfill({status:503,contentType:'application/json',body:'{"detail":"Offline"}'}));await page.goto(`${web}/producto/missing-shade`);await page.getByRole('heading',{name:'Producto no encontrado'}).waitFor();assert.equal(await page.getByRole('button',{name:/Agregar al carrito/}).count(),0);
   await page.route('**/api/backend/catalog/products/offline-shade',route=>route.abort('connectionfailed'));await page.goto(`${web}/producto/offline-shade`);await page.getByRole('heading',{name:'Producto no encontrado'}).waitFor();assert.equal(await page.getByRole('button',{name:/Agregar al carrito/}).count(),0);await capture(page,'api-error-mobile.png');
  });
  await record('checkout inventory conflict displays server error and retains UUID cart line',async()=>{
   const limited=await request('/admin/products',{name:`Changed stock ${stamp}`,brandId:product.brand.id,categoryId:product.category.id,salePrice:10000,stockOnHand:1});
   await page.goto(`${web}/producto/${limited.slug}`);await page.getByRole('button',{name:/Agregar al carrito/}).click();
   await request('/orders',{customerName:'Synthetic competing buyer',customerPhone:'573001234567',deliveryType:'LOCAL_PICKUP',items:[{variantId:limited.variants[0].id,quantity:1}]});
   await page.goto(`${web}/pedido`);await page.getByLabel('Nombre y apellido').fill('Synthetic failed reservation');await page.getByLabel('Número de WhatsApp').fill('573001234567');await page.getByRole('button',{name:/Retiro en tienda/}).click();
   const conflict=page.waitForResponse(response=>response.url().endsWith('/api/backend/orders')&&response.request().method()==='POST');await page.getByRole('button',{name:/Confirmar pedido y abrir WhatsApp/}).click();assert.equal((await conflict).status(),409);
   await page.getByText('No se pudo completar el pedido:',{exact:true}).waitFor();assert.match(await page.locator('body').innerText(),/Stock insuficiente/);
   assert.equal((await page.evaluate(()=>JSON.parse(localStorage.getItem('alexa_beauty_cart_v2'))))[0].variantId,limited.variants[0].id);await capture(page,'checkout-stock-error-mobile.png');
  });
  await context.close();
 } finally {await browser.close();fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify(evidence,null,2)+'\n');}
})().catch(error=>{console.error(error);process.exitCode=1;});
