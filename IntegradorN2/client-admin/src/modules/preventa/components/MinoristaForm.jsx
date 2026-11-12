import { useState } from "react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";

const inicial = {
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  tipoDocumento: "DNI",
  numeroDocumento: "",
  correo: "",
  clave: "",
};

export default function MinoristaForm({ onCreado }) {
  const [form, setForm] = useState(inicial);
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const campo = (name) => ({
    name,
    value: form[name],
    required: true,
    onChange: (event) => setForm({ ...form, [name]: event.target.value }),
  });

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    setGuardando(true);
    setError(null);
    try {
      const { data } = await api.post("/preventa/minoristas", form);
      onCreado(data);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <form className="design-form preventa-minorista" onSubmit={guardar}>
      <h2>Nuevo minorista</h2>
      {error && <Alert tone="danger">{error}</Alert>}
      <div className="preventa-campos">
        <FormField
          label="Nombre"
          autoComplete="off"
          maxLength={100}
          {...campo("nombre")}
        />
        <FormField
          label="Apellido"
          autoComplete="off"
          maxLength={100}
          {...campo("apellido")}
        />
        <FormField
          label="Fecha de nacimiento"
          type="date"
          {...campo("fechaNacimiento")}
        />
        <label className="dm-field">
          <span>Tipo de documento</span>
          <select className="dm-input" {...campo("tipoDocumento")}>
            <option value="DNI">DNI</option>
            <option value="CUIT">CUIT</option>
            <option value="PASAPORTE">Pasaporte</option>
          </select>
        </label>
        <FormField
          label="Número de documento"
          maxLength={30}
          {...campo("numeroDocumento")}
        />
        <FormField
          label="Correo del minorista"
          type="email"
          autoComplete="off"
          maxLength={254}
          {...campo("correo")}
        />
        <FormField
          label="Clave elegida por el minorista"
          type="password"
          autoComplete="new-password"
          minLength={8}
          hint="El minorista activa su cuenta con el código enviado a su correo."
          {...campo("clave")}
        />
      </div>
      <button className="dm-button dm-button-primary" disabled={guardando}>
        {guardando ? "Registrando…" : "Registrar minorista"}
      </button>
    </form>
  );
}
