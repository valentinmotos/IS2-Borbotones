import { useEffect, useRef, useState } from "react";
import "leaflet/dist/leaflet.css";

const MENDOZA = [-32.8895, -68.8458];
const TILES = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const CREDIT =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

export default function AddressMap({ latitud, longitud, onPick }) {
  const container = useRef(null);
  const [map, setMap] = useState(null);
  const [error, setError] = useState("");
  const hasPoint =
    Number.isFinite(latitud) &&
    Number.isFinite(longitud) &&
    Math.abs(latitud) <= 90 &&
    Math.abs(longitud) <= 180;
  const initialView = useRef(
    hasPoint
      ? { center: [latitud, longitud], zoom: 16 }
      : { center: MENDOZA, zoom: 12 },
  );

  useEffect(() => {
    let active = true;
    let instance;
    let observer;
    import("leaflet")
      .then(({ default: L }) => {
        if (!active) return;
        const { center, zoom } = initialView.current;
        instance = L.map(container.current, { scrollWheelZoom: false }).setView(
          center,
          zoom,
        );
        L.tileLayer(TILES, { attribution: CREDIT, maxZoom: 19 }).addTo(
          instance,
        );
        const marker = L.circleMarker(MENDOZA, {
          radius: 9,
          color: "#fff",
          weight: 2,
          fillColor: "#9b361e",
          fillOpacity: 1,
          interactive: false,
        });
        observer = new ResizeObserver(() =>
          instance.invalidateSize({ pan: false }),
        );
        observer.observe(container.current);
        setMap({ instance, marker });
      })
      .catch(() => {
        if (active) setError("No pudimos cargar el mapa.");
      });
    return () => {
      active = false;
      observer?.disconnect();
      instance?.remove();
    };
  }, []);

  useEffect(() => {
    if (!map) return;
    const { instance } = map;
    const pick = ({ latlng }) => {
      const point = latlng.wrap();
      onPick({
        latitud: Number(point.lat.toFixed(6)),
        longitud: Number(point.lng.toFixed(6)),
      });
    };
    instance.on("click", pick);
    return () => instance.off("click", pick);
  }, [map, onPick]);

  useEffect(() => {
    if (!map) return;
    const { instance, marker } = map;
    if (!hasPoint) {
      marker.remove();
      return;
    }
    const point = [latitud, longitud];
    marker.setLatLng(point).addTo(instance);
    instance.setView(point, Math.max(instance.getZoom(), 16), {
      animate: false,
    });
  }, [map, hasPoint, latitud, longitud]);

  return (
    <>
      <div
        className="dm-address-map"
        ref={container}
        role="region"
        aria-label="Mapa de la dirección"
      />
      <p className="dm-address-hint" role="status">
        {error ||
          (hasPoint
            ? `Punto seleccionado: ${latitud}, ${longitud}`
            : "Todavía no se seleccionó un punto.")}
      </p>
    </>
  );
}
