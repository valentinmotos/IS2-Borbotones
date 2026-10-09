"use strict";

// HTML del original + History API. Los datos pasan siempre por el cliente REST de api.js.
let rendering = 0;
let pendingPopstate = false;
const paths = {
  "/": "home", "/login": "login", "/registro": "register", "/inicio": "start",
  "/usuario/editar-perfil": "profile", "/mascota/mis-mascotas": "pets",
  "/mascota/editar-perfil": "pet", "/mascota/explorar-mascotas": "explore",
  "/mascota/mascotas-de-baja": "inactive", "/votos/recibidos": "votes", "/matches": "matches"
};
const publicViews = new Set(["home", "login", "register"]);

function notify(text, error = false) {
  $("message").textContent = text;
  $("message").className = "alert mt-3 " + (error ? "alert-danger" : "alert-success");
  $("message").hidden = false;
}
async function run(callback) {
  if (busy) return;
  busy = true; document.body.setAttribute("aria-busy", "true");
  try { await callback(); }
  catch (error) { notify(error.message || "No se pudo completar la operación.", true); }
  finally {
    busy = false; document.body.removeAttribute("aria-busy");
    if (pendingPopstate) { pendingPopstate = false; await run(() => navigate(location.pathname + location.search, true)); }
  }
}
function renderLog() {
  $("request-log").replaceChildren();
  if (!state.logs.length) $("request-log").append(node("p", "text-muted", "Las peticiones aparecerán acá."));
  for (const entry of state.logs) {
    const details = node("details", "request-entry"); const summary = node("summary");
    summary.append(node("strong", "text-primary mr-2", entry.method), node("span", "", entry.path),
      node("span", "http-status " + (entry.status >= 400 || !entry.status ? "text-danger" : "text-success"),
        (entry.status || "Sin conexión") + " · " + entry.ms + " ms"));
    const grid = node("div", "row mt-3");
    for (const [title, content] of [["Petición", { headers: entry.headers, body: entry.request }], ["Respuesta", entry.response]]) {
      const column = node("div", "col-md-6");
      column.append(node("strong", "small", title), node("pre", "request-json", JSON.stringify(content, null, 2))); grid.append(column);
    }
    details.append(summary, grid); $("request-log").append(details);
  }
}
function action(text, style, callback) {
  const button = node("button", "btn " + style, text); button.type = "button";
  button.addEventListener("click", () => run(callback)); return button;
}
function link(text, href, style = "") { const anchor = node("a", style, text); anchor.href = href; return anchor; }
function clearPhotos() { state.photos.forEach((url) => URL.revokeObjectURL(url)); state.photos.clear(); }
async function picture(entity, img) {
  img.src = "/img/m1.jpeg"; img.alt = "Foto de " + (entity.nombre || "perfil");
  if (!entity.fotoId) return;
  const generation = rendering;
  try {
    const blob = await api("/fotos/" + encodeURIComponent(entity.fotoId), { image: true });
    if (generation !== rendering || !img.isConnected) return;
    const url = URL.createObjectURL(blob); state.photos.add(url); img.src = url;
  } catch { if (img.isConnected) img.title = "No se pudo cargar la foto; se muestra la imagen original por defecto."; }
}
function updateNavigation() {
  $("navigation").replaceChildren();
  const items = state.user ? [["Inicio", "/inicio"], ["Perfil", "/usuario/editar-perfil"], ["Mis Mascotas", "/mascota/mis-mascotas"],
    ["Explorar", "/mascota/explorar-mascotas"], ["Votos", "/votos/recibidos"], ["Matches", "/matches"], ["Salir", "/logout"]]
    : [["Registro", "/registro"], ["Log In", "/login"]];
  for (const [text, href] of items) {
    const item = node("li", "nav-item"); item.append(link(text, href, "nav-link")); $("navigation").append(item);
  }
  $("identity").textContent = state.user ? "Usuario: " + state.user.nombre + " " + state.user.apellido + " · " + state.user.mail : "Sin sesión iniciada";
  $("switch-ana").disabled = !state.demo; $("switch-juan").disabled = !state.demo;
}
async function navigate(href, replace = false) {
  const url = new URL(href, location.origin);
  if (url.pathname === "/logout") {
    state.user = null; state.auth = null; state.pets = []; state.pendingUpload = null; state.afterLogin = null;
    $("upload-retry").hidden = true; await navigate("/login", true); notify("Ha salido correctamente de la plataforma"); return;
  }
  const view = paths[url.pathname];
  if (!view) throw new Error("La pantalla solicitada no existe.");
  if (!publicViews.has(view) && !state.auth) {
    state.afterLogin = url.pathname + url.search;
    history.replaceState(null, "", "/login"); await render("login", new URLSearchParams());
    notify("Iniciá sesión para acceder a esa pantalla."); return;
  }
  history[replace ? "replaceState" : "pushState"](null, "", url.pathname + url.search);
  $("message").hidden = true; await render(view, url.searchParams);
}
async function loadZones(select, selected) {
  const zones = await api("/zonas", { auth: null });
  select.replaceChildren(new Option(zones.length ? "Seleccioná una zona" : "Agregá una zona en la sección de demostración", ""));
  zones.forEach((zone) => select.add(new Option(zone.nombre, zone.id)));
  if (selected) select.value = selected;
}
async function login(mail, clave, target) {
  state.user = await api("/auth/login", { method: "POST", body: { mail, clave }, auth: null });
  state.auth = credentials(mail, clave); state.pendingUpload = null; $("upload-retry").hidden = true;
  const next = target || state.afterLogin || "/inicio"; state.afterLogin = null;
  await navigate(next, true); notify("¡Bienvenido, " + state.user.nombre + "!");
}
function validatePhoto(file) {
  if (!file?.size) return null;
  if (file.size > 5 * 1024 * 1024) throw new Error("La foto no puede superar 5 MB.");
  if (!["image/jpeg", "image/png", "image/gif", "image/webp"].includes(file.type)) throw new Error("La foto debe ser JPEG, PNG, GIF o WebP.");
  return file;
}
async function uploadPhoto(path, file, savedMessage) {
  state.pendingUpload = { path, file, savedMessage, owner: state.user.id }; return retryPhoto();
}
async function retryPhoto() {
  const pending = state.pendingUpload;
  if (!pending || pending.owner !== state.user?.id) return true;
  const body = new FormData(); body.append("archivo", pending.file);
  try {
    await api(pending.path, { method: "PUT", body }); state.pendingUpload = null; $("upload-retry").hidden = true; return true;
  } catch (error) {
    $("upload-detail").textContent = pending.savedMessage + ". La foto no se pudo guardar: " + error.message;
    $("upload-retry").hidden = false; return false;
  }
}
function bindForm(id, handler) {
  const form = $(id);
  form.addEventListener("submit", (event) => {
    event.preventDefault(); if (busy || !form.reportValidity()) return;
    const data = Object.fromEntries(new FormData(form)); void run(() => handler(data, form));
  });
}
function fillForm(form, entity) {
  for (const [key, value] of Object.entries(entity)) {
    const input = form.elements.namedItem(key);
    if (input && input.type !== "file" && input.type !== "password") input.value = value ?? "";
  }
}
function tableWrapper(table) { const wrapper = node("div", "table-responsive"); table.replaceWith(wrapper); wrapper.append(table); }
function cell(row, text) { const td = node("td", "", text); row.append(td); return td; }
function thumbnail(entity) { const img = node("img", "pet-thumbnail"); void picture(entity, img); return img; }
function noData(parent, text) { parent.append(node("div", "alert alert-warning text-center mt-4", text)); }

