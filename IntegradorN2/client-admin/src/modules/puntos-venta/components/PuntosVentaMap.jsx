import { useEffect, useRef, useState } from "react";
import "leaflet/dist/leaflet.css";

const MENDOZA = [-32.8895, -68.8458];

export default function PuntosVentaMap({ puntos }) {
  const ref = useRef(null);
  const [error, setError] = useState("");
  useEffect(() => {
    let activo = true;
    let mapa;
    let grupo;
    let observer;
    import("leaflet")
      .then(({ default: L }) => {
        if (!activo) return;
        mapa = L.map(ref.current, { scrollWheelZoom: false }).setView(
          MENDOZA,
          12,
        );
        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        }).addTo(mapa);
        grupo = L.featureGroup().addTo(mapa);
        puntos.forEach((punto) => {
          const marker = L.marker([punto.latitud, punto.longitud]).bindPopup(
            `<strong>${L.Util.escapeHTML(punto.nombreComercio)}</strong><br>${L.Util.escapeHTML(punto.calle)} ${L.Util.escapeHTML(punto.numeracion)}`,
          );
          grupo.addLayer(marker);
        });
        if (puntos.length)
          mapa.fitBounds(grupo.getBounds().pad(0.15), { maxZoom: 15 });
        observer = new ResizeObserver(() =>
          mapa?.invalidateSize({ pan: false }),
        );
        observer.observe(ref.current);
      })
      .catch(() => {
        if (activo) setError("No pudimos cargar el mapa.");
      });
    return () => {
      activo = false;
      observer?.disconnect();
      mapa?.remove();
    };
  }, [puntos]);
  return (
    <div className="pv-map-wrap">
      <div
        className="pv-map"
        ref={ref}
        role="region"
        aria-label="Mapa de puntos de venta de la zona"
      />
      {error && <p role="alert">{error}</p>}
    </div>
  );
}
