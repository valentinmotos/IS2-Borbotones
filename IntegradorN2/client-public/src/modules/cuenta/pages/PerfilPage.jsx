import { useEffect, useState } from "react";
import {
  Alert,
  FormField,
} from "../../../../../packages/design/components.jsx";
import DireccionField from "../../../../../packages/address/DireccionField.jsx";
import { api } from "../../../lib/api.js";
import CambioClaveForm from "../components/CambioClaveForm.jsx";
import "../styles.css";

// E2-07: perfil del minorista autenticado. Datos personales, dirección de entrega con el
// componente de E1-04 y cambio de clave voluntario. El documento y el correo son de solo lectura.

const nombresDocumento = { DNI: "DNI", CUIT: "CUIT", PASAPORTE: "Pasaporte" };

function aFormulario(perfil) {
  return {
    nombre: perfil.nombre ?? "",
    apellido: perfil.apellido ?? "",
    fechaNacimiento: perfil.fechaNacimiento ?? "",
    departamentoId: perfil.departamentoId ?? "",
    localidadId: perfil.localidadId ?? "",
    calle: perfil.calle ?? "",
    numeracion: perfil.numeracion ?? "",
    barrio: perfil.barrio ?? "",
    manzanaPiso: perfil.manzanaPiso ?? "",
    casaDepartamento: perfil.casaDepartamento ?? "",
    referencia: perfil.referencia ?? "",
    latitud: perfil.latitud ?? null,
    longitud: perfil.longitud ?? null,
  };
}

export default function PerfilPage() {
  const [perfil, setPerfil] = useState(null);
  const [form, setForm] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    document.title = "Mis datos · Dulce Mayor";
    const controller = new AbortController();
    api.get("/minorista/perfil", { signal: controller.signal }).then(
      ({ data }) => {
        if (controller.signal.aborted) return;
        setPerfil(data);
        setForm(aFormulario(data));
      },
      (fallo) => {
        if (!controller.signal.aborted)
          setAviso({ tono: "danger", texto: fallo.message });
      },
    );
    return () => controller.abort();
  }, []);

  function cambiar(campo, valor) {
    setForm((actual) => ({ ...actual, [campo]: valor }));
  }

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    if (form.latitud == null || form.longitud == null) {
      setAviso({
        tono: "danger",
        texto: "Marcá la ubicación de entrega en el mapa.",
      });
      return;
    }
    setGuardando(true);
    setAviso(null);
    try {
      const { departamentoId: _departamentoId, ...datos } = form;
      const { data } = await api.put("/minorista/perfil", {
        ...datos,
        latitud: Number(form.latitud),
        longitud: Number(form.longitud),
      });
      setPerfil(data);
      setForm(aFormulario(data));
      setAviso({
        tono: "success",
        texto: `Guardamos tus datos. Entregamos en ${data.calle} ${data.numeracion}, ${data.localidad}.`,
      });
    } catch (fallo) {
      setAviso({ tono: "danger", texto: fallo.message });
    } finally {
      setGuardando(false);
    }
  }

  if (!perfil) {
    return (
      <div>
        <h1>Mis datos</h1>
        {aviso ? (
          <Alert tone={aviso.tono}>{aviso.texto}</Alert>
        ) : (
          <p>Cargando tus datos…</p>
        )}
      </div>
    );
  }

  return (
    <div className="cuenta-perfil">
      <h1>Mis datos</h1>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      <form
        className="cuenta-form"
        onSubmit={guardar}
        aria-labelledby="cuenta-datos"
      >
        <h2 id="cuenta-datos">Datos personales</h2>
        <dl className="cuenta-fijos">
          <div>
            <dt>Correo</dt>
            <dd>{perfil.correo}</dd>
          </div>
          <div>
            <dt>Documento</dt>
            <dd>
              {nombresDocumento[perfil.tipoDocumento] ?? perfil.tipoDocumento}{" "}
              {perfil.numeroDocumento}
            </dd>
          </div>
        </dl>
        <FormField
          label="Nombre"
          name="nombre"
          required
          maxLength={100}
          value={form.nombre}
          onChange={(e) => cambiar("nombre", e.target.value)}
        />
        <FormField
          label="Apellido"
          name="apellido"
          required
          maxLength={100}
          value={form.apellido}
          onChange={(e) => cambiar("apellido", e.target.value)}
        />
        <FormField
          label="Fecha de nacimiento"
          name="fechaNacimiento"
          type="date"
          required
          value={form.fechaNacimiento}
          onChange={(e) => cambiar("fechaNacimiento", e.target.value)}
        />
        <h2>Dirección de entrega</h2>
        <DireccionField api={api} value={form} onChange={setForm} />
        <div className="dm-actions">
          <button
            className="dm-button dm-button-primary"
            type="submit"
            disabled={guardando}
          >
            {guardando ? "Guardando…" : "Guardar mis datos"}
          </button>
        </div>
      </form>
      <CambioClaveForm />
    </div>
  );
}
