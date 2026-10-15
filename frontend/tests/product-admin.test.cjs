const { test, afterEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const ts = require("typescript");
const Module = require("node:module");
const { JSDOM } = require("jsdom");
const dom = new JSDOM("<!doctype html><html><body></body></html>", {
  url: "http://localhost",
});
Object.defineProperty(global, "navigator", {
  value: dom.window.navigator,
  configurable: true,
});
for (const key of [
  "window",
  "document",
  "HTMLElement",
  "HTMLInputElement",
  "Event",
  "MouseEvent",
  "File",
  "FileList",
  "Blob",
])
  global[key] = dom.window[key];
global.IS_REACT_ACT_ENVIRONMENT = true;
global.URL.createObjectURL = () => "/preview";
global.URL.revokeObjectURL = () => {};
const original = Module._resolveFilename;
Module._resolveFilename = function (name, parent, ...rest) {
  return original.call(
    this,
    name.startsWith("@/") ? path.resolve("src", name.slice(2)) : name,
    parent,
    ...rest,
  );
};
for (const ext of [".ts", ".tsx"])
  require.extensions[ext] = (m, f) =>
    m._compile(
      ts.transpileModule(fs.readFileSync(f, "utf8"), {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          jsx: ts.JsxEmit.ReactJSX,
          esModuleInterop: true,
        },
      }).outputText,
      f,
    );
const React = require("react");
const { render, screen, cleanup, waitFor } = require("@testing-library/react");
const userEvent = require("@testing-library/user-event").default;
const { api } = require("../src/lib/api.ts");
const { BrandPicker } = require("../src/components/admin/BrandPicker.tsx");
const { VariantFields } = require("../src/components/admin/VariantFields.tsx");
afterEach(cleanup);
test("admin creates arbitrary brand and sees validation error without losing input", async () => {
  const user = userEvent.setup();
  let selected;
  let fails = true;
  api.createBrand = async (name) => {
    assert.equal(name, "Mi marca libre");
    if (fails) throw new Error("No se pudo conectar");
    return { id: "new", name, slug: "mi-marca-libre", active: true };
  };
  render(
    React.createElement(BrandPicker, {
      brands: [],
      value: "",
      onChange: () => {},
      onCreated: (b) => (selected = b),
    }),
  );
  await user.click(screen.getByText("Crear una marca"));
  await user.type(
    screen.getByLabelText("Nombre de la nueva marca"),
    "Mi marca libre",
  );
  await user.click(
    screen.getByRole("button", { name: "Crear y seleccionar marca" }),
  );
  assert.equal(
    (await screen.findByRole("alert")).textContent,
    "No se pudo conectar",
  );
  assert.equal(
    screen.getByLabelText("Nombre de la nueva marca").value,
    "Mi marca libre",
  );
  fails = false;
  await user.click(
    screen.getByRole("button", { name: "Crear y seleccionar marca" }),
  );
  await waitFor(() => assert.equal(selected.name, "Mi marca libre"));
});
test("purchase cost is independent of sale price, blank distinct from confirmed zero", async () => {
  const user = userEvent.setup();
  let latest;
  function Form() {
    const [value, setValue] = React.useState({ salePrice: 100, minStock: 3 });
    return React.createElement(VariantFields, {
      value,
      onChange: (v) => {
        latest = v;
        setValue(v);
      },
    });
  }
  render(React.createElement(Form));
  const cost = screen.getByRole("spinbutton", { name: /Costo de compra/ });
  await user.type(cost, "0");
  assert.equal(latest.referenceCost, 0);
  assert.equal(latest.salePrice, 100);
  await user.clear(cost);
  assert.equal(latest.referenceCost, null);
});

