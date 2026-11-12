import { useState } from "react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import DireccionField from "../../../../../packages/address/DireccionField.jsx";
import { api } from "../../../lib/api.js";
import "../styles.css";

const vacio = {
  nombreComercio: "",
  minoristaId: "",
  departamentoId: "",
  localidadId: "",
  calle: "",
  numeracion: "",
  barrio: "",
  manzanaPiso: "",
  casaDepartamento: "",
  referencia: "",
  latitud: null,
  longitud: null,
};

export default function PuntoVentaForm({
  minoristas,
  minoristaId = "",
  onCreado,
}) {
  const [form, setForm] = useState({ ...vacio, minoristaId });
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    if (form.latitud == null || form.longitud == null) {
      setError("Marcá la ubicación del comercio en el mapa.");
      return;
    }
    setGuardando(true);
    setError(null);
    try {
      const { departamentoId, ...datos } = form;
      const { data } = await api.post("/puntos-de-venta", {
        ...datos,
        latitud: Number(form.latitud),
        longitud: Number(form.longitud),
      });
      setForm({ ...vacio, departamentoId });
      onCreado(data);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="design-form pv-form" onSubmit={guardar}>
      <div className="pv-form-head">
        <h2>Nuevo punto de venta</h2>
        <p>Cargá el comercio y marcá su ubicación en el mapa.</p>
      </div>
      {error && <Alert tone="danger">{error}</Alert>}
      <FormField
        label="Nombre del comercio"
        name="nombreComercio"
        value={form.nombreComercio}
        required
        maxLength={160}
        onChange={(e) => setForm({ ...form, nombreComercio: e.target.value })}
      />
      <label className="dm-field">
        <span>Minorista responsable</span>
        <select
          className="dm-input"
          value={form.minoristaId}
          onChange={(e) => setForm({ ...form, minoristaId: e.target.value })}
          required
        >
          <option value="">Seleccioná un minorista</option>
          {minoristas.map((m) => (
            <option key={m.id} value={m.id}>
              {m.apellido}, {m.nombre} · {m.numeroDocumento}
            </option>
          ))}
        </select>
        {!minoristas.length && (
          <small>Registrá un minorista para asociarlo al comercio.</small>
        )}
      </label>
      <DireccionField api={api} value={form} onChange={setForm} />
      <button
        className="dm-button dm-button-primary"
        type="submit"
        disabled={guardando || !minoristas.length}
      >
        {guardando ? "Guardando…" : "Guardar punto de venta"}
      </button>
    </form>
  );
}
