export async function request(path, options = {}, auth) {
  const token = btoa(String.fromCharCode(...new TextEncoder().encode(`${auth.usuario}:${auth.clave}`)));
  const isForm = options.body instanceof FormData;
  const res = await fetch(`/api${path}`, {
    ...options, credentials: 'omit',
    headers: { Authorization: `Basic ${token}`, ...(options.body && !isForm ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
    body: options.body && !isForm ? JSON.stringify(options.body) : options.body
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(res.status === 401 ? 'Credenciales inválidas. Volvé a ingresar.' : error.mensaje || 'No se pudo completar la operación');
  }
  if (res.status === 204) return null;
  if (options.image) return res.blob();
  return res.json();
}
