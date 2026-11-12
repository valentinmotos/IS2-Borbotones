import { useState } from "react";
import { KeyRound } from "lucide-react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import { useSession } from "../../../../../packages/auth/session.js";

// E2-07: cambio de clave voluntario. Usa PUT /api/auth/clave (E1-01) a través de la sesión,
// que guarda el token nuevo: el anterior deja de servir.

const vacio = { claveActual: "", nuevaClave: "", confirmarClave: "" };

export default function CambioClaveForm() {
  const { changePassword } = useSession();
  const [claves, setClaves] = useState(vacio);
  const [aviso, setAviso] = useState(null);
  const [pendiente, setPendiente] = useState(false);

  function cambiar(campo, valor) {
    setClaves((actuales) => ({ ...actuales, [campo]: valor }));
    setAviso(null);
  }

  async function guardar(event) {
    event.preventDefault();
    if (claves.nuevaClave !== claves.confirmarClave) {
      setAviso({
        tono: "danger",
        texto: "Las claves nuevas deben coincidir.",
        campo: "confirmarClave",
      });
      return;
    }
    setPendiente(true);
    setAviso(null);
    try {
      await changePassword(claves);
      setClaves(vacio);
      setAviso({
        tono: "success",
        texto: "Cambiaste tu clave. Usala la próxima vez que ingreses.",
      });
    } catch (fallo) {
      setAviso({ tono: "danger", texto: fallo.message, campo: fallo.campo });
    } finally {
      setPendiente(false);
    }
  }

  const error = (campo) =>
    aviso?.tono === "danger" && aviso.campo === campo ? aviso.texto : undefined;

  return (
    <form
      className="panel cuenta-form cuenta-clave"
      onSubmit={guardar}
      aria-labelledby="cuenta-clave"
    >
      <div className="cuenta-clave-head">
        <span className="cuenta-clave-icon" aria-hidden="true">
          <KeyRound size={18} />
        </span>
        <h2 id="cuenta-clave">Cambiar clave</h2>
      </div>
      <p className="cuenta-nota">
        Después de cambiarla, usá la clave nueva cada vez que ingreses.
      </p>
      {aviso && !aviso.campo && <Alert tone={aviso.tono}>{aviso.texto}</Alert>}
      <FormField
        label="Clave actual"
        name="claveActual"
        type="password"
        autoComplete="current-password"
        required
        value={claves.claveActual}
        onChange={(e) => cambiar("claveActual", e.target.value)}
        error={error("claveActual")}
      />
      <FormField
        label="Clave nueva"
        name="nuevaClave"
        type="password"
        autoComplete="new-password"
        minLength={8}
        required
        hint="Al menos 8 caracteres."
        value={claves.nuevaClave}
        onChange={(e) => cambiar("nuevaClave", e.target.value)}
        error={error("nuevaClave")}
      />
      <FormField
        label="Repetí la clave nueva"
        name="confirmarClave"
        type="password"
        autoComplete="new-password"
        required
        value={claves.confirmarClave}
        onChange={(e) => cambiar("confirmarClave", e.target.value)}
        error={error("confirmarClave")}
      />
      <div className="form-actions">
        <button
          className="dm-button dm-button-block"
          type="submit"
          disabled={pendiente}
        >
          {pendiente ? "Guardando…" : "Cambiar clave"}
        </button>
      </div>
    </form>
  );
}