async function userForm(register) {
  const form = $(register ? "register-form" : "profile-form");
  if (!register) {
    state.user = await api("/usuarios/me"); fillForm(form, state.user); void picture(state.user, $("user-picture"));
  }
  await loadZones($("user-zone"), register ? null : state.user.zonaId);
  bindForm(form.id, async (data) => {
    const { archivo, id, ...body } = data; const file = validatePhoto(archivo);
    const user = await api(register ? "/usuarios" : "/usuarios/me", { method: register ? "POST" : "PUT", body, auth: register ? null : state.auth });
    state.user = user; state.auth = credentials(body.mail, body.clave); state.afterLogin = null;
    state.pendingUpload = null; $("upload-retry").hidden = true;
    const uploaded = file ? await uploadPhoto("/usuarios/me/foto", file, "El perfil quedó guardado") : true;
    await navigate("/inicio", true);
    notify(uploaded ? "El perfil se guardó correctamente." : "El perfil quedó guardado, pero falta subir la foto. Podés reintentar abajo.", !uploaded);
  });
}
async function ownPets(inactive) {
  state.pets = await api("/mascotas?incluirBajas=true");
  const parent = $("pets-table").parentElement; const toolbar = node("div", "mb-3 clearfix");
  toolbar.append(link(inactive ? "← Mis Mascotas" : "Mascotas dadas de baja", inactive ? "/mascota/mis-mascotas" : "/mascota/mascotas-de-baja", "btn btn-secondary rounded-pill"));
  parent.insertBefore(toolbar, $("pets-table")); tableWrapper($("pets-table"));
  const pets = state.pets.filter((pet) => inactive ? !!pet.baja : !pet.baja);
  for (const pet of pets) {
    const row = node("tr"); row.dataset.petId = pet.id;
    cell(row).append(thumbnail(pet)); cell(row, pet.nombre); cell(row, pet.sexo); cell(row, pet.tipo);
    const controls = cell(row);
    if (!inactive) controls.append(link("Editar", "/mascota/editar-perfil?id=" + pet.id + "&accion=Actualizar"), document.createTextNode(" - "));
    controls.append(link(inactive ? "Dar Alta" : "Eliminar", "/mascota/editar-perfil?id=" + pet.id + "&accion=" + (inactive ? "Alta" : "Eliminar")));
    $("pets-body").append(row);
  }
  if (!pets.length) noData(parent, inactive ? "No hay mascotas dadas de baja." : "Todavía no tenés mascotas. ¡Agregá una Mascota!");
}
async function petForm(params) {
  const id = params.get("id"); const mode = params.get("accion") || (id ? "Actualizar" : "Crear");
  if (!["Crear", "Actualizar", "Eliminar", "Alta"].includes(mode) || (mode !== "Crear" && !id)) throw new Error("Acción de mascota inválida.");
  const form = $("pet-form");
  for (const type of ["PERRO", "GATO", "CONEJO"]) form.elements.tipo.add(new Option(type, type));
  for (const sex of ["MACHO", "HEMBRA"]) form.elements.sexo.add(new Option(sex, sex));
  let pet = null;
  if (id) {
    const pets = await api("/mascotas?incluirBajas=true"); pet = pets.find((item) => item.id === id);
    if (!pet) throw new Error("La mascota no existe o pertenece a otro usuario."); fillForm(form, pet);
  }
  $("pet-heading").textContent = mode + " Mascota"; void picture(pet || { nombre: "mascota" }, $("pet-picture"));
  const readonly = mode === "Eliminar" || mode === "Alta";
  for (const key of ["nombre", "tipo", "sexo"]) form.elements[key].disabled = readonly;
  form.elements.archivo.hidden = readonly;
  if (readonly) form.elements.archivo.previousElementSibling.hidden = true;
  const back = link("Volver", mode === "Alta" ? "/mascota/mascotas-de-baja" : "/mascota/mis-mascotas", "btn btn-dark mr-2");
  const save = node("button", "btn " + (mode === "Eliminar" ? "btn-danger" : "btn-primary"),
    { Crear: "Crear Mascota", Actualizar: "Actualizar Mascota", Eliminar: "Eliminar Mascota", Alta: "Dar Alta" }[mode]);
  save.type = "submit"; $("pet-actions").append(back, save);
  bindForm("pet-form", async (data) => {
    if (readonly) {
      await api("/mascotas/" + id + (mode === "Alta" ? "/habilitar" : ""), { method: mode === "Alta" ? "PUT" : "DELETE" });
      await navigate("/mascota/mis-mascotas"); notify(mode === "Alta" ? "La mascota fue dada de alta." : "La mascota fue dada de baja."); return;
    }
    const file = validatePhoto(data.archivo); const body = { nombre: data.nombre, tipo: data.tipo, sexo: data.sexo };
    const saved = await api("/mascotas" + (id ? "/" + id : ""), { method: id ? "PUT" : "POST", body });
    const uploaded = file ? await uploadPhoto("/mascotas/" + saved.id + "/foto", file, "La mascota quedó guardada") : true;
    await navigate("/mascota/mis-mascotas");
    notify(uploaded ? "La mascota se guardó correctamente." : "La mascota quedó guardada, pero falta subir la foto. Podés reintentar abajo.", !uploaded);
  });
}
async function explore(params) {
  const parent = $("explore-content"); const pets = await api("/mascotas"); const originId = params.get("idMascotaPropia");
  if (!originId) {
    const box = node("div", "card mt-4 shadow"); const content = node("div", "card-body");
    content.append(node("h3", "card-title text-center mb-4", "😼 Selecciona tu mascota para explorar 😼")); const grid = node("div", "row");
    for (const pet of pets) {
      const column = node("div", "col-md-4 col-lg-3 mb-3"); const card = node("div", "card h-100 text-center");
      const img = node("img", "card-img-top explore-picture"); const body = node("div", "card-body");
      body.append(node("h5", "card-title", pet.nombre), node("p", "card-text", pet.sexo + " - " + pet.tipo),
        link("Explorar con " + pet.nombre, "/mascota/explorar-mascotas?idMascotaPropia=" + pet.id, "btn btn-primary btn-block"));
      card.append(img, body); column.append(card); grid.append(column); void picture(pet, img);
    }
    content.append(grid); box.append(content); parent.append(box);
    if (!pets.length) noData(content, "Primero agregá una mascota para explorar."); return;
  }
  const origin = pets.find((pet) => pet.id === originId);
  if (!origin) throw new Error("Elegí una mascota propia y activa para explorar.");
  const banner = node("div", "alert alert-info mt-4 d-flex align-items-center flex-wrap"); const img = node("img", "pet-thumbnail mr-3");
  banner.append(img, node("span", "mr-auto", "Explorando con: " + origin.nombre), link("🔄 Cambiar mascota", "/mascota/explorar-mascotas", "btn btn-sm btn-outline-primary"));
  parent.append(banner); void picture(origin, img);
  const candidates = await api("/mascotas/" + origin.id + "/candidatos");
  if (!candidates.length) { noData(parent, "😿 No hay mascotas disponibles en este momento. Vuelve más tarde para descubrir nuevas mascotas."); return; }
  const table = node("table", "table table-light table-hover mt-4"); const head = node("thead"); const headRow = node("tr", "bg-black text-white");
  ["Foto", "Nombre", "Sexo", "Tipo", "Dueño", "Acciones"].forEach((text) => headRow.append(node("th", "", text))); head.append(headRow); const body = node("tbody");
  for (const pet of candidates) {
    const row = node("tr"); cell(row).append(thumbnail(pet)); cell(row, pet.nombre); cell(row, pet.sexo); cell(row, pet.tipo); cell(row, pet.usuarioNombre + " " + pet.usuarioApellido);
    cell(row).append(action("❤️ Votar", "btn-success btn-sm", async () => {
      await api("/votos", { method: "POST", body: { mascota1Id: origin.id, mascota2Id: pet.id } });
      await navigate(location.pathname + location.search, true); notify("Voto enviado a " + pet.nombre + ". Su dueño puede aceptarlo en Votos recibidos.");
    })); body.append(row);
  }
  table.append(head, body); parent.append(table); tableWrapper(table);
}
async function votes(matches) {
  const parent = $("vote-content"); const votes = await api(matches ? "/matches" : "/votos/recibidos");
  if (!votes.length) { noData(parent, matches ? "Todavía no hay matches. Un voto aceptado conecta a las dos mascotas." : "Todavía no recibiste votos."); return; }
  const names = new Map();
  for (const id of new Set(votes.flatMap((vote) => [vote.mascota1Id, vote.mascota2Id]))) { const pet = await api("/mascotas/" + id); names.set(id, pet.nombre); }
  const table = node("table", "table table-light table-hover"); const head = node("thead"); const headRow = node("tr", "bg-black text-white");
  ["Mascotas", "Fecha del voto", "Estado", "Acciones"].forEach((text) => headRow.append(node("th", "", text))); head.append(headRow); const body = node("tbody");
  for (const vote of votes) {
    const row = node("tr"); cell(row, names.get(vote.mascota1Id) + (vote.match ? " ♥ " : " → ") + names.get(vote.mascota2Id));
    cell(row, new Date(vote.fecha).toLocaleDateString("es-AR"));
    cell(row).append(node("span", "badge " + (vote.match ? "badge-success" : "badge-secondary"), vote.match ? "¡Hay match!" : "Pendiente"));
    const controls = cell(row);
    if (!vote.match) controls.append(action("Aceptar y hacer match", "btn-success btn-sm", async () => {
      await api("/votos/" + vote.id + "/respuesta", { method: "PUT" }); await navigate("/matches"); notify("¡Hay match! Las dos mascotas están conectadas.");
    })); body.append(row);
  }
  table.append(head, body); parent.append(table); tableWrapper(table);
}
async function render(view, params) {
  rendering++; clearPhotos(); updateNavigation();
  document.title = "Tinder de Mascota - " + ({ home: "Inicio", start: "Inicio", login: "Login", register: "Registro", profile: "Editar Perfil", pets: "Mis Mascotas", inactive: "Mascotas de Baja", pet: "Mascota", explore: "Explorar", votes: "Votos recibidos", matches: "Matches" }[view]);
  if (view === "votes" || view === "matches") {
    $("page").innerHTML = '<header><h1 style="margin-top:80px;text-align:center" id="vote-heading"></h1></header><section class="container my-5" id="vote-content"></section>';
    $("vote-heading").textContent = view === "votes" ? "Votos recibidos" : "Matches";
  } else $("page").innerHTML = window.TINDER_VIEWS[view];
  if (view === "login") bindForm("login-form", (data) => login(data.mail, data.clave));
  else if (view === "register" || view === "profile") await userForm(view === "register");
  else if (view === "start") $("user-name").textContent = state.user.nombre + " " + state.user.apellido;
  else if (view === "pets" || view === "inactive") await ownPets(view === "inactive");
  else if (view === "pet") await petForm(params);
  else if (view === "explore") await explore(params);
  else if (view === "votes" || view === "matches") await votes(view === "matches");
}

