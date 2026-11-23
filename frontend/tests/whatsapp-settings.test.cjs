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
  "HTMLButtonElement",
  "HTMLFormElement",
  "HTMLAnchorElement",
  "Event",
  "MouseEvent",
  "File",
  "FileList",
  "Blob",
]) {
  global[key] = dom.window[key];
}
global.IS_REACT_ACT_ENVIRONMENT = true;

const originalResolveFilename = Module._resolveFilename;
Module._resolveFilename = function (name, parent, ...rest) {
  return originalResolveFilename.call(
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
const { render, screen, cleanup, waitFor } = require("@testing-library/react");
const userEvent = require("@testing-library/user-event").default;

const {
  normalizeWhatsAppNumber,
  isValidWhatsAppNumber,
  formatWhatsAppNumberDisplay,
  whatsappContact,
} = require("../src/lib/contact.ts");
const { api } = require("../src/lib/api.ts");

const originalLoad = Module._load;
Module._load = function (name, ...args) {
  if (name === "lucide-react") {
    return new Proxy(
      {},
      {
        get: () => () => React.createElement("svg", { "aria-hidden": true }),
      },
    );
  }
  return originalLoad.call(this, name, ...args);
};
const { WhatsAppSettingsForm } = require("../src/components/admin/WhatsAppSettingsForm.tsx");
Module._load = originalLoad;

const originalUpdateWhatsAppNumber = api.updateWhatsAppNumber;

afterEach(() => {
  cleanup();
  api.updateWhatsAppNumber = originalUpdateWhatsAppNumber;
});

test("contact helpers normalize Colombian mobile numbers and build wa.me links safely", () => {
  assert.equal(normalizeWhatsAppNumber("323 5814497"), "573235814497");
  assert.equal(normalizeWhatsAppNumber("+57 323-581-4497"), "573235814497");
  assert.equal(normalizeWhatsAppNumber("14155552671"), "14155552671");
  assert.equal(normalizeWhatsAppNumber(""), "");
  assert.equal(normalizeWhatsAppNumber(null), "");

  assert.equal(isValidWhatsAppNumber("323 5814497"), true);
  assert.equal(isValidWhatsAppNumber("573235814497"), true);
  assert.equal(isValidWhatsAppNumber("14155552671"), true);
  assert.equal(isValidWhatsAppNumber("12345"), false);
  assert.equal(isValidWhatsAppNumber("abc"), false);
  assert.equal(isValidWhatsAppNumber("0123456789"), false);
  assert.equal(isValidWhatsAppNumber(null), false);

  assert.equal(formatWhatsAppNumberDisplay("573235814497"), "+57 323 581 4497");
  assert.equal(formatWhatsAppNumberDisplay("14155552671"), "+14155552671");
  assert.equal(formatWhatsAppNumberDisplay(""), "");

  assert.equal(
    whatsappContact("Hola tienda", "323 5814497"),
    "https://wa.me/573235814497?text=Hola%20tienda",
  );
  assert.equal(whatsappContact("Hola tienda", null), undefined);
  assert.equal(whatsappContact("Hola tienda", "123"), undefined);
});

test("WhatsAppSettingsForm previews normalization and saves valid number", async () => {
  const user = userEvent.setup();
  let capturedInput = null;
  api.updateWhatsAppNumber = async (rawNumber) => {
    capturedInput = rawNumber;
    return {
      whatsappNumber: "573235814497",
      updatedAt: "2026-11-23T22:30:00Z",
    };
  };

  render(
    React.createElement(WhatsAppSettingsForm, {
      initialNumber: "573001234567",
      initialUpdatedAt: "2026-10-01T12:00:00Z",
      canEdit: true,
    }),
  );

  assert.ok(screen.getAllByText("+57 300 123 4567").length >= 1);

  const input = screen.getByLabelText("Nuevo número de WhatsApp");
  await user.clear(input);
  await user.type(input, "323 5814497");

  assert.ok(screen.getByText("+57 300 123 4567"));
  assert.ok(screen.getByText("+57 323 581 4497"));
  assert.ok(screen.getByText("https://wa.me/573235814497"));

  const saveButton = screen.getByRole("button", { name: /Guardar número de WhatsApp/ });
  assert.equal(saveButton.disabled, false);

  await user.click(saveButton);

  await waitFor(() => {
    assert.equal(capturedInput, "323 5814497");
    const status = screen.getByRole("status");
    assert.match(status.textContent, /\+57 323 581 4497/);
  });
  assert.ok(screen.getAllByText("+57 323 581 4497").length >= 1);
});

test("WhatsAppSettingsForm blocks invalid numbers and shows validation message", async () => {
  const user = userEvent.setup();
  let called = false;
  api.updateWhatsAppNumber = async () => {
    called = true;
    return { whatsappNumber: "573235814497" };
  };

  render(
    React.createElement(WhatsAppSettingsForm, {
      initialNumber: "573235814497",
      canEdit: true,
    }),
  );

  const input = screen.getByLabelText("Nuevo número de WhatsApp");
  await user.clear(input);
  await user.type(input, "12345");

  const alert = screen.getByRole("alert");
  assert.match(alert.textContent, /válido/i);

  const saveButton = screen.getByRole("button", { name: /Guardar número de WhatsApp/ });
  assert.equal(saveButton.disabled, true);
  assert.equal(called, false);
});

test("WhatsAppSettingsForm renders read-only notice and disables editing when canEdit is false", () => {
  render(
    React.createElement(WhatsAppSettingsForm, {
      initialNumber: "573235814497",
      canEdit: false,
    }),
  );

  const note = screen.getByRole("note");
  assert.match(note.textContent, /Administrador/);
  assert.equal(screen.getByLabelText("Nuevo número de WhatsApp").disabled, true);
  assert.equal(screen.getByRole("button", { name: /Guardar número de WhatsApp/ }).disabled, true);
});
