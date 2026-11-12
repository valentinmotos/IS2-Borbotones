import { useEffect, useState } from "react";
import { api } from "./api.js";

export default function useApiResource(path, version = 0) {
  const [resultado, setResultado] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    api.get(path, { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted) setResultado({ path, version, data });
      },
      (error) => {
        if (!controller.signal.aborted) setResultado({ path, version, error });
      },
    );
    return () => controller.abort();
  }, [path, version]);

  return resultado?.path === path && resultado.version === version
    ? resultado
    : { cargando: true };
}
