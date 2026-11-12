import { useState } from "react";
import { ExternalLink, MapPin, Store } from "lucide-react";
import BranchesMap from "./BranchesMap.jsx";
import { usePublicData } from "../usePublicData.js";
import "../styles.css";

const apiUrl = (import.meta.env.VITE_API_URL || "/api").replace(/\/+$/, "");

function BranchPhoto({ branch }) {
  const [failed, setFailed] = useState(false);
  if (failed || !branch.idImagen)
    return (
      <span className="shop-branch-photo is-empty" aria-hidden="true">
        <Store size={28} />
      </span>
    );

  return (
    <img
      className="shop-branch-photo"
      src={`${apiUrl}/imagenes/${encodeURIComponent(branch.idImagen)}`}
      alt={`Foto de ${branch.nombre}`}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  );
}

export default function Branches({ withMap = false }) {
  const branches = usePublicData("/publico/sucursales");

  if (branches.status === "loading")
    return (
      <p className="shop-branches-status" role="status">
        Cargando sucursales…
      </p>
    );
  if (branches.status === "error")
    return (
      <p className="shop-branches-status" role="status">
        No pudimos consultar las sucursales.
      </p>
    );
  if (!branches.data.length)
    return (
      <p className="shop-branches-status">No hay sucursales publicadas.</p>
    );

  return (
    <div className={withMap ? "shop-branches has-map" : "shop-branches"}>
      {withMap && (
        <div className="shop-branch-map-wrap">
          <BranchesMap branches={branches.data} />
        </div>
      )}
      <ul className="shop-branch-list" role="list">
        {branches.data.map((branch) => {
          const {
            id,
            nombre,
            calle,
            numeracion,
            localidad,
            barrio,
            referencia,
            latitud,
            longitud,
          } = branch;
          return (
            <li key={id} className="shop-branch-card">
              <BranchPhoto branch={branch} />
              <div className="shop-branch-details">
                <h3>{nombre}</h3>
                <address>
                  <MapPin size={15} aria-hidden="true" />
                  <span>
                    {calle} {numeracion}, {localidad}
                    {barrio && <small>Barrio {barrio}</small>}
                  </span>
                </address>
                {referencia && <p className="shop-branch-ref">{referencia}</p>}
                <a
                  href={`https://www.openstreetmap.org/?mlat=${latitud}&mlon=${longitud}#map=16/${latitud}/${longitud}`}
                  aria-label={`Ver la ubicación de ${nombre} en OpenStreetMap`}
                >
                  Ver ubicación <ExternalLink size={13} aria-hidden="true" />
                </a>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
