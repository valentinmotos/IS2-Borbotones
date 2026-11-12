import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { KeyRound, LoaderCircle } from "lucide-react";
import { useSession } from "../../../../packages/auth/session.js";
import { Alert } from "../../../../packages/design/components.jsx";
import RegistroMinoristaPage from "../registro/pages/RegistroMinoristaPage.jsx";
import GooglePasswordForm from "./GooglePasswordForm.jsx";
import { googlePost, googleStart, googleStatus } from "./google.js";
import "./styles.css";

export default function GoogleAccessPage() {
  const { status, user, acceptLogin } = useSession();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const callbackError = params.get("error");

  useEffect(() => {
    document.title = "Acceso con Google · Dulce Mayor";
    if (callbackError) return;
    const controller = new AbortController();
    googleStatus(controller.signal).then(setResult, (failure) => {
      if (!controller.signal.aborted) setError(failure.message);
    });
    return () => controller.abort();
  }, [callbackError]);

  const canLink =
    status === "ready" && user.rol === "MINORISTA" && !user.debeCambiarClave;
  const mayComplete =
    result?.estado === "EXISTENTE" || result?.estado === "REGISTRO";

  async function login() {
    setPending(true);
    setError(null);
    try {
      const session = await googlePost("sesion", result, {}, true);
      if (acceptLogin(session)) navigate("/cuenta", { replace: true });
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending(false);
    }
  }

  async function link(clave) {
    await googlePost("vincular", result, { clave });
    navigate("/cuenta/datos", { replace: true, state: { googleLinked: true } });
  }

  if (
    status === "guest" &&
    !callbackError &&
    (result?.estado === "REGISTRO" || result?.estado === "PENDIENTE")
  ) {
    return <RegistroMinoristaPage google={result} />;
  }

  return (
    <section className="auth-panel google-access">
      <header className="auth-head">
        <span className="auth-icon" aria-hidden="true">
          <KeyRound size={22} />
        </span>
        <h1>Acceso con Google</h1>
        <p>Ingresá o vinculá tu cuenta de comercio con tu cuenta de Google.</p>
      </header>
      {callbackError && (
        <Alert tone="warning">
          {callbackError === "correo"
            ? "Google no devolvió un correo verificado para esta cuenta."
            : "No se completó el acceso con Google. Podés volver a intentarlo."}
        </Alert>
      )}
      {error && <Alert tone="danger">{error}</Alert>}
      {!callbackError && !error && (!result || status === "loading") && (
        <p className="google-status" role="status">
          <LoaderCircle className="auth-spin" size={18} aria-hidden="true" />
          Verificando el acceso…
        </p>
      )}
      {!callbackError && result && status !== "loading" && (
        <>
          {result.estado === "SIN_FLUJO" && (
            <p>El acceso venció. Volvé a iniciarlo.</p>
          )}
          {result.estado === "BLOQUEADA" && (
            <p>Esta cuenta no está habilitada para ingresar con Google.</p>
          )}
          {canLink && mayComplete && !result.vinculada && (
            <>
              <p>
                Confirmá tu clave para vincular <strong>{result.correo}</strong>{" "}
                a tu cuenta minorista.
              </p>
              <GooglePasswordForm
                label="Vincular cuenta de Google"
                submit={link}
              />
            </>
          )}
          {canLink &&
            (result.vinculada ||
              result.estado === "VINCULADA" ||
              result.estado === "PENDIENTE") && (
              <p>Tu cuenta o la identidad elegida ya tiene un vínculo.</p>
            )}
          {status === "guest" && result.estado === "EXISTENTE" && (
            <p>
              Si ya tenés una cuenta minorista, ingresá con correo y clave y
              vinculá Google desde Mis datos.
            </p>
          )}
          {status === "guest" && result.estado === "VINCULADA" && (
            <button
              className="dm-button dm-button-primary dm-button-lg dm-button-block"
              onClick={login}
              disabled={pending}
            >
              {pending ? "Ingresando…" : "Ingresar a mi cuenta"}
            </button>
          )}
        </>
      )}
      <div className="auth-foot">
        <Link to={canLink ? "/cuenta/datos" : "/login"}>
          Volver {canLink ? "a Mis datos" : "al inicio de sesión"}
        </Link>
        {(callbackError || error || result?.estado === "SIN_FLUJO") && (
          <a href={googleStart}>Volver a intentar con Google</a>
        )}
      </div>
    </section>
  );
}
