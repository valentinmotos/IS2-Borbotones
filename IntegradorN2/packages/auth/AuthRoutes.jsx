import { useEffect, useState } from "react";
import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
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
      {signingOut ? "Cerrando sesión…" : "Cerrar sesión"}
    </button>
  );
}

function SessionStatus() {
  const { status, notice, retry, signingOut } = useSession();
  if (signingOut) return <p role="status">Cerrando sesión…</p>;
  if (status === "loading") return <p role="status">Verificando tu sesión…</p>;
  return (
    <section className="auth-panel">
      <Alert tone="danger">{notice}</Alert>
      <button type="button" className="dm-button" onClick={retry}>
        Reintentar
      </button>
      <LogoutButton />
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
      <h1>Acceso denegado</h1>
      <p>Tu rol no tiene permiso para acceder a esta página.</p>
      {home ? <Link to={home}>Volver a mi inicio</Link> : <LogoutButton />}
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

function AuthForm({ changing = false, otherLogin }) {
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
      <h1>{title}</h1>
      {changing && (
        <p>Para continuar, reemplazá tu clave temporal por una nueva.</p>
      )}
      {!changing && notice && <Alert tone="warning">{notice}</Alert>}
      <form onSubmit={submit} aria-busy={pending}>
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
          className="dm-button dm-button-primary"
          type="submit"
          disabled={pending}
        >
          {pending ? "Procesando…" : changing ? "Guardar clave" : "Ingresar"}
        </button>
      </form>
      {changing ? (
        <LogoutButton />
      ) : (
        <a href={otherLogin.href}>{otherLogin.label}</a>
      )}
    </section>
  );
}

export function LoginPage({ otherLogin }) {
  const { status, user, home, signingOut } = useSession();
  const { state } = useLocation();
  if (signingOut || status === "loading" || status === "error")
    return <SessionStatus />;
  if (status === "guest") return <AuthForm otherLogin={otherLogin} />;
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
