import { useState } from "react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import { formasPago } from "../formato.js";
import useApiResource from "../../../lib/useApiResource.js";

export default function PagoForm({ saldo, ocupado, registrar }) {
  const [divisa, setDivisa] = useState("ARS");
  const [version, setVersion] = useState(0);
  const recurso = useApiResource("/formas-de-pago", version);
  const formas = (recurso.data ?? []).filter(
    (forma) =>
      forma.activo && ["EFECTIVO", "TRANSFERENCIA"].includes(forma.tipoPago),
  );

  function guardar(event) {
    event.preventDefault();
    const campos = Object.fromEntries(new FormData(event.currentTarget));
    registrar({ ...campos, monto: Number(campos.monto) });
  }

  if (recurso.cargando) return <p role="status">Cargando formas de pago…</p>;
  if (recurso.error) {
    return (
      <Alert tone="danger">
        {recurso.error.message}{" "}
        <button
          type="button"
          className="dm-button"
          onClick={() => setVersion((actual) => actual + 1)}
        >
          Reintentar
        </button>
      </Alert>
    );
  }
  if (!formas.length)
    return (
      <Alert tone="warning">
        No hay formas activas de efectivo o transferencia.
      </Alert>
    );

  return (
    <form onSubmit={guardar}>
      <fieldset className="almacen-pago" disabled={ocupado}>
        <legend>Registrar pago</legend>
        <label className="dm-field">
          <span>Forma de pago</span>
          <select name="formaDePagoId" required defaultValue="">
            <option value="">Seleccioná una forma</option>
            {formas.map((forma) => (
              <option key={forma.id} value={forma.id}>
                {formasPago[forma.tipoPago]}
                {forma.observacion ? ` · ${forma.observacion}` : ""}
              </option>
            ))}
          </select>
        </label>
        <label className="dm-field">
          <span>Moneda</span>
          <select
            name="moneda"
            value={divisa}
            onChange={(event) => setDivisa(event.target.value)}
          >
            <option value="ARS">Pesos argentinos (ARS)</option>
            <option value="USD">Dólares estadounidenses (USD)</option>
          </select>
        </label>
        <FormField
          key={divisa}
          label="Monto"
          name="monto"
          type="number"
          required
          min="0.01"
          step="0.01"
          max={divisa === "ARS" ? saldo : undefined}
          defaultValue={divisa === "ARS" ? saldo.toFixed(2) : ""}
          hint={
            divisa === "USD"
              ? "Se aplica la cotización de venta vigente al registrar el pago."
              : undefined
          }
        />
        <FormField
          label="Observación (opcional)"
          name="observacion"
          maxLength={500}
        />
        <button className="dm-button dm-button-primary" type="submit">
          {ocupado ? "Procesando…" : "Registrar pago"}
        </button>
      </fieldset>
    </form>
  );
}
