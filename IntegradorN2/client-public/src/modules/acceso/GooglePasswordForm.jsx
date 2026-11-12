import { useState } from "react";
import { Alert, FormField } from "../../../../packages/design/components.jsx";

export default function GooglePasswordForm({ label, submit }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);

  async function confirm(event) {
    event.preventDefault();
    if (pending) return;
    const form = event.currentTarget;
    const clave = new FormData(form).get("clave");
    setPending(true);
    setError(null);
    try {
      await submit(clave);
      form.reset();
    } catch (failure) {
      setError(failure);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={confirm} aria-busy={pending}>
      {error && <Alert tone="danger">{error.message}</Alert>}
      <FormField
        label="Clave actual de Dulce Mayor"
        name="clave"
        type="password"
        autoComplete="current-password"
        required
        maxLength={72}
      />
      <button className="dm-button dm-button-primary" disabled={pending}>
        {pending ? "Confirmando…" : label}
      </button>
    </form>
  );
}
