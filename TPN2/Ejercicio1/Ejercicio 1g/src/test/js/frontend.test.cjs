const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Entorno DOM aislado para ejecutar el script real y probar sus eventos sin navegador.
class Element {
  constructor(tag = 'div') {
    this.tag = tag;
    this.value = '';
    this.textContent = '';
    this.children = [];
    this.events = {};
    this.dataset = {};
    this.classList = { toggle() {} };
    this.hidden = false;
    this.open = false;
  }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = children; }
  setAttribute(name, value) { this[name] = value; }
  addEventListener(name, handler) { this.events[name] = handler; }
  showModal() { this.open = true; }
  close() { this.open = false; }
  focus() {}
  reset() {}
  reportValidity() { return true; }
  async trigger(name) { await this.events[name]?.({ preventDefault() {} }); }
}

const tick = () => new Promise(resolve => setImmediate(resolve));
async function fixture() {
  const ids = ['total', 'printed', 'digital', 'search', 'filter', 'books', 'empty', 'empty-title',
    'empty-text', 'empty-add', 'results', 'status', 'reload', 'book-form', 'book-id', 'name',
    'isbn', 'date', 'price', 'type', 'editor-title', 'form-error', 'editor', 'delete-name',
    'delete-error', 'delete-dialog', 'save', 'confirm-delete', 'add-book'];
  const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
  let nextId = 1;
  let data = [];
  let fail = false;
  const requests = [];
  const fetch = async (url, options) => {
    requests.push({ url, options });
    if (fail) return { ok: false, status: 400, json: async () => ({ detail: 'Datos inválidos' }) };
    let body = '';
    if (url.endsWith('/getAllBooks')) body = JSON.stringify(data);
    else if (url.endsWith('/addBook')) {
      const book = { ...JSON.parse(options.body), id: nextId++ };
      data.push(book);
      body = JSON.stringify(book);
    } else if (url.includes('/updateBook/')) {
      const id = Number(url.split('/').at(-1));
      const book = { ...JSON.parse(options.body), id };
      data = data.map(item => item.id === id ? book : item);
      body = JSON.stringify(book);
    } else if (url.includes('/deleteBook/')) {
      const id = Number(url.split('/').at(-1));
      data = data.filter(item => item.id !== id);
    }
    return { ok: true, text: async () => body };
  };
  const document = {
    getElementById: id => elements[id],
    createElement: tag => new Element(tag),
    querySelectorAll: selector => selector === 'dialog'
      ? [elements.editor, elements['delete-dialog']] : []
  };
  const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/app.js'), 'utf8');
  vm.runInNewContext(source, { document, fetch, Intl, console });
  await tick();
  return { elements, requests, setFail: value => { fail = value; } };
}

async function add(elements, name = 'Libro de prueba', format = 'HARDCOVER') {
  await elements['add-book'].trigger('click');
  elements.name.value = name;
  elements.isbn.value = '9780000000001';
  elements.date.value = '2020-01-01';
  elements.price.value = '2500.50';
  elements.type.value = format;
  await elements['book-form'].trigger('submit');
}

test('alta, búsqueda, edición y eliminación mediante los eventos del front', async () => {
  const { elements: e, requests } = await fixture();
  assert.equal(e.empty.hidden, false);
  await add(e);
  assert.equal(e.total.textContent, 1);
  assert.equal(e.editor.open, false);
  assert.equal(e.books.children.length, 1);
  e.search.value = 'no existe';
  await e.search.trigger('input');
  assert.equal(e.books.children.length, 0);
  e.search.value = 'prueba';
  await e.search.trigger('input');
  const actions = e.books.children[0].children[1].children.at(-1);
  await actions.children[0].trigger('click');
  assert.equal(e.editor.open, true);
  e.name.value = 'Libro editado';
  await e['book-form'].trigger('submit');
  assert.ok(requests.some(request => request.options.method === 'PUT'));
  e.search.value = '';
  await e.search.trigger('input');
  const deleteButton = e.books.children[0].children[1].children.at(-1).children[1];
  await deleteButton.trigger('click');
  assert.equal(e['delete-dialog'].open, true);
  await e['confirm-delete'].trigger('click');
  assert.equal(e.total.textContent, 0);
  assert.ok(requests.some(request => request.options.method === 'DELETE'));
});

test('filtrado por formato y títulos tratados como texto', async () => {
  const { elements: e } = await fixture();
  await add(e, '<img src=x onerror=alert(1)>', 'EBOOK');
  assert.equal(e.digital.textContent, 1);
  const title = e.books.children[0].children[1].children[1];
  assert.equal(title.textContent, '<img src=x onerror=alert(1)>');
  assert.equal(title.children.length, 0);
  e.filter.value = 'HARDCOVER';
  await e.filter.trigger('change');
  assert.equal(e.books.children.length, 0);
  e.filter.value = 'EBOOK';
  await e.filter.trigger('change');
  assert.equal(e.books.children.length, 1);
});

test('un error del servidor conserva el formulario para corregir los datos', async () => {
  const { elements: e, setFail } = await fixture();
  setFail(true);
  await add(e);
  assert.equal(e.editor.open, true);
  assert.equal(e['form-error'].textContent, 'Datos inválidos');
  assert.equal(e.name.value, 'Libro de prueba');
});
