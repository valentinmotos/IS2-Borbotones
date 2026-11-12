import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CircleCheck, LogIn, MailCheck } from "lucide-react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import GoogleButton from "../../acceso/GoogleButton.jsx";
import { googlePost } from "../../acceso/google.js";
import "../styles.css";

// Pasos que se muestran al costado del formulario; el actual sale de la fase del registro.
const pasos = [
  {
    fase: "registro",
    titulo: "Completá tus datos",
    texto: "Nombre, documento, correo y una clave para tu cuenta.",
  },
  {
    fase: "activacion",
    titulo: "Confirmá tu correo",
    texto: "Te mandamos un código de 6 dígitos y lo ingresás acá mismo.",
  },
  {
    fase: "activada",
    titulo: "Listo",
    texto:
      "Ingresás a tu cuenta para seguir tus pedidos, descargar tus facturas y pagarlas.",
  },
];

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
  const [datos, setDatos] = useState(() => ({
    ...datosVacios,
    correo: google?.correo ?? "",
  }));
  const [fase, setFase] = useState(
    google?.estado === "PENDIENTE" ? "activacion" : "registro",
  );
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
      setError({
        message: "Las claves no coinciden.",
        campo: "confirmarClave",
      });
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

  const pasoActual = pasos.findIndex((paso) => paso.fase === fase);

  return (
    <div className="wrap page registro">
      <div className="registro-grid">
        <section className="panel registro-main">
          {fase === "registro" && (
            <>
              <header className="registro-head">
                <p className="eyebrow">Cuenta de comercio</p>
                <h1>Crear cuenta minorista</h1>
                <p>
                  {google
                    ? "Completá tus datos y elegí una clave de respaldo para tu cuenta de Dulce Mayor."
                    : "Completá tus datos y confirmá el correo para ingresar a tu cuenta."}
                </p>
              </header>
              {error && <Alert tone="danger">{error.message}</Alert>}
              <form
                className="registro-form"
                onSubmit={registrar}
                aria-busy={pendiente}
              >
                <fieldset className="registro-grupo">
                  <legend>Tus datos</legend>
                  <FormField
                    label="Nombre"
                    name="nombre"
                    autoComplete="given-name"
                    maxLength={100}
                    required
                    value={datos.nombre}
                    onChange={(e) => cambiar("nombre", e.target.value)}
                    error={error?.campo === "nombre" ? error.message : ""}
                  />
                  <FormField
                    label="Apellido"
                    name="apellido"
                    autoComplete="family-name"
                    maxLength={100}
                    required
                    value={datos.apellido}
                    onChange={(e) => cambiar("apellido", e.target.value)}
                    error={error?.campo === "apellido" ? error.message : ""}
                  />
                  <label className="dm-field">
                    <span>Tipo de documento</span>
                    <select
                      className="dm-location-select"
                      name="tipoDocumento"
                      value={datos.tipoDocumento}
                      onChange={(e) => cambiar("tipoDocumento", e.target.value)}
                    >
                      <option value="DNI">DNI</option>
                      <option value="CUIT">CUIT</option>
                      <option value="PASAPORTE">Pasaporte</option>
                    </select>
                  </label>
                  <FormField
                    label="Número de documento"
                    name="numeroDocumento"
                    autoComplete="off"
                    maxLength={30}
                    required
                    value={datos.numeroDocumento}
                    onChange={(e) => cambiar("numeroDocumento", e.target.value)}
                    error={
                      error?.campo === "numeroDocumento" ? error.message : ""
                    }
                  />
                  <FormField
                    label="Fecha de nacimiento"
                    name="fechaNacimiento"
                    type="date"
                    autoComplete="bday"
                    required
                    value={datos.fechaNacimiento}
                    onChange={(e) => cambiar("fechaNacimiento", e.target.value)}
                    error={
                      error?.campo === "fechaNacimiento" ? error.message : ""
                    }
                  />
                </fieldset>
                <fieldset className="registro-grupo">
                  <legend>Datos de acceso</legend>
                  <div className="registro-ancho">
                    <FormField
                      label="Correo electrónico"
                      name="correo"
                      type="email"
                      autoComplete="email"
                      maxLength={254}
                      required
                      readOnly={Boolean(google)}
                      value={datos.correo}
                      onChange={(e) => cambiar("correo", e.target.value)}
                      error={error?.campo === "correo" ? error.message : ""}
                    />
                  </div>
                  <FormField
                    label="Clave (mínimo 8 caracteres)"
                    name="clave"
                    type="password"
                    autoComplete="new-password"
                    minLength={8}
                    required
                    value={datos.clave}
                    onChange={(e) => cambiar("clave", e.target.value)}
                    error={error?.campo === "clave" ? error.message : ""}
                  />
                  <FormField
                    label="Confirmar clave"
                    name="confirmarClave"
                    type="password"
                    autoComplete="new-password"
                    minLength={8}
                    required
                    value={datos.confirmarClave}
                    onChange={(e) => cambiar("confirmarClave", e.target.value)}
                    error={
                      error?.campo === "confirmarClave" ? error.message : ""
                    }
                  />
                </fieldset>
                <div className="registro-acciones">
                  <button
                    className="dm-button dm-button-primary dm-button-lg"
                    disabled={pendiente}
                  >
                    {pendiente ? "Enviando…" : "Crear cuenta"}
                  </button>
                  <p>
                    ¿Ya tenés una cuenta?{" "}
                    <Link to="/login">Iniciar sesión</Link>
                  </p>
                </div>
              </form>
              {!google && (
                <div className="registro-alt">
                  <GoogleButton />
                  <button
                    className="dm-button dm-button-ghost"
                    type="button"
                    onClick={() => {
                      setError(null);
                      setActivarManualmente(true);
                      setFase("activacion");
                    }}
                  >
                    Ya tengo un código de activación
                  </button>
                </div>
              )}
            </>
          )}

          {fase === "activacion" && (
            <>
              <header className="registro-head">
                <span className="registro-icono" aria-hidden="true">
                  <MailCheck size={24} />
                </span>
                <h1>Confirmá tu correo</h1>
                <p>
                  {mensaje ||
                    "Ingresá el código de seis dígitos que recibiste por correo."}
                </p>
                {!activarManualmente && (
                  <p>
                    Correo de activación <strong>{datos.correo}</strong>.
                  </p>
                )}
              </header>
              {error && <Alert tone="danger">{error.message}</Alert>}
              <form
                className="registro-codigo"
                onSubmit={activar}
                aria-busy={pendiente}
              >
                {activarManualmente && (
                  <FormField
                    label="Correo electrónico"
                    name="correo"
                    type="email"
                    autoComplete="email"
                    maxLength={254}
                    required
                    value={datos.correo}
                    onChange={(e) => cambiar("correo", e.target.value)}
                  />
                )}
                <FormField
                  label="Código de 6 dígitos"
                  name="codigo"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  required
                  value={codigo}
                  onChange={(e) => {
                    setCodigo(e.target.value.replace(/\D/g, "").slice(0, 6));
                    setError(null);
                  }}
                  error={error?.campo === "codigo" ? error.message : ""}
                />
                <button
                  className="dm-button dm-button-primary dm-button-lg"
                  disabled={pendiente || codigo.length !== 6}
                >
                  {pendiente ? "Verificando…" : "Activar cuenta"}
                </button>
              </form>
              <p className="registro-reenviar">
                ¿No te llegó? Revisá el correo no deseado o{" "}
                <button
                  className="dm-button dm-button-ghost dm-button-sm"
                  type="button"
                  onClick={reenviar}
                  disabled={pendiente || !datos.correo}
                >
                  Reenviar código
                </button>
              </p>
            </>
          )}

          {fase === "activada" && (
            <div className="registro-listo">
              <span
                className="registro-icono"
                data-tono="success"
                aria-hidden="true"
              >
                <CircleCheck size={26} />
              </span>
              <h1>Cuenta activada</h1>
              <p>{mensaje}</p>
              <Link
                className="dm-button dm-button-primary dm-button-lg"
                to="/login"
              >
                <LogIn size={18} aria-hidden="true" />
                Iniciar sesión
              </Link>
              {google && <GoogleButton />}
            </div>
          )}
        </section>

        <aside className="registro-lado" aria-labelledby="registro-pasos">
          <p className="script-label">En tres pasos</p>
          <h2 id="registro-pasos">Cómo creás tu cuenta</h2>
          <ol className="registro-pasos">
            {pasos.map((paso, indice) => (
              <li
                key={paso.fase}
                data-estado={
                  indice < pasoActual
                    ? "hecho"
                    : indice === pasoActual
                      ? "actual"
                      : "pendiente"
                }
                aria-current={indice === pasoActual ? "step" : undefined}
              >
                <strong>{paso.titulo}</strong>
                <span>{paso.texto}</span>
              </li>
            ))}
          </ol>
          <p className="registro-login">
            ¿Ya tenés cuenta? <Link to="/login">Ingresá acá</Link>
          </p>
        </aside>
      </div>
    </div>
  );
}
