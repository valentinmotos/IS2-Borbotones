import { useEffect, useState } from "react";
import { UserPlus } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FormField,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import "../styles.css";

const url = "/usuarios/internos";
const rolesCrear = ["ADMINISTRATIVO", "PREVENTOR", "DISTRIBUIDOR"];
const rolesEditar = ["JEFE", ...rolesCrear];
const personaVacia = {
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  tipoDocumento: "DNI",
  numeroDocumento: "",
  legajo: "",
};
const nombreRol = {
  JEFE: "Jefe",
  ADMINISTRATIVO: "Administrativo",
  PREVENTOR: "Preventor",
  DISTRIBUIDOR: "Distribuidor",
};

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState([]);
  const [correo, setCorreo] = useState("");
  const [rol, setRol] = useState(rolesCrear[0]);
  const [persona, setPersona] = useState(personaVacia);
  const [editando, setEditando] = useState(null);
  const [confirmacion, setConfirmacion] = useState(null);
  const [aviso, setAviso] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  const [version, setVersion] = useState(0);
  const cargar = () => setVersion((v) => v + 1);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;
    api
      .get(url, { signal })
      .then(
        (respuesta) => {
          if (!signal.aborted) setUsuarios(respuesta.data);
        },
        (error) => {
          if (!signal.aborted)
            setAviso({ tono: "danger", texto: error.message });
        },
      )
      .finally(() => {
        if (!signal.aborted) setCargando(false);
      });
    return () => controller.abort();
  }, [version]);

  function limpiar() {
    setCorreo("");
    setRol(rolesCrear[0]);
    setPersona(personaVacia);
    setEditando(null);
  }

  function editar(usuario) {
    setEditando(usuario);
    setCorreo(usuario.correo);
    setRol(usuario.rol);
    setPersona({
      nombre: usuario.nombre ?? "",
      apellido: usuario.apellido ?? "",
      fechaNacimiento: usuario.fechaNacimiento ?? "",
      tipoDocumento: usuario.tipoDocumento ?? "DNI",
      numeroDocumento: usuario.numeroDocumento ?? "",
      legajo: usuario.legajo ?? "",
    });
    setAviso(null);
  }

  async function guardar(event) {
    event.preventDefault();
    setGuardando(true);
    setAviso(null);
    try {
      const datos = {
        correo: correo.trim(),
        rol,
        ...(["PREVENTOR", "DISTRIBUIDOR"].includes(rol) ? persona : {}),
      };
      const respuesta = editando
        ? await api.put(`${url}/${encodeURIComponent(editando.id)}`, datos)
        : await api.post(url, datos);
      limpiar();
      setAviso({
        tono: "success",
        texto: editando
          ? `Actualizaste la cuenta ${respuesta.data.correo}.`
          : `Creaste la cuenta ${respuesta.data.correo}. Su clave inicial es «dulcemayor» y deberá cambiarla al ingresar.`,
      });
      cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  async function resetear(usuario) {
    try {
      await api.post(`${url}/${encodeURIComponent(usuario.id)}/resetear-clave`);
      setAviso({
        tono: "success",
        texto: `La clave de ${usuario.correo} se restableció a «dulcemayor»; deberá cambiarla al ingresar.`,
      });
      cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  async function darDeBaja() {
    const usuario = confirmacion;
    setConfirmacion(null);
    try {
      await api.delete(`${url}/${encodeURIComponent(usuario.id)}`);
      if (editando?.id === usuario.id) limpiar();
      setAviso({
        tono: "success",
        texto: `Diste de baja la cuenta ${usuario.correo}.`,
      });
      cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  const columnas = [
    { key: "correo", label: "Correo" },
    {
      key: "nombre",
      label: "Nombre",
      render: (usuario) =>
        usuario.nombre ? `${usuario.nombre} ${usuario.apellido}` : "—",
    },
    {
      key: "rol",
      label: "Rol",
      render: (usuario) => nombreRol[usuario.rol] ?? usuario.rol,
    },
    {
      key: "estado",
      label: "Estado",
      render: (usuario) => (usuario.activo ? "Activo" : "Inactivo"),
    },
    {
      key: "clave",
      label: "Acceso",
      render: (usuario) =>
        usuario.claveTemporal ? "Debe cambiar la clave" : "Clave personal",
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (usuario) => (
        <div className="abm-row-actions">
          <button
            className="dm-button"
            type="button"
            onClick={() => editar(usuario)}
          >
            Editar rol
          </button>
          <button
            className="dm-button"
            type="button"
            onClick={() => resetear(usuario)}
          >
            Restablecer clave
          </button>
          <button
            className="dm-button dm-button-danger"
            type="button"
            onClick={() => setConfirmacion(usuario)}
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="usuarios-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Configuración</span>
          <h1>Usuarios internos</h1>
          <p className="lead">
            Creá y administrá las cuentas del equipo. Solo Jefatura puede usar
            esta sección.
          </p>
        </div>
        <div className="page-head-actions">
          <a className="dm-button dm-button-primary" href="#usuario-formulario">
            <UserPlus size={16} aria-hidden="true" />
            Crear usuario
          </a>
        </div>
      </header>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}
      <section className="panel" aria-labelledby="usuarios-titulo">
        <div className="panel-head">
          <h2 id="usuarios-titulo">Usuarios registrados</h2>
          {!cargando && (
            <p>
              {usuarios.length === 1
                ? "1 cuenta"
                : `${usuarios.length} cuentas`}
            </p>
          )}
        </div>
        <DataTable
          caption="Usuarios internos"
          columns={columnas}
          rows={usuarios}
          rowKey={(usuario) => usuario.id}
          empty={
            cargando
              ? "Cargando usuarios…"
              : "No hay usuarios internos. Creá la primera cuenta con el formulario."
          }
        />
      </section>
      <form
        id="usuario-formulario"
        className="panel"
        onSubmit={guardar}
        noValidate
        aria-labelledby="usuario-formulario-titulo"
      >
        <div className="panel-head">
          <h2 id="usuario-formulario-titulo">
            {editando ? `Editar usuario: ${editando.correo}` : "Crear usuario"}
          </h2>
          <p>
            Las cuentas nuevas reciben la clave temporal{" "}
            <strong>dulcemayor</strong> y deben cambiarla al ingresar.
          </p>
        </div>
        <div className="form-grid">
          <FormField
            label="Correo electrónico"
            name="correo"
            type="email"
            autoComplete="email"
            maxLength={254}
            required
            value={correo}
            onChange={(event) => setCorreo(event.target.value)}
          />
          <label className="dm-field" htmlFor="usuario-rol">
            <span>Rol</span>
            <select
              className="dm-input"
              id="usuario-rol"
              value={rol}
              onChange={(event) => setRol(event.target.value)}
            >
              {(editando ? rolesEditar : rolesCrear).map((opcion) => (
                <option key={opcion} value={opcion}>
                  {nombreRol[opcion]}
                </option>
              ))}
            </select>
          </label>
          {["PREVENTOR", "DISTRIBUIDOR"].includes(rol) && (
            <fieldset className="usuarios-perfil">
              <legend>Datos del perfil de empleado</legend>
              <div className="form-grid">
                <FormField
                  label="Nombre"
                  name="nombre"
                  autoComplete="given-name"
                  maxLength={100}
                  required
                  value={persona.nombre}
                  onChange={(event) =>
                    setPersona({ ...persona, nombre: event.target.value })
                  }
                />
                <FormField
                  label="Apellido"
                  name="apellido"
                  autoComplete="family-name"
                  maxLength={100}
                  required
                  value={persona.apellido}
                  onChange={(event) =>
                    setPersona({ ...persona, apellido: event.target.value })
                  }
                />
                <FormField
                  label="Fecha de nacimiento"
                  name="fechaNacimiento"
                  type="date"
                  autoComplete="bday"
                  required
                  value={persona.fechaNacimiento}
                  onChange={(event) =>
                    setPersona({
                      ...persona,
                      fechaNacimiento: event.target.value,
                    })
                  }
                />
                <label className="dm-field" htmlFor="usuario-tipo-documento">
                  <span>Tipo de documento</span>
                  <select
                    className="dm-input"
                    id="usuario-tipo-documento"
                    value={persona.tipoDocumento}
                    onChange={(event) =>
                      setPersona({
                        ...persona,
                        tipoDocumento: event.target.value,
                      })
                    }
                  >
                    <option value="DNI">DNI</option>
                    <option value="CUIT">CUIT</option>
                    <option value="PASAPORTE">Pasaporte</option>
                  </select>
                </label>
                <FormField
                  label="Número de documento"
                  name="numeroDocumento"
                  maxLength={30}
                  required
                  value={persona.numeroDocumento}
                  onChange={(event) =>
                    setPersona({
                      ...persona,
                      numeroDocumento: event.target.value,
                    })
                  }
                />
                <FormField
                  label="Legajo"
                  name="legajo"
                  maxLength={40}
                  required
                  value={persona.legajo}
                  onChange={(event) =>
                    setPersona({ ...persona, legajo: event.target.value })
                  }
                />
              </div>
            </fieldset>
          )}
          <div className="form-actions">
            <button
              className="dm-button dm-button-primary"
              disabled={guardando}
            >
              {guardando
                ? "Guardando…"
                : editando
                  ? "Guardar cambios"
                  : "Crear usuario"}
            </button>
            {editando && (
              <button className="dm-button" type="button" onClick={limpiar}>
                Cancelar
              </button>
            )}
          </div>
        </div>
      </form>
      <ConfirmDialog
        open={confirmacion !== null}
        title="Dar de baja usuario"
        confirmLabel="Dar de baja"
        onConfirm={darDeBaja}
        onCancel={() => setConfirmacion(null)}
      >
        <p>
          ¿Querés dar de baja la cuenta de {confirmacion?.correo}? Esta
          operación es lógica y no elimina su historial.
        </p>
      </ConfirmDialog>
    </div>
  );
}
