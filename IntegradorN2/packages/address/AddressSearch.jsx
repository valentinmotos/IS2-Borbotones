import { useEffect, useRef, useState } from "react";

export default function AddressSearch({ api, query, onPick }) {
  const request = useRef(null);
  const [pending, setPending] = useState(false);
  const [results, setResults] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => () => request.current?.abort(), []);

  async function search() {
    if (request.current) return;
    const current = new AbortController();
    request.current = current;
    setPending(true);
    setResults(null);
    setError("");
    try {
      const { data } = await api.get("/geocodificacion", {
        params: { q: query },
        signal: current.signal,
      });
      if (!current.signal.aborted) setResults(data);
    } catch (failure) {
      if (!current.signal.aborted) setError(failure.message);
    } finally {
      if (!current.signal.aborted) {
        request.current = null;
        setPending(false);
      }
    }
  }

  return (
    <div className="dm-address-search">
      <button
        type="button"
        className="dm-button"
        disabled={!query || pending}
        onClick={search}
      >
        {pending ? "Buscando…" : "Buscar dirección en el mapa"}
      </button>
      {error && (
        <p className="dm-error" role="alert">
          {error}
        </p>
      )}
      {results?.length === 0 && (
        <p role="status">
          No encontramos coincidencias. Podés marcar el punto en el mapa.
        </p>
      )}
      {results?.length > 0 && (
        <ul aria-label="Resultados de la búsqueda">
          {results.map(({ etiqueta, latitud, longitud }, index) => (
            <li key={index}>
              <button
                type="button"
                className="dm-button"
                onClick={() => {
                  onPick({ latitud, longitud });
                  setResults(null);
                }}
              >
                {etiqueta}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
