import assert from 'node:assert/strict';

// Comprobación HTTP sin navegador. Requiere los tres procesos iniciados.
const backend = process.env.CLINICA_SMOKE_BACKEND || 'http://localhost:8080';
const java = process.env.CLINICA_SMOKE_JAVA || 'http://localhost:8081';
const react = process.env.CLINICA_SMOKE_REACT || 'http://localhost:5173';
const usuario = process.env.CLINICA_SMOKE_USUARIO || 'demo';
const clave = process.env.CLINICA_SMOKE_CLAVE || 'Consultorio123!';
const headers = { Authorization: `Basic ${Buffer.from(`${usuario}:${clave}`).toString('base64')}` };
async function json(url) {
  const res = await fetch(url, { headers });
  assert.equal(res.status, 200, url);
  return res.json();
}
const historias = await json(`${backend}/api/historias`);
const usuarios = await json(`${backend}/api/usuarios`);
assert.ok(usuarios.every(u => !('clave' in u)));
assert.ok(historias.length, 'Se necesita una historia para comprobar el expediente');
const expediente = await json(`${backend}/api/historias/${historias[0].id}`);
assert.equal(expediente.totalAtenciones, expediente.atenciones.length);

const login = await fetch(`${java}/login`);
assert.equal(login.status, 200);
const cookie = login.headers.getSetCookie().map(v => v.split(';')[0]).join('; ');
const htmlLogin = await login.text();
const csrf = htmlLogin.match(/name="csrf" value="([^"]+)"/)?.[1];
assert.ok(csrf, 'El formulario incluye token CSRF');
const sesion = await fetch(`${java}/login`, { method: 'POST', redirect: 'manual', headers: { Cookie: cookie }, body: new URLSearchParams({usuario,clave,csrf}) });
assert.equal(sesion.status, 302);
assert.equal(new URL(sesion.headers.get('location'),java).pathname, '/');
for (const vista of ['pacientes','medicos','usuarios','historias']) {
  const res = await fetch(`${java}/?vista=${vista}`, { headers: {Cookie:cookie} });
  const html = await res.text();
  assert.equal(res.status,200);
  assert.ok(html.includes('Nuevo registro') && html.includes('Registros activos'), `Catálogo Java ${vista}`);
}
const historia = await fetch(`${java}/?historia=${historias[0].id}`, {headers:{Cookie:cookie}});
assert.ok((await historia.text()).includes('EXPEDIENTE DEL PACIENTE'));

const paginaReact = await fetch(react);
assert.equal(paginaReact.status,200);
const htmlReact = await paginaReact.text();
assert.ok(htmlReact.includes('<div id="root"></div>'));
assert.equal((await fetch(`${react}/vendor/flatly.min.css`)).status,200);
for (const asset of htmlReact.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g)) {
  assert.equal((await fetch(`${react}${asset[1]}`)).status,200, asset[1]);
}
const historiasProxy = await json(`${react}/api/historias`);
assert.equal(historiasProxy.length,historias.length);
console.log('OK: API autenticada, expediente, sesión/CSRF Java, cuatro catálogos Thymeleaf, archivos React y proxy REST.');
