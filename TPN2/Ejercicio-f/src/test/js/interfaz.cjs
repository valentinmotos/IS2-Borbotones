// Prueba DOM del cliente real contra una API levantada, sin automatizar un navegador.
// npm.cmd install --prefix target/ui-check --no-save --package-lock=false jsdom@30.1.2
// node src/test/js/interfaz.cjs http://localhost:8080
const path = require('node:path');
const { JSDOM } = require(path.resolve(__dirname, '../../../target/ui-check/node_modules/jsdom'));
const assert = require('node:assert/strict');
const base = process.argv[2] || 'http://localhost:8080';
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1kAAAAASUVORK5CYII=', 'base64');

async function createClient(route = '/') {
  const html = await (await fetch(base + route)).text();
  const dom = new JSDOM(html, { url: base + route, runScripts: 'outside-only' });
  const window = dom.window;
  window.TextEncoder = TextEncoder;
  window.URL.createObjectURL = URL.createObjectURL;
  window.URL.revokeObjectURL = URL.revokeObjectURL;
  const files = new WeakMap();
  const DomFormData = window.FormData;
  window.FormData = class extends DomFormData {
    constructor(form) {
      super(form);
      const input = form?.elements.namedItem('archivo');
      if (input && files.has(input)) this.set('archivo', files.get(input));
    }
  };
  let failPhotoOnce = false;
  window.fetch = async (url, options = {}) => {
    const absolute = new URL(url, base);
    if (failPhotoOnce && options.method === 'PUT' && absolute.pathname.endsWith('/foto')) {
      failPhotoOnce = false;
      return new Response(JSON.stringify({ detail: 'Fallo transitorio de foto para verificar reintento' }), {
        status: 503, headers: { 'Content-Type': 'application/problem+json' }
      });
    }
    if (options.body instanceof window.FormData) {
      const native = new FormData();
      for (const [key, value] of options.body.entries()) {
        if (typeof value === 'string') native.append(key, value);
        else {
          const bytes = await new Promise((resolve, reject) => {
            const reader = new window.FileReader();
            reader.onload = () => resolve(new Uint8Array(reader.result));
            reader.onerror = reject; reader.readAsArrayBuffer(value);
          });
          native.append(key, new Blob([bytes], { type: value.type }), value.name);
        }
      }
      options = { ...options, body: native };
    }
    return fetch(absolute, options);
  };
  const script = (await Promise.all(['/views.js', '/api.js', '/app.js'].map(async file => {
    const response = await fetch(base + file); assert.equal(response.status, 200); return response.text();
  }))).join('\n');
  window.eval(script);
  const document = window.document;
  const byId = id => document.getElementById(id);
  async function idle(allowError = false) {
    for (let i = 0; i < 500; i++) {
      await new Promise(resolve => setTimeout(resolve, 20));
      if (!document.body.hasAttribute('aria-busy')) {
        if (!allowError && byId('message').classList.contains('alert-danger')) throw new Error(byId('message').textContent);
        return;
      }
    }
    throw new Error('La accion de interfaz no termino');
  }
  async function click(element, allowError = false) {
    assert.ok(element, 'Control encontrado'); assert.notEqual(element.disabled, true);
    element.click(); await idle(allowError);
  }
  const named = (text, parent = document) => [...parent.querySelectorAll('a,button')].find(element => element.textContent.trim() === text);
  const row = name => [...document.querySelectorAll('#pets-body tr')].find(element => [...element.cells].some(cell => cell.textContent === name));
  async function submit(id, values, allowError = false) {
    const form = byId(id); assert.ok(form);
    for (const [key, value] of Object.entries(values)) form.elements.namedItem(key).value = value;
    assert.ok(form.checkValidity(), 'Formulario valido');
    form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true })); await idle(allowError);
  }
  await idle();
  return { dom, window, document, byId, idle, click, named, row, submit,
    addPhoto: id => files.set(byId(id).elements.archivo, new window.File([png], 'mascota.png', { type: 'image/png' })),
    failPhoto: () => { failPhotoOnce = true; } };
}