document.addEventListener("click", (event) => {
  const anchor = event.target.closest("a[href]");
  if (!anchor || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || anchor.target === "_blank") return;
  const url = new URL(anchor.href, location.origin);
  if (url.origin !== location.origin || !(url.pathname in paths || url.pathname === "/logout")) return;
  event.preventDefault(); if (busy) return;
  document.querySelector(".navbar-collapse")?.classList.remove("show");
  document.querySelector(".navbar-toggler")?.setAttribute("aria-expanded", "false");
  void run(() => navigate(url.pathname + url.search));
});
window.addEventListener("popstate", () => {
  if (busy) { pendingPopstate = true; return; }
  void run(() => navigate(location.pathname + location.search, true));
});
bindForm("zone-form", async (body, form) => {
  const zone = await api("/zonas", { method: "POST", body, auth: null }); form.reset();
  if ($("user-zone")) await loadZones($("user-zone"), zone.id); notify("Zona agregada. Ya está disponible para registrarte.");
});
$("clear-log").addEventListener("click", () => { state.logs = []; renderLog(); });
$("retry-photo").addEventListener("click", () => run(async () => {
  if (await retryPhoto()) { await navigate(location.pathname + location.search, true); notify("Foto guardada correctamente."); }
}));
$("discard-photo").addEventListener("click", () => { state.pendingUpload = null; $("upload-retry").hidden = true; notify("El perfil sigue guardado. Podés subir otra foto desde su formulario."); });
$("demo").addEventListener("click", () => run(async () => {
  const suffix = crypto.randomUUID();
  const zone = await api("/zonas", { method: "POST", body: { nombre: "Zona demo " + suffix.slice(0, 8), descripcion: "Demostración de la exposición" }, auth: null });
  const demo = { ana: { mail: "ana." + suffix + "@demo.test", clave: "mascotas123" }, juan: { mail: "juan." + suffix + "@demo.test", clave: "mascotas123" } };
  for (const [key, name, pet] of [["ana", "Ana", { nombre: "Luna", sexo: "HEMBRA", tipo: "PERRO" }], ["juan", "Juan", { nombre: "Toby", sexo: "MACHO", tipo: "PERRO" }]]) {
    const account = demo[key];
    await api("/usuarios", { method: "POST", auth: null, body: { nombre: name, apellido: "Demo", ...account, clave2: account.clave, zonaId: zone.id } });
    await api("/mascotas", { method: "POST", body: pet, auth: credentials(account.mail, account.clave) });
  }
  state.demo = demo; $("demo-status").textContent = "Demostración lista: Ana tiene a Luna y Juan tiene a Toby. Usá los botones para cambiar de dueño.";
  await login(demo.ana.mail, demo.ana.clave, "/inicio"); notify("Demostración preparada. Abrí Explorar para votar a Toby.");
}));
for (const key of ["ana", "juan"]) $("switch-" + key).addEventListener("click", () => run(() => login(state.demo[key].mail, state.demo[key].clave, "/inicio")));
void run(() => navigate(location.pathname + location.search, true));
