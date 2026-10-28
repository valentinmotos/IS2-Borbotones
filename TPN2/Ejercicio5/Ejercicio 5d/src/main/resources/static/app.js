'use strict';
// El JWT vive solo en memoria: no se persiste en cookies, localStorage ni sessionStorage.
let token = null, user = null, pets = [], registering = false, expiryTimer, expiresAt = 0;
const urls = new Set();
const $ = id => document.getElementById(id);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function notice(message, success = false) {
  $('notice').textContent = message; $('notice').className = success ? 'success' : ''; $('notice').hidden = false;
}
async function api(path, {method='GET', data, blob=false} = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (data && !(data instanceof FormData)) headers['Content-Type'] = 'application/json';
  const response = await fetch(`/api${path}`, {method, headers, cache:'no-store',
    body:data ? (data instanceof FormData ? data : JSON.stringify(data)) : undefined});
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 401 && token) clearAuth();
    throw new Error(error.mensaje || `No se pudo completar la operación (${response.status})`);
  }
  if (response.status === 204) return null;
  return blob ? response.blob() : response.json();
}
function run(action) { return async event => {
  event?.preventDefault();
  const button = event?.submitter || (event?.currentTarget instanceof HTMLButtonElement ? event.currentTarget : null);
  if (button) button.disabled = true;
  try { await action(event); } catch (error) { notice(error.message); }
  finally { if (button) button.disabled = false; }
}; }
function navigation() {
  $('nav').replaceChildren();
  const actions = token ? [['Salir', logout]] : [['Ingresar', () => showAuth(false)], ['Registrarme', () => showAuth(true)]];
  for (const [label, action] of actions) {
    const button = document.createElement('button'); button.textContent = label; button.className = 'secondary';
    button.addEventListener('click', run(action)); $('nav').append(button);
  }
}
function showAuth(register) {
  registering = register; $('welcome').hidden = true; $('auth').hidden = false; $('dashboard').hidden = true;
  $('auth-form').reset(); $('auth-title').textContent = register ? 'Crear mi cuenta' : 'Iniciar sesión';
  $('auth-submit').textContent = register ? 'Registrarme' : 'Ingresar';
  document.querySelectorAll('.register-only').forEach(el => {
    el.hidden = !register; el.querySelectorAll('input,select').forEach(input => input.required = register);
  });
  $('auth-form').elements.clave.minLength = register ? 8 : 1;
  $('auth-form').elements.clave.autocomplete = register ? 'new-password' : 'current-password';
}
function clearAuth() {
  token = null; user = null; pets = []; clearTimeout(expiryTimer);
  urls.forEach(url => URL.revokeObjectURL(url)); urls.clear();
  $('pet-list').replaceChildren(); $('explore-list').replaceChildren(); $('vote-list').replaceChildren();
  $('profile-form').reset(); $('pet-form').reset(); $('profile-photo').replaceChildren();
  navigation(); showAuth(false);
}
async function logout() {
  await api('/auth/logout', {method:'POST'}); clearAuth(); notice('Cerraste sesión correctamente.', true);
}
async function enter(auth) {
  token = auth.accessToken; user = auth.usuario;
  expiresAt = Date.now() + auth.expiresIn * 1000;
  clearTimeout(expiryTimer);
  expiryTimer = setTimeout(() => { clearAuth(); notice('Tu sesión venció. Volvé a ingresar.'); }, auth.expiresIn * 1000);
  $('expires').textContent = `Sesión hasta las ${new Date(expiresAt).toLocaleTimeString('es-AR', {hour:'2-digit',minute:'2-digit'})}`;
  $('auth').hidden = true; $('welcome').hidden = true; $('dashboard').hidden = false; $('notice').hidden = true;
  navigation(); await refresh(); await showTab('pets');
}
function greeting() { $('greeting').textContent = `Hola, ${user.nombre}`; }
async function refresh() {
  [user, pets] = await Promise.all([api('/usuarios/me'), api('/mascotas')]);
  greeting();
  const form = $('profile-form');
  ['nombre','apellido','mail'].forEach(key => form.elements[key].value = user[key]);
  form.elements.idZona.value = user.zona.id;
  $('profile-photo').innerHTML = user.tieneFoto ? '<img class="pet-photo" alt="Mi foto" data-photo="/usuarios/me/foto">' : '';
  const selected = $('explore-pet').value;
  $('explore-pet').innerHTML = pets.map(p => `<option value="${escapeHtml(p.id)}">${escapeHtml(p.nombre)}</option>`).join('');
  if (pets.some(p => p.id === selected)) $('explore-pet').value = selected;
  renderPets(); await photos($('profile-photo'));
}
function petCard(p) {
  return `${p.tieneFoto ? `<img class="pet-photo" alt="Foto de ${escapeHtml(p.nombre)}" data-photo="/mascotas/${escapeHtml(p.id)}/foto">` : '<div class="placeholder" aria-label="Sin foto">🐾</div>'}
    <h3>${escapeHtml(p.nombre)}</h3><p>${escapeHtml(p.tipo)} · ${escapeHtml(p.sexo)}</p><p class="muted">${escapeHtml(p.zona.nombre)} · ${escapeHtml(p.dueno)}</p>`;
}
async function photos(container) {
  await Promise.all([...container.querySelectorAll('[data-photo]')].map(async img => {
    try {
      const blob = await api(img.dataset.photo, {blob:true});
      if (!img.isConnected || !token) return;
      const url = URL.createObjectURL(blob); urls.add(url); img.src = url;
      img.onload = () => { URL.revokeObjectURL(url); urls.delete(url); };
    } catch (error) { img.alt = 'Foto no disponible'; }
  }));
}
function renderPets() {
  $('pet-list').innerHTML = pets.length ? pets.map(p => `<article class="card">${petCard(p)}<button data-edit="${escapeHtml(p.id)}" class="secondary">Editar</button><button data-delete="${escapeHtml(p.id)}" class="danger">Dar de baja</button></article>`).join('') : '<p class="empty">Todavía no tenés mascotas. Agregá la primera para empezar a explorar.</p>';
  $('pet-list').querySelectorAll('[data-edit]').forEach(button => button.onclick = () => {
    const p = pets.find(p => p.id === button.dataset.edit), form = $('pet-form');
    form.reset(); ['id','nombre','sexo','tipo'].forEach(k => form.elements[k].value = p[k]);
    $('pet-title').textContent = `Editar a ${p.nombre}`; form.scrollIntoView({behavior:'smooth'});
  });
  $('pet-list').querySelectorAll('[data-delete]').forEach(button => button.onclick = run(async () => {
    const p = pets.find(p => p.id === button.dataset.delete);
    if (!confirm(`¿Dar de baja a ${p.nombre}? Dejará de aparecer en exploración y votos.`)) return;
    await api(`/mascotas/${p.id}`, {method:'DELETE'}); resetPet(); await refresh(); notice('Mascota dada de baja.', true);
  }));
  void photos($('pet-list'));
}
function resetPet() { $('pet-form').reset(); $('pet-form').elements.id.value = ''; $('pet-title').textContent = 'Agregar mascota'; }
async function upload(path, file) {
  if (!file || !file.size) return;
  if (file.size > 2*1024*1024) throw new Error('La foto debe pesar como máximo 2 MB.');
  const data = new FormData(); data.append('archivo', file); await api(path, {method:'PUT', data});
}
async function explore() {
  const id = $('explore-pet').value;
  if (!id) { $('explore-list').innerHTML = '<p class="empty">Agregá una mascota para explorar.</p>'; return; }
  const results = await api(`/mascotas/explorar?idMascotaPropia=${encodeURIComponent(id)}`);
  $('explore-list').innerHTML = results.length ? results.map(p => `<article class="card">${petCard(p)}<button data-vote="${escapeHtml(p.id)}">♡ Me gusta</button></article>`).join('') : '<p class="empty">No hay nuevas mascotas compatibles en tu zona. Volvé más tarde.</p>';
  $('explore-list').querySelectorAll('[data-vote]').forEach(button => button.onclick = run(async () => {
    const vote = await api('/votos', {method:'POST', data:{idMascotaPropia:id, idMascotaVotada:button.dataset.vote}});
    await explore(); notice(vote.match ? '¡Es un match!' : '¡Voto enviado!', true);
  }));
  await photos($('explore-list'));
}
async function votes() {
  const results = await api('/votos');
  $('vote-list').innerHTML = results.length ? results.map(v => `<article class="card"><p class="eyebrow">${v.match ? '♥ MATCH' : v.recibido ? 'VOTO RECIBIDO' : 'VOTO ENVIADO'}</p>
    <h3>${escapeHtml(v.origen.nombre)} → ${escapeHtml(v.destino.nombre)}</h3><p class="muted">${new Date(v.fecha).toLocaleDateString('es-AR')}</p>
    ${v.recibido && !v.match ? `<button data-accept="${escapeHtml(v.id)}">Aceptar y hacer match</button>` : `<p>${v.match ? '¡Las dos mascotas se gustan!' : 'Esperando respuesta…'}</p>`}</article>`).join('') : '<p class="empty">Aún no hay votos. Explorá y enviá el primero.</p>';
  $('vote-list').querySelectorAll('[data-accept]').forEach(button => button.onclick = run(async () => {
    await api(`/votos/${button.dataset.accept}/responder`, {method:'POST'}); await votes(); notice('¡Hicieron match!', true);
  }));
}
async function showTab(id) {
  document.querySelectorAll('.tab').forEach(el => el.hidden = el.id !== id);
  document.querySelectorAll('[data-tab]').forEach(el => el.setAttribute('aria-current', el.dataset.tab === id));
  if (id === 'explore') await explore();
  if (id === 'votes') await votes();
}
$('auth-form').onsubmit = run(async () => {
  const data = Object.fromEntries(new FormData($('auth-form')));
  if (registering && data.clave !== data.clave2) throw new Error('Las contraseñas deben coincidir.');
  const auth = await api(`/auth/${registering ? 'registro' : 'login'}`, {method:'POST', data});
  $('auth-form').reset(); await enter(auth);
});
$('pet-form').onsubmit = run(async () => {
  const form = $('pet-form'), values = Object.fromEntries(new FormData(form)), id = values.id;
  const pet = await api(`/mascotas${id ? '/'+id : ''}`, {method:id ? 'PUT' : 'POST', data:{nombre:values.nombre, sexo:values.sexo, tipo:values.tipo}});
  // Guardar el ID antes de subir evita duplicar la mascota si la foto falla y se reintenta.
  form.elements.id.value = pet.id;
  try { await upload(`/mascotas/${pet.id}/foto`, values.archivo); }
  catch (error) { await refresh(); throw new Error(`La mascota se guardó. ${error.message}`); }
  resetPet(); await refresh(); notice('Mascota guardada.', true);
});
$('profile-form').onsubmit = run(async () => {
  const form = $('profile-form'), data = Object.fromEntries(new FormData(form));
  await api('/usuarios/me', {method:'PUT', data:{nombre:data.nombre, apellido:data.apellido, idZona:data.idZona}});
  await upload('/usuarios/me/foto', data.archivo); form.elements.archivo.value = ''; await refresh(); notice('Perfil actualizado.', true);
});
$('cancel-pet').onclick = resetPet;
$('start').onclick = () => showAuth(true);
$('explore-pet').onchange = run(explore);
document.querySelectorAll('[data-tab]').forEach(button => button.onclick = run(() => showTab(button.dataset.tab)));
navigation();
void api('/zonas').then(zones => document.querySelectorAll('.zones').forEach(select => {
  select.innerHTML = zones.map(z => `<option value="${escapeHtml(z.id)}">${escapeHtml(z.nombre)}</option>`).join('');
})).catch(error => notice(error.message));