(async () => {
  const ui = await createClient();
  const { window, document, byId, click, named, row, submit, idle } = ui;
  assert.ok(document.querySelector('.masthead .bg-circle-1'));
  assert.equal(document.querySelector('link[href="/css/one-page-wonder.min.css"]').getAttribute('href'), '/css/one-page-wonder.min.css');
  assert.equal(document.querySelector('#page .img-fluid').getAttribute('src'), '/img/m1.jpeg');
  assert.equal(byId('exposition').hidden, false);
  assert.ok(!byId('page').innerHTML.includes('th:'));
  await click(byId('demo'));
  assert.equal(window.location.pathname, '/inicio');
  await click(named('Explorar', byId('navigation')));
  assert.ok(named('Explorar con Luna'));
  await click(named('Explorar con Luna'));
  assert.ok(window.location.search.includes('idMascotaPropia='));
  assert.match(byId('explore-content').textContent, /Juan Demo/);
  await click(named('❤️ Votar'));
  assert.match(byId('message').textContent, /Voto enviado/);
  await click(byId('switch-juan'));
  await click(named('Votos', byId('navigation')));
  assert.match(byId('vote-content').textContent, /Luna → Toby/);
  await click(named('Aceptar y hacer match'));
  assert.equal(window.location.pathname, '/matches');
  assert.match(byId('vote-content').textContent, /Luna ♥ Toby/);
  await click(byId('switch-ana'));
  await click(named('Matches', byId('navigation')));
  assert.match(byId('vote-content').textContent, /Luna ♥ Toby/);
  await click(named('Mis Mascotas', byId('navigation')));
  await click(named('Editar', row('Luna')));
  assert.match(window.location.search, /accion=Actualizar/);
  await submit('pet-form', { nombre: 'Lunita' });
  assert.ok(row('Lunita'));
  await click(named('Eliminar', row('Lunita')));
  assert.equal(byId('pet-form').elements.nombre.disabled, true);
  await submit('pet-form', {});
  assert.equal(row('Lunita'), undefined);
  await click(named('Mascotas dadas de baja'));
  assert.ok(row('Lunita'));
  await click(named('Dar Alta', row('Lunita')));
  await submit('pet-form', {});
  assert.ok(row('Lunita'));
  await click(named('¡Agrega una Mascota!'));
  ui.addPhoto('pet-form');
  await submit('pet-form', { nombre: 'Michi', tipo: 'GATO', sexo: 'MACHO' });
  assert.ok(row('Michi'));
  await click(named('Editar', row('Michi')));
  ui.addPhoto('pet-form'); ui.failPhoto();
  await submit('pet-form', { nombre: 'Michi editado' }, true);
  assert.ok(row('Michi editado'));
  assert.equal(byId('upload-retry').hidden, false);
  await click(byId('retry-photo'));
  assert.equal(byId('upload-retry').hidden, true);
  await click(named('Perfil', byId('navigation')));
  const mail = `interfaz.${crypto.randomUUID()}@test.com`;
  ui.addPhoto('profile-form');
  await submit('profile-form', { nombre: 'Anita', mail, clave: 'nuevaClave123', clave2: 'nuevaClave123' });
  assert.match(byId('identity').textContent, /Anita/);
  await click(named('Mis Mascotas', byId('navigation')));
  assert.ok(row('Lunita'), 'Las peticiones usan las credenciales nuevas');
  await click(named('Inicio', byId('navigation')));
  window.history.back(); await new Promise(resolve => setTimeout(resolve, 40)); await idle();
  assert.equal(window.location.pathname, '/mascota/mis-mascotas');
  window.history.forward(); await new Promise(resolve => setTimeout(resolve, 40)); await idle();
  assert.equal(window.location.pathname, '/inicio');
  const history = byId('request-log').textContent;
  assert.ok(!history.includes('mascotas123'));
  assert.ok(!history.includes('nuevaClave123'));
  assert.ok(!history.includes('Basic '));
  await click(named('Salir', byId('navigation')));
  assert.equal(window.location.pathname, '/login');
  await submit('login-form', { mail, clave: 'nuevaClave123' });
  assert.match(byId('identity').textContent, /Anita/);
  await click(named('Salir', byId('navigation')));
  await click(named('Registro', byId('navigation')));
  await submit('zone-form', { nombre: 'Zona de registro', descripcion: 'Prueba de interfaz' });
  const zoneId = byId('user-zone').value;
  const registeredMail = `registro.${crypto.randomUUID()}@test.com`;
  ui.addPhoto('register-form');
  ui.failPhoto();
  await submit('register-form', { nombre: 'Registro', apellido: 'UI', mail: registeredMail, clave: 'registro123', clave2: 'registro123', zonaId: zoneId }, true);
  assert.match(byId('identity').textContent, /Registro UI/);
  assert.equal(byId('upload-retry').hidden, false, 'La cuenta queda creada aunque falle la foto');
  await click(byId('retry-photo'));
  assert.equal(byId('upload-retry').hidden, true);
  await click(named('Perfil', byId('navigation')));
  // Espera a la descarga asincrona de imagen antes de verificar su URL temporal.
  for (let i = 0; i < 100 && !byId('user-picture').src.startsWith('blob:'); i++) await new Promise(resolve => setTimeout(resolve, 20));
  assert.ok(byId('user-picture').src.startsWith('blob:'), 'La foto se descarga con autenticacion');
  const direct = await createClient('/mascota/mis-mascotas');
  assert.equal(direct.window.location.pathname, '/login');
  assert.ok(direct.byId('login-form'));
  await direct.submit('login-form', { mail: registeredMail, clave: 'registro123' });
  assert.equal(direct.window.location.pathname, '/mascota/mis-mascotas');
  assert.equal(window.sessionStorage.length, 0); assert.equal(window.localStorage.length, 0);
  direct.dom.window.close(); ui.dom.window.close();
  console.log('UI DOM + HTTP OK: plantilla original, rutas, historial, registro y perfil con fotos, nuevas credenciales, mascotas, baja/alta, reintento de foto, voto/match e inspector sin claves.');
})().catch(error => { console.error(error); process.exitCode = 1; });
