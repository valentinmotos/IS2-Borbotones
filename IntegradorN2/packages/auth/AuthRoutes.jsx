import { useEffect, useState } from "react";
import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
import {
  KeyRound,
  LoaderCircle,
  LogIn,
  LogOut,
  RefreshCw,
  ShieldAlert,
  TriangleAlert,
  UserPlus,
} from "lucide-react";
import { Alert } from "../design/components.jsx";
import { useSession } from "./session.js";

const loginFields = [
  {
    name: "nombreUsuario",
    label: "Correo electrónico",
    type: "email",
    autoComplete: "username",
  },
  {
    name: "clave",
    label: "Clave",
    type: "password",
    autoComplete: "current-password",
  },
];
const passwordFields = [
  {
    name: "claveActual",
    label: "Clave actual",
    type: "password",
    autoComplete: "current-password",
  },
  {
    name: "nuevaClave",
    label: "Clave nueva",
    type: "password",
    autoComplete: "new-password",
  },
  {
    name: "confirmarClave",
    label: "Confirmá la clave nueva",
    type: "password",
    autoComplete: "new-password",
  },
];

export function LogoutButton() {
  const { logout, signingOut } = useSession();
  return (
    <button
      className="dm-button"
      type="button"
      onClick={logout}
      disabled={signingOut}
    >
      <LogOut size={16} aria-hidden="true" />
      {signingOut ? "Cerrando sesión…" : "Cerrar sesión"}
    </button>
  );
}

function SessionStatus() {
  const { status, notice, retry, signingOut } = useSession();
  if (signingOut || status === "loading")
    return (
      <p className="auth-status" role="status">
        <LoaderCircle className="auth-spin" size={20} aria-hidden="true" />
        {signingOut ? "Cerrando sesión…" : "Verificando tu sesión…"}
      </p>
    );
  return (
    <section className="auth-panel">
      <div className="auth-head">
        <span className="auth-icon" data-tone="danger" aria-hidden="true">
          <TriangleAlert size={22} />
        </span>
        <h2>No pudimos verificar tu sesión</h2>
      </div>
      <Alert tone="danger">{notice}</Alert>
      <div className="auth-actions">
        <button
          type="button"
          className="dm-button dm-button-primary"
          onClick={retry}
        >
          <RefreshCw size={16} aria-hidden="true" />
          Reintentar
        </button>
        <LogoutButton />
      </div>
    </section>
  );
}

function AccessDenied() {
  const { home } = useSession();
  useEffect(() => {
    document.title = "Acceso denegado · Dulce Mayor";
  }, []);
  return (
    <section className="auth-panel">
      <div className="auth-head">
        <span className="auth-icon" data-tone="danger" aria-hidden="true">
          <ShieldAlert size={22} />
        </span>
        <h1>Acceso denegado</h1>
        <p>Tu rol no tiene permiso para acceder a esta página.</p>
      </div>
      <div className="auth-actions">
        {home ? (
          <Link className="dm-button dm-button-primary" to={home}>
            Volver a mi inicio
          </Link>
        ) : (
          <LogoutButton />
        )}
      </div>
    </section>
  );
}

export function RequireAccess({ roles }) {
  const { status, user, home, signingOut, signedOut } = useSession();
  const location = useLocation();
  if (signingOut || status === "loading" || status === "error")
    return <SessionStatus />;
  if (status === "guest")
    return (
      <Navigate
        to="/login"
        replace
        state={signedOut ? null : { from: location }}
      />
    );
  if (!home) return <AccessDenied />;
  if (user.debeCambiarClave)
    return <Navigate to="/cambiar-clave" replace state={{ from: location }} />;
  return !roles || roles.includes(user.rol) ? <Outlet /> : <AccessDenied />;
}

