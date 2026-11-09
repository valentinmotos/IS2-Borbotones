import { useEffect, useState } from "react";
import { api } from "../../lib/api.js";

export function useProductoImagen(id) {
  const [imagen, setImagen] = useState(null);

  useEffect(() => {
    if (!id) return;
    const solicitud = new AbortController();
    let url;
    api
      .get(`/imagenes/${encodeURIComponent(id)}`, {
        responseType: "blob",
        signal: solicitud.signal,
      })
      .then(
        ({ data }) => {
          if (!solicitud.signal.aborted) {
            url = URL.createObjectURL(data);
            setImagen({ id, url });
          }
        },
        () => {
          if (!solicitud.signal.aborted) setImagen({ id, error: true });
        },
      );

    return () => {
      solicitud.abort();
      if (url) URL.revokeObjectURL(url);
    };
  }, [id]);

  return id && imagen?.id === id ? imagen : null;
}
