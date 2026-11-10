import { useState } from "react";
import DireccionField from "../../../../packages/address/DireccionField.jsx";
import { useLocationOptions } from "../../../../packages/address/useLocationOptions.js";
import { api } from "../../lib/api.js";

export default function AddressDemo() {
  const [edited, setEdited] = useState(null);
  const example = useLocationOptions(api, "/localidades/codigo-postal/5500");
  const address = example.data && {
    departamentoId: example.data.departamentoId,
    localidadId: example.data.id,
    calle: "Chile",
    numeracion: "1050",
    barrio: "Barrio Cívico",
    referencia: "Escuela Agustín Álvarez",
    latitud: -32.8898066,
    longitud: -68.8465449,
  };

  return (
    <section className="dm-address-demo" aria-labelledby="demo-direccion">
      <h2 id="demo-direccion">Dirección con mapa</h2>
      <p>
        Probá una dirección de ejemplo. Los cambios de esta demostración duran
        hasta que salís de la página.
      </p>
      {example.loading && <p role="status">Cargando dirección de ejemplo…</p>}
      {example.error && (
        <p role="status">
          La localidad de ejemplo no está disponible. Podés completar una
          dirección desde cero.
        </p>
      )}
      {!example.loading && (
        <DireccionField
          api={api}
          value={edited ?? address ?? {}}
          onChange={setEdited}
        />
      )}
    </section>
  );
}
