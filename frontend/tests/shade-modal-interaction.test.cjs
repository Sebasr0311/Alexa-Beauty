const { test, afterEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const ts = require("typescript");
const Module = require("node:module");
const { JSDOM } = require("jsdom");

const dom = new JSDOM("<!doctype html><html><body><div id=\"root\"></div></body></html>", {
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
  "HTMLButtonElement",
  "Event",
  "MouseEvent",
  "KeyboardEvent",
  "File",
  "FileList",
  "Blob",
]) {
  global[key] = dom.window[key];
}

global.IS_REACT_ACT_ENVIRONMENT = true;

const original = Module._resolveFilename;
Module._resolveFilename = function (name, parent, ...rest) {
  return original.call(
    this,
    name.startsWith("@/") ? path.resolve("src", name.slice(2)) : name,
    parent,
    ...rest,
  );
};

for (const ext of [".ts", ".tsx"]) {
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
}

const React = require("react");
const { render, screen, cleanup, fireEvent } = require("@testing-library/react");
const { ShadeSelectorModal } = require("../src/components/catalog/ShadeSelectorModal.tsx");

afterEach(cleanup);

const mockProduct = {
  id: "prod-1",
  name: "Base Líquida Radiante",
  slug: "base-liquida-radiante",
  brand: { id: "b1", name: "Luxe Beauty" },
  images: [{ id: "img-1", url: "https://example.com/base.jpg", isCover: true }],
  variants: [
    { id: "v1", active: true, availableStock: 10, salePrice: 45000, shadeName: "Porcelana", shadeCode: "01", sku: "BLR-01" },
    { id: "v2", active: true, availableStock: 5, salePrice: 45000, shadeName: "Vainilla", shadeCode: "02", sku: "BLR-02" },
    { id: "v3", active: false, availableStock: 0, salePrice: 45000, shadeName: "Almendra", shadeCode: "03", sku: "BLR-03" },
  ],
};

test("modal renders directly into document.body via portal and outside parent container", () => {
  const container = document.getElementById("root");
  let closed = false;
  render(
    React.createElement(ShadeSelectorModal, {
      product: mockProduct,
      isOpen: true,
      onClose: () => { closed = true; },
      onSelectVariant: () => {},
    }),
    { container }
  );

  const dialog = screen.getByRole("dialog");
  assert.ok(dialog);
  // Modal must not be inside #root, but in document.body
  assert.equal(container.contains(dialog), false);
  assert.equal(document.body.contains(dialog), true);
});

test("cursor movement and pointer events inside and outside dialog do not close or flicker modal", () => {
  let closeCount = 0;
  render(
    React.createElement(ShadeSelectorModal, {
      product: mockProduct,
      isOpen: true,
      onClose: () => { closeCount++; },
      onSelectVariant: () => {},
    })
  );

  const dialog = screen.getByRole("dialog");
  const backdrop = dialog.parentElement;

  // Move mouse 25 times over dialog, backdrop, and document body
  for (let i = 0; i < 25; i++) {
    fireEvent.mouseOver(dialog);
    fireEvent.mouseMove(dialog);
    fireEvent.mouseLeave(dialog);
    fireEvent.mouseOver(backdrop);
    fireEvent.mouseMove(backdrop);
    fireEvent.mouseLeave(backdrop);
  }

  assert.equal(closeCount, 0, "Mouse movement must never trigger close");
});

test("clicking inside dialog panel does not close modal, backdrop click closes once", () => {
  let closeCount = 0;
  render(
    React.createElement(ShadeSelectorModal, {
      product: mockProduct,
      isOpen: true,
      onClose: () => { closeCount++; },
      onSelectVariant: () => {},
    })
  );

  const dialog = screen.getByRole("dialog");
  const backdrop = dialog.parentElement;

  // Click on dialog heading and body
  fireEvent.click(dialog);
  assert.equal(closeCount, 0, "Click inside dialog must not close modal");

  // Click directly on backdrop
  fireEvent.click(backdrop);
  assert.equal(closeCount, 1, "Click on backdrop must trigger close exactly once");
});

test("escape key closes modal and X button closes modal", () => {
  let closeCount = 0;
  render(
    React.createElement(ShadeSelectorModal, {
      product: mockProduct,
      isOpen: true,
      onClose: () => { closeCount++; },
      onSelectVariant: () => {},
    })
  );

  // Press Escape
  fireEvent.keyDown(document, { key: "Escape" });
  assert.equal(closeCount, 1, "Escape must trigger close");

  // Click close button
  const closeBtn = screen.getByLabelText("Cerrar selector de tonos");
  fireEvent.click(closeBtn);
  assert.equal(closeCount, 2, "Close button must trigger close");
});
