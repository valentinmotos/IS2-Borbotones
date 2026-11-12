import { useState } from "react";
import BranchesMap from "./BranchesMap.jsx";
import { usePublicData } from "../usePublicData.js";

const apiUrl = (import.meta.env.VITE_API_URL || "/api").replace(/\/+$/, "");

function BranchPhoto({ branch }) {
  const [failed, setFailed] = useState(false);
  if (failed || !branch.idImagen) return null;

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
    return <p role="status">Cargando sucursales…</p>;
  if (branches.status === "error")
    return <p role="status">No pudimos consultar las sucursales.</p>;
  if (!branches.data.length) return <p>No hay sucursales publicadas.</p>;

  return (
    <div className="shop-branches">
      {withMap && <BranchesMap branches={branches.data} />}
      <ul className="shop-branch-list">
        {branches.data.map((branch) => {
          const {
            id,
            nombre,
            calle,
            numeracion,
            localidad,
            latitud,
            longitud,
          } = branch;
          return (
            <li key={id} className="shop-branch-card">
              {withMap && <BranchPhoto branch={branch} />}
              <div className="shop-branch-details">
                <h3>{nombre}</h3>
                <address>
                  {calle} {numeracion}, {localidad}
                </address>
                <a
                  href={`https://www.openstreetmap.org/?mlat=${latitud}&mlon=${longitud}#map=16/${latitud}/${longitud}`}
                  aria-label={`Ver la ubicación de ${nombre} en OpenStreetMap`}
                >
                  Ver ubicación
                </a>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