function AuthForm({
  changing = false,
  otherLogin,
  registrationPath,
  alternateLogin,
}) {
  const { login, changePassword, notice } = useSession();
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const title = changing ? "Cambiar clave" : "Iniciar sesión";
  const fields = changing ? passwordFields : loginFields;
  useEffect(() => {
    document.title = `${title} · Dulce Mayor`;
  }, [title]);

  async function submit(event) {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.currentTarget));
    if (changing && values.nuevaClave !== values.confirmarClave) {
      setError({
        message: "Las claves nuevas deben coincidir.",
        campo: "confirmarClave",
      });
      return;
    }
    setPending(true);
    setError(null);
    try {
      if (changing) await changePassword(values);
      else if (!(await login(values))) {
        setError({
          message: "Acceso denegado. Esta cuenta corresponde al otro portal.",
        });
      }
    } catch (failure) {
      setError(failure);
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="auth-panel">
      <div className="auth-head">
        <span className="auth-icon" aria-hidden="true">
          {changing ? <KeyRound size={22} /> : <LogIn size={22} />}
        </span>
        <h1>{title}</h1>
        <p>
          {changing
            ? "Para continuar, reemplazá tu clave temporal por una nueva."
            : "Ingresá con el correo y la clave de tu cuenta."}
        </p>
      </div>
      {!changing && notice && <Alert tone="warning">{notice}</Alert>}
      <form className="auth-form" onSubmit={submit} aria-busy={pending}>
        {error && (
          <Alert tone="danger">
            <span id="auth-error">{error.message}</span>
          </Alert>
        )}
        {fields.map(({ name, label, type, autoComplete }) => (
          <div key={name} className="dm-field">
            <label htmlFor={name}>{label}</label>
            <input
              id={name}
              name={name}
              type={type}
              autoComplete={autoComplete}
              required
              aria-invalid={error?.campo === name || undefined}
              aria-describedby={
                error?.campo === name ? "auth-error" : undefined
              }
            />
          </div>
        ))}
        <button
          className="dm-button dm-button-primary dm-button-lg dm-button-block"
          type="submit"
          disabled={pending}
        >
          {pending ? "Procesando…" : changing ? "Guardar clave" : "Ingresar"}
        </button>
      </form>
      {changing ? (
        <div className="auth-foot">
          <span>¿No es tu cuenta?</span>
          <LogoutButton />
        </div>
      ) : (
        <>
          {/* Ingreso alternativo (p. ej. Google); el separador se oculta si viene vacío. */}
          <div className="auth-alt">{alternateLogin}</div>
          {registrationPath && (
            <div className="auth-register">
              <p>
                <strong>¿No tenés cuenta?</strong> Registrá tu comercio para
                seguir tus pedidos y pagar tus facturas.
              </p>
              <Link className="dm-button dm-button-block" to={registrationPath}>
                <UserPlus size={17} aria-hidden="true" />
                Creá tu cuenta
              </Link>
            </div>
          )}
          <p className="auth-other">
            <a href={otherLogin.href}>{otherLogin.label}</a>
          </p>
        </>
      )}
    </section>
  );
}

export function LoginPage({ otherLogin, registrationPath, alternateLogin }) {
  const { status, user, home, signingOut } = useSession();
  const { state } = useLocation();
  if (signingOut || status === "loading" || status === "error")
    return <SessionStatus />;
  if (status === "guest")
    return (
      <AuthForm
        otherLogin={otherLogin}
        registrationPath={registrationPath}
        alternateLogin={alternateLogin}
      />
    );
  if (!home) return <AccessDenied />;
  if (user.debeCambiarClave)
    return <Navigate to="/cambiar-clave" replace state={state} />;
  return <Navigate to={state?.from ?? home} replace />;
}

export function ChangePasswordPage() {
  const { status, user, home, signingOut, signedOut } = useSession();
  const { state } = useLocation();
  if (signingOut || status === "loading" || status === "error")
    return <SessionStatus />;
  if (status === "guest")
    return <Navigate to="/login" replace state={signedOut ? null : state} />;
  if (!home) return <AccessDenied />;
  if (!user.debeCambiarClave)
    return <Navigate to={state?.from ?? home} replace />;
  return <AuthForm key="change-password" changing />;
}
