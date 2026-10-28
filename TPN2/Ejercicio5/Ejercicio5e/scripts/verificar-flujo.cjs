// Prueba HTTP del flujo real. Ejecutar contra instancias locales de prueba, no contra produccion.
// Requiere Node 22+ y los tres servicios iniciados. No imprime tokens ni contrasenas.
const assert = require('node:assert/strict');
const client = process.argv[2] || 'http://127.0.0.1:8080';
const issuer = process.argv[3] || 'http://127.0.0.1:9000';
const api = process.argv[4] || 'http://127.0.0.1:8000';
const allowed = new Set([client, issuer, api]);
const decode = s => s.replace(/&amp;/g,'&').replace(/&quot;/g,'"').replace(/&#39;/g,"'");
function hidden(html) {
  const data = new URLSearchParams();
  for (const tag of html.match(/<input\b[^>]*>/gi) || []) {
    const attrs = Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(m => [m[1],decode(m[2])]));
    if (attrs.type === 'hidden' && attrs.name) data.append(attrs.name,attrs.value || '');
  }
  return data;
}
class Session {
  cookies = new Map();
  async request(url, options = {}) {
    assert(allowed.has(new URL(url).origin),'Destino inesperado fuera de los tres servicios locales');
    const headers = {...options.headers};
    if (this.cookies.size) headers.Cookie = [...this.cookies].map(([k,v]) => `${k}=${v}`).join('; ');
    const response = await fetch(url,{...options,headers,redirect:'manual'});
    for (const value of response.headers.getSetCookie()) {
      const part=value.split(';')[0], i=part.indexOf('='); this.cookies.set(part.slice(0,i),part.slice(i+1));
    }
    return response;
  }
  async follow(url, options = {}) {
    for(let i=0;i<15;i++) {
      const response=await this.request(url,options);
      if (![301,302,303,307,308].includes(response.status)) return response;
      url=new URL(response.headers.get('location'),url).href;
      await response.arrayBuffer(); options={};
    }
    throw Error('Demasiadas redirecciones');
  }
  async login(username,password) {
    let response=await this.request(client+'/oauth2/authorization/local');
    const authorization=response.headers.get('location');
    assert(authorization.includes('code_challenge_method=S256'));
    response=await this.follow(authorization);
    assert.equal(response.status,200);
    const loginData=hidden(await response.text());
    assert(loginData.has('_csrf'),'El login debe incluir CSRF');
    loginData.set('username',username); loginData.set('password',password);
    response=await this.follow(issuer+'/login',{method:'POST',body:loginData});
    let html=await response.text();
    if (html.includes('name="scope"')) {
      const consent=hidden(html);
      for(const scope of ['profile','api.read','api.write']) consent.append('scope',scope);
      consent.set('client_id','ejercicio5-cliente');
      response=await this.follow(issuer+'/oauth2/authorize',{method:'POST',body:consent});
      html=await response.text();
    }
    assert.equal(response.status,200,'El callback debe completar el login');
    assert(html.includes('Tinder de Mascotas'),'Debe volver a la aplicacion');
    response=await this.request(client+'/sesion'); assert.equal(response.status,200,'El cliente debe quedar autenticado');
    this.csrf=await response.json();
    assert(!('accessToken' in this.csrf),'El navegador no recibe tokens OAuth');
  }
  async json(path,method='GET',body) {
    const headers={};
    if(method!=='GET') headers[this.csrf.csrfHeader]=this.csrf.csrfToken;
    if(body) headers['Content-Type']='application/json';
    const response=await this.request(client+path,{method,headers,body:body ? JSON.stringify(body) : undefined});
    assert(response.ok,`Fallo ${method} ${path}: ${response.status} ${response.ok ? '' : await response.text()}`);
    return response.status===204 ? null : response.json();
  }
}
(async () => {
  const anon=new Session();
  assert.equal((await anon.request(api+'/api/mascotas')).status,401);
  assert.equal((await anon.request(client+'/api/mascotas')).status,401);
  const a=new Session(), b=new Session();
  await a.login('usuario',process.env.DEMO_USER_PASSWORD || 'Usuario123!');
  await b.login('admin',process.env.DEMO_ADMIN_PASSWORD || 'Admin123!');
  const perfilA=await a.json('/api/usuarios/me'), perfilB=await b.json('/api/usuarios/me');
  assert.notEqual(perfilA.id,perfilB.id);
  const origen=await a.json('/api/mascotas','POST',{nombre:'Luna prueba OAuth',sexo:'HEMBRA',tipo:'PERRO'});
  const destino=await b.json('/api/mascotas','POST',{nombre:'Toby prueba OAuth',sexo:'MACHO',tipo:'PERRO'});
  const sinCsrf=await a.request(client+'/api/mascotas',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'});
  assert.equal(sinCsrf.status,403);
  const editarAjena=await b.request(client+'/api/mascotas/'+origen.id,{method:'PUT',headers:{[b.csrf.csrfHeader]:b.csrf.csrfToken,'Content-Type':'application/json'},body:JSON.stringify({nombre:'Ajena',sexo:'HEMBRA',tipo:'PERRO'})});
  assert.equal(editarAjena.status,403);
  const foto=new FormData();
  foto.append('archivo',new Blob([Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aYZkAAAAASUVORK5CYII=','base64')],{type:'image/png'}),'foto.png');
  const subida=await a.request(client+`/api/mascotas/${origen.id}/foto`,{method:'PUT',headers:{[a.csrf.csrfHeader]:a.csrf.csrfToken},body:foto});
  assert.equal(subida.status,204,`Foto: ${await subida.text()}`);
  const imagen=await b.request(client+`/api/mascotas/${origen.id}/foto`); assert.equal(imagen.status,200); assert.equal(imagen.headers.get('content-type'),'image/png');
  const opciones=await a.json(`/api/mascotas/explorar?idMascotaPropia=${origen.id}`); assert(opciones.some(p => p.id===destino.id));
  const voto=await a.json('/api/votos','POST',{idMascotaPropia:origen.id,idMascotaVotada:destino.id});
  const match=await b.json(`/api/votos/${voto.id}/responder`,'POST'); assert.equal(match.match,true);
  await a.json('/api/mascotas/'+origen.id,'DELETE'); await b.json('/api/mascotas/'+destino.id,'DELETE');
  const logout=new URLSearchParams({[a.csrf.csrfParameter]:a.csrf.csrfToken});
  const salida=await a.follow(client+'/logout',{method:'POST',body:logout}); assert.equal(salida.status,200);
  assert.equal((await a.request(client+'/sesion')).status,401);
  console.log('OK: Authorization Code + PKCE, consentimiento, login OIDC, sesion sin tokens visibles, CSRF, propiedad, fotos, votos, match y logout.');
})().catch(error => { console.error(error.message); process.exitCode=1; });
