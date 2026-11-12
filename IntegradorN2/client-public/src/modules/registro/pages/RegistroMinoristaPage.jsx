import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Alert, FormField } from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import GoogleButton from "../../acceso/GoogleButton.jsx";
import { googlePost } from "../../acceso/google.js";

const datosVacios = {
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  tipoDocumento: "DNI",
  numeroDocumento: "",
  correo: "",
  clave: "",
  confirmarClave: "",
};

export default function RegistroMinoristaPage({ google }) {
  const [datos, setDatos] = useState(() => ({ ...datosVacios, correo: google?.correo ?? "" }));
  const [fase, setFase] = useState(google?.estado === "PENDIENTE" ? "activacion" : "registro");
  const [codigo, setCodigo] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState(null);
  const [pendiente, setPendiente] = useState(false);
  const [activarManualmente, setActivarManualmente] = useState(false);

  useEffect(() => {
    document.title = "Crear cuenta · Dulce Mayor";
  }, []);

  function cambiar(campo, valor) {
    setDatos((actuales) => ({ ...actuales, [campo]: valor }));
    setError(null);
  }

  async function registrar(event) {
    event.preventDefault();
    if (datos.clave !== datos.confirmarClave) {
      setError({ message: "Las claves no coinciden.", campo: "confirmarClave" });
      return;
    }
    setPendiente(true);
    setError(null);
    try {
      // La confirmación solo se controla en la pantalla; la API no la recibe.
      const { confirmarClave: _confirmarClave, ...request } = datos;
      const respuesta = google
        ? await googlePost("registro", google, request, true)
        : (await api.post("/publico/registro", request)).data;
      setMensaje(respuesta.mensaje);
      setFase("activacion");
    } catch (fallo) {
      setError(fallo);
    } finally {
      setPendiente(false);
    }
  }

  async function activar(event) {
    event.preventDefault();
    setPendiente(true);
    setError(null);
    try {
      const respuesta = await api.post("/publico/registro/activar", {
        correo: datos.correo,
        codigo,
      });
      setMensaje(respuesta.data.mensaje);
      setFase("activada");
    } catch (fallo) {
      setError(fallo);
    } finally {
      setPendiente(false);
    }
  }

  async function reenviar() {
    setPendiente(true);
    setError(null);
    try {
      const respuesta = await api.post("/publico/registro/reenviar", {
        correo: datos.correo,
      });
      setMensaje(respuesta.data.mensaje);
    } catch (fallo) {
      setError(fallo);
    } finally {
      setPendiente(false);
    }
  }

  return (
    <section className="auth-panel register-panel">
      {fase === "registro" && (
        <>
          <h1>Crear cuenta minorista</h1>
          <p>{google
            ? "Completá tus datos y elegí una clave de respaldo para tu cuenta de Dulce Mayor."
            : "Completá tus datos y confirmá el correo para ingresar a tu cuenta."}</p>
          {error && <Alert tone="danger">{error.message}</Alert>}
          <form onSubmit={registrar} aria-busy={pendiente}>
            <FormField label="Nombre" name="nombre" autoComplete="given-name" maxLength={100} required value={datos.nombre} onChange={(e) => cambiar("nombre", e.target.value)} error={error?.campo === "nombre" ? error.message : ""} />
            <FormField label="Apellido" name="apellido" autoComplete="family-name" maxLength={100} required value={datos.apellido} onChange={(e) => cambiar("apellido", e.target.value)} error={error?.campo === "apellido" ? error.message : ""} />
            <FormField label="Fecha de nacimiento" name="fechaNacimiento" type="date" autoComplete="bday" required value={datos.fechaNacimiento} onChange={(e) => cambiar("fechaNacimiento", e.target.value)} error={error?.campo === "fechaNacimiento" ? error.message : ""} />
            <label className="dm-field"><span>Tipo de documento</span><select className="dm-location-select" name="tipoDocumento" value={datos.tipoDocumento} onChange={(e) => cambiar("tipoDocumento", e.target.value)}><option value="DNI">DNI</option><option value="CUIT">CUIT</option><option value="PASAPORTE">Pasaporte</option></select></label>
            <FormField label="Número de documento" name="numeroDocumento" autoComplete="off" maxLength={30} required value={datos.numeroDocumento} onChange={(e) => cambiar("numeroDocumento", e.target.value)} error={error?.campo === "numeroDocumento" ? error.message : ""} />
            <FormField label="Correo electrónico" name="correo" type="email" autoComplete="email" maxLength={254} required readOnly={Boolean(google)} value={datos.correo} onChange={(e) => cambiar("correo", e.target.value)} error={error?.campo === "correo" ? error.message : ""} />
            <FormField label="Clave (mínimo 8 caracteres)" name="clave" type="password" autoComplete="new-password" minLength={8} required value={datos.clave} onChange={(e) => cambiar("clave", e.target.value)} error={error?.campo === "clave" ? error.message : ""} />
            <FormField label="Confirmar clave" name="confirmarClave" type="password" autoComplete="new-password" minLength={8} required value={datos.confirmarClave} onChange={(e) => cambiar("confirmarClave", e.target.value)} error={error?.campo === "confirmarClave" ? error.message : ""} />
            <button className="dm-button dm-button-primary" disabled={pendiente}>{pendiente ? "Enviando…" : "Crear cuenta"}</button>
          </form>
          {!google && (
            <div className="dm-actions">
              <GoogleButton />
              <button className="dm-button" type="button" onClick={() => {
                setError(null);
                setActivarManualmente(true);
                setFase("activacion");
              }}>Ya tengo un código de activación</button>
            </div>
          )}
          <p>¿Ya tenés una cuenta? <Link to="/login">Iniciar sesión</Link></p>
        </>
      )}

      {fase === "activacion" && (
        <>
          <h1>Confirmá tu correo</h1>
          <p>{mensaje || "Ingresá el código de seis dígitos que recibiste por correo."}</p>
          {!activarManualmente && <p>Correo de activación <strong>{datos.correo}</strong>.</p>}
          {error && <Alert tone="danger">{error.message}</Alert>}
          <form onSubmit={activar} aria-busy={pendiente}>
            {activarManualmente && <FormField label="Correo electrónico" name="correo" type="email" autoComplete="email" maxLength={254} required value={datos.correo} onChange={(e) => cambiar("correo", e.target.value)} />}
            <FormField label="Código de 6 dígitos" name="codigo" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} required value={codigo} onChange={(e) => { setCodigo(e.target.value.replace(/\D/g, "").slice(0, 6)); setError(null); }} error={error?.campo === "codigo" ? error.message : ""} />
            <button className="dm-button dm-button-primary" disabled={pendiente || codigo.length !== 6}>{pendiente ? "Verificando…" : "Activar cuenta"}</button>
          </form>
          <button className="dm-button" type="button" onClick={reenviar} disabled={pendiente || !datos.correo}>Reenviar código</button>
        </>
      )}

      {fase === "activada" && (
        <>
          <h1>Cuenta activada</h1>
          <p>{mensaje}</p>
          <Link className="dm-button dm-button-primary" to="/login">Iniciar sesión</Link>
          {google && <GoogleButton />}
        </>
      )}
    </section>
  );
}
