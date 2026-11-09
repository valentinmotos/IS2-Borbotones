import { api } from "./api.js";

/**
 * Operaciones de un ABM sobre AbstractCrudController (E0-08). Los errores llegan como ApiError
 * con `message` (texto para el usuario), `status` y `campo`.
 * Uso: const categoriasApi = crearCrudApi("/categorias");
 */
export function crearCrudApi(recurso) {
  return {
    listar: (config) => api.get(recurso, config).then((r) => r.data),
    buscar: (id) => api.get(`${recurso}/${id}`).then((r) => r.data),
    crear: (datos) => api.post(recurso, datos).then((r) => r.data),
    modificar: (id, datos) =>
      api.put(`${recurso}/${id}`, datos).then((r) => r.data),
    eliminar: (id) => api.delete(`${recurso}/${id}`),
  };
}
