import { useEffect, useState } from "react";
import { api } from "./api.js";

/**
 * GET con cancelación. Cambiar `version` vuelve a pedir el recurso.
 * Con `conservar: true`, mientras se vuelve a pedir la misma ruta se siguen mostrando los datos
 * anteriores (con `actualizando: true`) en vez de vaciar la pantalla: lo usa el refresco automático.
 */
export default function useApiResource(path, version = 0, { conservar = false } = {}) {
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    api.get(path, { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted)
          setResultado({ path, version, data, recibido: new Date() });
      },
      (error) => {
        if (!controller.signal.aborted) setResultado({ path, version, error });
      },
    );
    return () => controller.abort();
  }, [path, version]);

  if (resultado?.path === path && resultado.version === version)
    return resultado;
  if (conservar && resultado?.path === path && resultado.data)
    return { ...resultado, actualizando: true };
  return { cargando: true };
}
