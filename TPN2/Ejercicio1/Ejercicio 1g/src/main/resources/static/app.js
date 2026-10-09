'use strict';
const $ = id => document.getElementById(id);
const formats = { HARDCOVER: 'Tapa dura', SOFTCOVER: 'Tapa blanda', EBOOK: 'Digital' };
let books = [];
let deleteId = null;
let busy = false;
const number = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const api = '/api/books';

async function request(path, options = {}) {
  const response = await fetch(api + path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers }
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.detail || (response.status === 404 ? 'El libro ya no existe. Actualizá el catálogo.' : 'No se pudo completar la operación. Intentá nuevamente.'));
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function message(text, error = false) {
  $('status').textContent = text;
  $('status').classList.toggle('error', error);
}

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function render() {
  $('total').textContent = books.length;
  $('printed').textContent = books.filter(book => book.bookType !== 'EBOOK').length;
  $('digital').textContent = books.filter(book => book.bookType === 'EBOOK').length;
  const query = $('search').value.trim().toLocaleLowerCase('es');
  const format = $('filter').value;
  const visible = books.filter(book => (!format || book.bookType === format) &&
    `${book.name} ${book.isbnNumber}`.toLocaleLowerCase('es').includes(query));
  $('books').replaceChildren();
  visible.forEach(book => {
    const card = element('article', undefined, 'book-card');
    const cover = element('div', undefined, `cover ${book.bookType}`);
    cover.setAttribute('aria-hidden', 'true');
    cover.append(element('div', (book.name || '?').slice(0, 1).toUpperCase(), 'cover-spine'));
    const content = element('div', undefined, 'card-content');
    const date = book.publishDate ? book.publishDate.split('-').reverse().join('/') : 'Sin fecha';
    content.append(element('span', formats[book.bookType] || book.bookType, `badge ${book.bookType}`),
      element('h3', book.name), element('div', `ISBN ${book.isbnNumber}`, 'metadata'),
      element('div', `Publicado el ${date}`, 'metadata'), element('p', `$ ${number.format(book.price)}`, 'price'));
    const actions = element('div', undefined, 'card-actions');
    const edit = element('button', 'Editar');
    edit.setAttribute('aria-label', `Editar ${book.name}`);
    edit.addEventListener('click', () => openEditor(book));
    const remove = element('button', 'Eliminar', 'delete');
    remove.setAttribute('aria-label', `Eliminar ${book.name}`);
    remove.addEventListener('click', () => {
      deleteId = book.id;
      $('delete-name').textContent = book.name;
      $('delete-error').textContent = '';
      $('delete-dialog').showModal();
    });
    actions.append(edit, remove);
    content.append(actions);
    card.append(cover, content);
    $('books').append(card);
  });
  $('empty').hidden = visible.length !== 0;
  $('empty-title').textContent = books.length ? 'No encontramos ese libro' : 'Tu biblioteca empieza acá';
  $('empty-text').textContent = books.length ? 'Probá con otro título, ISBN o formato.' : 'Agregá tu primer libro y dale forma a tu colección.';
  $('empty-add').hidden = books.length !== 0;
  $('results').textContent = visible.length ? `${visible.length} de ${books.length} libros` : '';
}

async function load(successMessage = '') {
  $('reload').disabled = true;
  message('Cargando tu catálogo…');
  try {
    books = await request('/getAllBooks');
    books.sort((a, b) => b.id - a.id);
    render();
    message(successMessage);
  } catch (error) {
    message('No pudimos cargar el catálogo. Comprobá la conexión y presioná Actualizar.', true);
  } finally {
    $('reload').disabled = false;
  }
}

function openEditor(book = null) {
  $('book-form').reset();
  $('book-id').value = book?.id ?? '';
  $('name').value = book?.name ?? '';
  $('isbn').value = book?.isbnNumber ?? '';
  $('date').value = book?.publishDate ?? '';
  $('price').value = book?.price ?? '';
  $('type').value = book?.bookType ?? 'HARDCOVER';
  $('editor-title').textContent = book ? 'Editar libro' : 'Agregar libro';
  $('form-error').textContent = '';
  $('editor').showModal();
  $('name').focus();
}

function setBusy(value) {
  busy = value;
  document.querySelectorAll('dialog button').forEach(button => { button.disabled = value; });
}

$('book-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !$('book-form').reportValidity()) return;
  const id = $('book-id').value;
  const book = { name: $('name').value.trim(), isbnNumber: $('isbn').value.trim(),
    publishDate: $('date').value, price: Number($('price').value), bookType: $('type').value };
  if (!book.name || !book.isbnNumber) {
    $('form-error').textContent = 'Completá el título y el ISBN.';
    return;
  }
  setBusy(true);
  $('form-error').textContent = '';
  try {
    await request(id ? `/updateBook/${id}` : '/addBook', { method: id ? 'PUT' : 'POST', body: JSON.stringify(book) });
    $('editor').close();
    await load(id ? 'Los cambios se guardaron.' : 'Libro agregado a tu colección.');
  } catch (error) {
    $('form-error').textContent = error.message;
  } finally { setBusy(false); }
});

$('confirm-delete').addEventListener('click', async () => {
  if (busy || deleteId === null) return;
  setBusy(true);
  try {
    await request(`/deleteBook/${deleteId}`, { method: 'DELETE' });
    $('delete-dialog').close();
    deleteId = null;
    await load('Libro eliminado del catálogo.');
  } catch (error) { $('delete-error').textContent = error.message; }
  finally { setBusy(false); }
});

document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => {
  if (!busy) $(button.dataset.close).close();
}));
document.querySelectorAll('dialog').forEach(dialog => dialog.addEventListener('cancel', event => {
  if (busy) event.preventDefault();
}));
$('add-book').addEventListener('click', () => openEditor());
$('empty-add').addEventListener('click', () => openEditor());
$('reload').addEventListener('click', () => load());
$('search').addEventListener('input', render);
$('filter').addEventListener('change', render);
load();
