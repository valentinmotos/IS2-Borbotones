import { useId } from "react";
import AddressMap from "./AddressMap.jsx";
import AddressSearch from "./AddressSearch.jsx";
import { useLocationOptions } from "./useLocationOptions.js";
import "./styles.css";

const fields = [
  { name: "calle", label: "Calle", maxLength: 160, required: true },
  { name: "numeracion", label: "Numeración", maxLength: 30, required: true },
  { name: "barrio", label: "Barrio", maxLength: 120 },
  { name: "manzanaPiso", label: "Manzana o piso", maxLength: 80 },
  { name: "casaDepartamento", label: "Casa o departamento", maxLength: 80 },
  { name: "referencia", label: "Referencia", maxLength: 500 },
];

function LocationSelect({
  id,
  name,
  label,
  source,
  value,
  onChange,
  disabled,
}) {
  const options = source.data ?? [];
  const unavailable = value && !options.some((item) => item.id === value);
  return (
    <div className="dm-field">
      <label htmlFor={id}>{label}</label>
      <select
        id={id}
        name={name}
        value={value}
        onChange={onChange}
        required
        disabled={disabled || source.loading || Boolean(source.error)}
      >
        <option value="">
          {source.loading ? "Cargando…" : `Seleccioná ${label.toLowerCase()}`}
        </option>
        {unavailable && (
          <option value={value} disabled>
            {source.loading ? "Cargando selección…" : "Selección no disponible"}
          </option>
        )}
        {options.map((option) => (
          <option key={option.id} value={option.id}>
            {option.nombre}
          </option>
        ))}
      </select>
      {source.error && (
        <>
          <small className="dm-error" role="alert">
            {source.error}
          </small>
          <button type="button" className="dm-button" onClick={source.retry}>
            Reintentar carga de {label.toLowerCase()}
          </button>
        </>
      )}
      {source.data?.length === 0 && <small>No hay opciones disponibles.</small>}
    </div>
  );
}

/** Campo controlado. Los identificadores y las coordenadas llegan del formulario anfitrión. */
export default function DireccionField({ api, value = {}, onChange }) {
  const id = useId();
  const { latitud, longitud } = value;
  const departamentoId = value.departamentoId ?? "";
  const localidadId = value.localidadId ?? "";
  const departamentos = useLocationOptions(api, "/departamentos");
  const localidades = useLocationOptions(
    api,
    departamentoId
      ? `/departamentos/${encodeURIComponent(departamentoId)}/localidades`
      : null,
  );
  const localidad = localidades.data?.find((item) => item.id === localidadId);
  const calle = value.calle?.trim();
  const numeracion = value.numeracion?.trim();
  const query =
    calle && numeracion && localidad
      ? [
          `${calle} ${numeracion}`,
          localidad.nombre,
          "Mendoza",
          "Argentina",
        ].join(", ")
      : "";
  const pick = (point) => onChange({ ...value, ...point });

  return (
    <fieldset className="dm-address">
      <legend>Dirección</legend>
      <div className="dm-address-grid">
        <LocationSelect
          id={`${id}-departamento`}
          name="departamentoId"
          label="Departamento"
          source={departamentos}
          value={departamentoId}
          onChange={(event) =>
            onChange({
              ...value,
              departamentoId: event.target.value,
              localidadId: "",
              latitud: null,
              longitud: null,
            })
          }
        />
        <LocationSelect
          id={`${id}-localidad`}
          name="localidadId"
          label="Localidad"
          source={localidades}
          value={localidadId}
          disabled={!departamentoId}
          onChange={(event) =>
            onChange({
              ...value,
              localidadId: event.target.value,
              latitud: null,
              longitud: null,
            })
          }
        />
        <div className="dm-field">
          <label htmlFor={`${id}-postal`}>Código postal</label>
          <input
            id={`${id}-postal`}
            name="codigoPostal"
            value={localidad?.codigoPostal ?? ""}
            readOnly
          />
        </div>
        {fields.map(({ name, label, ...props }) => (
          <div key={name} className="dm-field">
            <label htmlFor={`${id}-${name}`}>{label}</label>
            <input
              id={`${id}-${name}`}
              name={name}
              value={value[name] ?? ""}
              {...props}
              onChange={(event) =>
                onChange({ ...value, [name]: event.target.value })
              }
            />
          </div>
        ))}
      </div>
      <p className="dm-address-hint">
        Hacé clic en el mapa para marcar un punto o buscá por calle, numeración
        y localidad.
      </p>
      <AddressSearch key={query} api={api} query={query} onPick={pick} />
      <AddressMap latitud={latitud} longitud={longitud} onPick={pick} />
    </fieldset>
  );
}
