import { useEffect, useRef, useState } from "react";
import markerUrl from "leaflet/dist/images/marker-icon.png";
import markerRetinaUrl from "leaflet/dist/images/marker-icon-2x.png";
import markerShadowUrl from "leaflet/dist/images/marker-shadow.png";
import "leaflet/dist/leaflet.css";

export default function BranchesMap({ branches }) {
  const container = useRef(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    let map;

    import("leaflet")
      .then(({ default: L }) => {
        if (!active) return;
        map = L.map(container.current, { scrollWheelZoom: false });
        L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
          maxZoom: 19,
          attribution:
            '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        })
          .on("tileerror", () => {
            if (active)
              setMessage("Hay sectores del mapa que no pudieron cargar.");
          })
          .addTo(map);

        const icon = L.icon({
          iconUrl: markerUrl,
          iconRetinaUrl: markerRetinaUrl,
          shadowUrl: markerShadowUrl,
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [0, -34],
          shadowSize: [41, 41],
        });
        const markers = L.featureGroup().addTo(map);
        for (const { latitud, longitud, nombre } of branches) {
          const popup = document.createElement("strong");
          popup.textContent = nombre;
          L.marker([latitud, longitud], { icon, alt: nombre, title: nombre })
            .bindPopup(popup)
            .addTo(markers);
        }
        map.fitBounds(markers.getBounds(), { padding: [32, 32], maxZoom: 15 });
      })
      .catch(() => {
        if (active) setMessage("No pudimos cargar el mapa.");
      });

    return () => {
      active = false;
      map?.remove();
    };
  }, [branches]);

  return (
    <>
      <div
        ref={container}
        className="shop-branch-map"
        role="region"
        aria-label="Mapa de sucursales"
      />
      {message && <p role="status">{message}</p>}
    </>
  );
}
