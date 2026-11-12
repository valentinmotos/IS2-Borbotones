import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useSession } from "../../../../packages/auth/session.js";
import { Alert } from "../../../../packages/design/components.jsx";
import GooglePasswordForm from "./GooglePasswordForm.jsx";
import {
  googlePost,
  googleStart,
  googleStatus,
  useGoogleAvailable,
} from "./google.js";

export default function GoogleAccount() {
  return useGoogleAvailable() ? <GoogleAccountControls /> : null;
}

function GoogleAccountControls() {
  const { acceptLogin } = useSession();
  const { state } = useLocation();
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(
    state?.googleLinked ? "Vinculaste tu cuenta de Google." : "",
  );

  useEffect(() => {
    const controller = new AbortController();
    googleStatus(controller.signal).then(setResult, (failure) => {
      if (!controller.signal.aborted) setError(failure.message);
    });
    return () => controller.abort();
  }, []);

  async function unlink(clave) {
    const session = await googlePost("desvincular", result, { clave });
    acceptLogin(session);
    setResult({ ...result, vinculada: false });
    setNotice(
      "Desvinculaste Google. Podés seguir ingresando con correo y clave.",
    );
  }

  return (
    <section className="cuenta-form" aria-labelledby="cuenta-google">
      <h2 id="cuenta-google">Acceso con Google</h2>
      {notice && <Alert tone="success">{notice}</Alert>}
      {error && <Alert tone="danger">{error}</Alert>}
      {!result && !error && <p>Consultando el vínculo…</p>}
      {result?.vinculada && (
        <GooglePasswordForm
          label="Desvincular cuenta de Google"
          submit={unlink}
        />
      )}
      {result && !result.vinculada && (
        <a className="dm-button" href={googleStart}>
          Vincular cuenta de Google
        </a>
      )}
    </section>
  );
}