test("photo error remains visible inside dialog and retry persists after reopening", async () => {
  const load = Module._load;
  Module._load = function (name, ...args) {
    if (name === "lucide-react")
      return new Proxy(
        {},
        {
          get: () => () => React.createElement("svg", { "aria-hidden": true }),
        },
      );
    if (name === "next/image" || name === "next/link")
      return {
        __esModule: true,
        default: ({ children, fill, unoptimized, priority, ...props }) =>
          React.createElement(
            name === "next/image" ? "img" : "a",
            props,
            children,
          ),
      };
    if (name === "@/components/admin/AdminShell")
      return {
        __esModule: true,
        default: ({ children, actions }) => React.createElement("main", null, actions, children),
      };
    return load.call(this, name, ...args);
  };
  const Page = require("../src/app/admin/productos/page.tsx").default;
  Module._load = load;
  const brand = { id: "b", name: "Marca", slug: "marca", active: true };
  const category = {
    id: "c",
    name: "Rostro",
    slug: "rostro",
    active: true,
    sortOrder: 0,
  };
  const product = {
    id: "p",
    sku: "PHOTO",
    name: "Producto foto",
    slug: "producto-foto",
    brand,
    category,
    minPrice: 100,
    maxPrice: 100,
    totalAvailableStock: 5,
    active: true,
    featured: false,
    variants: [],
    images: [],
  };
  api.getAdminBrands = async () => [brand];
  api.getCategories = async () => [category];
  api.getAdminProducts = async () => ({
    content: [product],
    totalPages: 1,
    totalElements: 1,
  });
  api.getAdminProductById = async () => ({
    ...product,
    images: [...product.images],
  });
  let calls = 0;
  api.uploadProductImage = async () => {
    calls++;
    if (calls === 1)
      throw new Error("Almacenamiento temporalmente no disponible");
    const image = { id: "photo", url: "/uploads/photo.png", isCover: true };
    product.images = [image];
    return image;
  };
  const user = userEvent.setup();
  render(React.createElement(Page));
  await user.click(await screen.findByRole("button", { name: "Fotos" }));
  await user.upload(
    screen.getByLabelText(/Adjuntar Nueva Foto/),
    new File(["png"], "photo.png", { type: "image/png" }),
  );
  await user.click(screen.getByRole("button", { name: "Guardar Foto" }));
  const { within } = require("@testing-library/react");
  const dialog = screen.getByRole("dialog");
  assert.match(within(dialog).getByRole("alert").textContent, /Almacenamiento/);
  assert.ok(within(dialog).getByText("photo.png"));
  await user.click(screen.getByRole("button", { name: "Guardar Foto" }));
  await waitFor(() => assert.equal(product.images.length, 1));
  await user.click(screen.getByRole("button", { name: "Cerrar" }));
  await user.click(screen.getByRole("button", { name: "Fotos" }));
  await waitFor(() => assert.ok(screen.getByText("Principal")));
  assert.equal(calls, 2);
});

test("partial product creation retries only image upload, never creates product twice", async () => {
 const Page = require("../src/app/admin/productos/page.tsx").default;
 const brand={id:"b",name:"Marca",slug:"marca",active:true};const category={id:"c",name:"Rostro",slug:"rostro",sortOrder:0,active:true};
 const product={id:"created",sku:"NEW",name:"Producto nuevo",slug:"nuevo",brand,category,active:true,featured:false,images:[],variants:[],totalAvailableStock:10};
 api.getAdminBrands=async()=>[brand];api.getCategories=async()=>[category];api.getAdminProducts=async()=>({content:[],totalPages:0,totalElements:0});
 let creates=0,uploads=0;api.createProduct=async()=>{creates++;return product;};api.uploadProductImage=async id=>{assert.equal(id,"created");uploads++;if(uploads===1)throw new Error("Carga fallida");return {id:"photo",url:"/uploads/new.png",isCover:true};};
 const user=userEvent.setup();render(React.createElement(Page));await user.click(await screen.findByRole("button",{name:"Nuevo Producto"}));await user.type(screen.getByLabelText("Nombre del producto"),"Producto nuevo");await user.type(screen.getByLabelText("Precio general COP"),"100");await user.upload(screen.getByLabelText("Foto del nuevo producto"),new File(["png"],"new.png",{type:"image/png"}));await user.click(screen.getByRole("button",{name:"Crear Producto"}));
 const {within}=require("@testing-library/react");await waitFor(()=>assert.match(within(screen.getByRole("dialog")).getByRole("alert").textContent,/Producto creado.*Carga fallida/));await user.click(screen.getByRole("button",{name:"Guardar Foto"}));await waitFor(()=>assert.equal(uploads,2));assert.equal(creates,1);
});

test("product metadata editor saves admin-selected arbitrary brand", async () => {
 const {ProductEditor}=require("../src/components/admin/ProductEditor.tsx");
 const oldBrand={id:"old",name:"Anterior",slug:"anterior",active:true},newBrand={id:"new",name:"Marca personalizada",slug:"marca-personalizada",active:true};const category={id:"c",name:"Rostro",slug:"rostro",sortOrder:0,active:true};
 api.getAdminProductById=async()=>({id:"p",name:"Original",brand:oldBrand,category,description:"Antes",variants:[],images:[]});api.getAdminBrands=async()=>[oldBrand,newBrand];api.getCategories=async()=>[category];let saved;
 api.updateProduct=async(id,data)=>{saved={id,...data};return saved;};
 const user=userEvent.setup();render(React.createElement(ProductEditor,{id:"p",onClose:()=>{},onSaved:()=>{}}));await screen.findByLabelText("Nombre del producto");await user.clear(screen.getByLabelText("Nombre del producto"));await user.type(screen.getByLabelText("Nombre del producto"),"Actualizado");await user.selectOptions(screen.getByLabelText("Marca"),"new");await user.click(screen.getByRole("button",{name:"Guardar producto"}));await waitFor(()=>assert.equal(saved.brandId,"new"));assert.equal(saved.name,"Actualizado");
});
