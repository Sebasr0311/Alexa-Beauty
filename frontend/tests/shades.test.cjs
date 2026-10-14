const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const ts = require('typescript');
const code = ts.transpileModule(fs.readFileSync('src/lib/shades.ts', 'utf8'), {compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText;
const target = {exports:{}};
new Function('exports','require','module',code)(target.exports,require,target);
const { initialVariant, variantLabel, variantImages, activeVariants } = target.exports;
const a={id:'a',active:true,availableStock:0,shadeCode:'107',shadeName:'Ivory',presentation:'30 ml'};
const b={id:'b',active:true,availableStock:4,shadeCode:'110',shadeName:'Classic Ivory'};
test('select first active in-stock shade, preserve exhausted choices',()=>{
  assert.equal(initialVariant([a,{...b,id:'hidden',active:false},b]).id,'b');
  assert.equal(activeVariants([a,b,{...b,active:false}]).length,2);
  assert.equal(initialVariant([{...a,active:false}]),null);
});
test('labels retain commercial code name and size, never manufacture metadata',()=>{
  assert.equal(variantLabel(a),'107 · Ivory · 30 ml');
  assert.equal(variantLabel({presentation:'15 ml'}),'15 ml');
  assert.equal(variantLabel({shadeName:'Default'}),'Única presentación');
});
test('photos never leak other shade, fall back only to general cover',()=>{
  const images=[{id:'wrong',variantId:'b',isCover:true},{id:'general',variantId:null,isCover:true},{id:'own',variantId:'a'}];
  assert.equal(variantImages(images,'a')[0].id,'own');
  assert.equal(variantImages(images,'missing')[0].id,'general');
  assert.deepEqual(variantImages([images[0]],'a'),[]);
});
