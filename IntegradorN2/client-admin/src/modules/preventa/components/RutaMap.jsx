import { useEffect, useRef, useState } from "react";
import "leaflet/dist/leaflet.css";

const MENDOZA = [-32.8895, -68.8458];

/** Contenido del popup armado con textContent: los datos del comercio nunca se interpretan como HTML. */
function popup(parada, estado) {
  const contenido = document.createElement("div");
  const nombre = document.createElement("strong");
  nombre.textContent = parada.nombreComercio;
  const direccion = document.createElement("div");
  direccion.textContent = `${parada.calle ?? ""} ${parada.numeracion ?? ""}`;
  const detalle = document.createElement("div");
  detalle.textContent = estado;
  contenido.append(nombre, direccion, detalle);
  return contenido;
}

/**
 * E5-01: mapa de la ruta del día. Muestra todas las paradas (las visitadas en gris), numera las
 * pendientes según el recorrido sugerido, resalta el próximo punto y dibuja el recorrido desde la
 * posición actual (o desde el último punto visitado) con una polilínea.
 *
 * NOTA (E5-01): el recorrido son tramos en línea recta entre puntos, no el camino por calle.
 * E5-02 puede reutilizarlo pasando su ruta con la misma forma (`paradas`, `siguiente`,
 * `recorridoSugerido`).
 *
 * NOTA (E5-02): `etiquetaHecha` permite que el portal del distribuidor diga "Entregado" en vez de
 * "Visitado"; por defecto se mantiene el texto del preventor.
 */
export default function RutaMap({
  ruta,
  posicion,
  origen,
  etiquetaHecha = "Visitado",
}) {
  const contenedor = useRef(null);
  const mapa = useRef(null);
  const [leaflet, setLeaflet] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let activo = true;
    let observer;
    import("leaflet")
      .then(({ default: L }) => {
        if (!activo) return;
        mapa.current = L.map(contenedor.current, {
          scrollWheelZoom: false,
        }).setView(MENDOZA, 12);
        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        }).addTo(mapa.current);
        observer = new ResizeObserver(() =>
          mapa.current?.invalidateSize({ pan: false }),
        );
        observer.observe(contenedor.current);
        setLeaflet(L);
      })
      .catch(() => {
        if (activo) setError("No pudimos cargar el mapa.");
      });
    return () => {
      activo = false;
      observer?.disconnect();
      mapa.current?.remove();
      mapa.current = null;
    };
  }, []);

  useEffect(() => {
    if (!leaflet || !mapa.current || !ruta) return undefined;
    const L = leaflet;
    const capa = L.featureGroup().addTo(mapa.current);
    const porId = new Map(ruta.paradas.map((parada) => [parada.id, parada]));
    const numero = new Map(
      ruta.recorridoSugerido.map((id, indice) => [id, indice + 1]),
    );

    ruta.paradas.forEach((parada) => {
      const esSiguiente = parada.id === ruta.siguiente?.id;
      const clase = parada.visitada
        ? "ruta-marcador ruta-marcador-visitado"
        : esSiguiente
          ? "ruta-marcador ruta-marcador-siguiente"
          : "ruta-marcador";
      const etiqueta = parada.visitada ? "✓" : (numero.get(parada.id) ?? "");
      L.marker([parada.latitud, parada.longitud], {
        icon: L.divIcon({
          className: clase,
          html: `<span>${etiqueta}</span>`,
          iconSize: esSiguiente ? [34, 34] : [28, 28],
        }),
        title: parada.nombreComercio,
        zIndexOffset: esSiguiente ? 1000 : 0,
      })
        .bindPopup(
          popup(
            parada,
            parada.visitada
              ? etiquetaHecha
              : esSiguiente
                ? "Próximo punto"
                : "Pendiente",
          ),
        )
        .addTo(capa);
    });

    if (posicion) {
      L.circleMarker([posicion.latitud, posicion.longitud], {
        radius: 8,
        className: "ruta-posicion",
      })
        .bindTooltip("Tu ubicación")
        .addTo(capa);
    }

    const tramo = ruta.recorridoSugerido
      .map((id) => porId.get(id))
      .filter(Boolean)
      .map((parada) => [parada.latitud, parada.longitud]);
    const inicio = posicion ?? origen;
    if (inicio) tramo.unshift([inicio.latitud, inicio.longitud]);
    if (tramo.length > 1) {
      L.polyline(tramo, { className: "ruta-linea", weight: 4 }).addTo(capa);
    }

    if (capa.getLayers().length) {
      mapa.current.fitBounds(capa.getBounds().pad(0.15), { maxZoom: 15 });
    }
    return () => capa.remove();
  }, [leaflet, ruta, posicion, origen, etiquetaHecha]);

  return (
    <div className="ruta-mapa-wrap">
      <div
        className="ruta-mapa"
        ref={contenedor}
        role="region"
        aria-label="Mapa de la ruta del día"
      />
      {error && <p role="alert">{error}</p>}
    </div>
  );
}
