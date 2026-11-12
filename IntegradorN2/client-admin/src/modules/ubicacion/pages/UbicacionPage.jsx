import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Plus } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FormField,
  Pagination,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { crearCrudApi } from "../../../lib/crudApi.js";
import "../styles.css";

const departamentosApi = crearCrudApi("/departamentos");
const localidadesApi = crearCrudApi("/localidades");
const porPagina = 10;

function SelectField({ label, error, hint, ...selectProps }) {
  const id = `ubicacion-${selectProps.name}`;
  const messageId = `${id}-message`;
  return (
    <div className="dm-field">
      <label htmlFor={id}>{label}</label>
      <select
        {...selectProps}
        id={id}
        className="dm-location-select"
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? messageId : undefined}
      />
      {error || hint ? (
        <small id={messageId} className={error ? "dm-error" : undefined}>
          {error || hint}
        </small>
      ) : null}
    </div>
  );
}

export default function UbicacionPage() {
  const [departamentos, setDepartamentos] = useState([]);
  const [localidades, setLocalidades] = useState([]);
  const [cargandoDepartamentos, setCargandoDepartamentos] = useState(true);
  const [cargandoLocalidades, setCargandoLocalidades] = useState(false);
  const [departamentoFiltro, setDepartamentoFiltro] = useState("");
  const [departamentoId, setDepartamentoId] = useState("");
  const [nombreDepartamento, setNombreDepartamento] = useState("");
  const [nombreLocalidad, setNombreLocalidad] = useState("");
  const [codigoPostal, setCodigoPostal] = useState("");
  const [editandoDepartamento, setEditandoDepartamento] = useState(null);
  const [editandoLocalidad, setEditandoLocalidad] = useState(null);
  const [errorDepartamento, setErrorDepartamento] = useState("");
  const [erroresLocalidad, setErroresLocalidad] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const [paginaDepartamentos, setPaginaDepartamentos] = useState(1);
  const [paginaLocalidades, setPaginaLocalidades] = useState(1);
  const campoDepartamento = useRef(null);
  const campoLocalidad = useRef(null);
  const campoCodigoPostal = useRef(null);

  const cargarDepartamentos = useCallback((signal) => {
    return departamentosApi
      .listar({ signal })
      .then((datos) => {
        setDepartamentos(datos);
        const seleccionarValido = (actual) =>
          datos.some((departamento) => departamento.id === actual)
            ? actual
            : (datos[0]?.id ?? "");
        setDepartamentoFiltro(seleccionarValido);
        setDepartamentoId(seleccionarValido);
        return datos;
      })
      .catch((error) => {
        if (!signal?.aborted)
          setAviso({ tono: "danger", texto: error.message });
        return [];
      })
      .finally(() => {
        if (!signal?.aborted) setCargandoDepartamentos(false);
      });
  }, []);

  useEffect(() => {
    const controlador = new AbortController();
    cargarDepartamentos(controlador.signal);
    return () => controlador.abort();
  }, [cargarDepartamentos]);

  useEffect(() => {
    if (!departamentoFiltro) {
      setLocalidades([]);
      setCargandoLocalidades(false);
      return undefined;
    }
    const controlador = new AbortController();
    setCargandoLocalidades(true);
    api
      .get(
        `/departamentos/${encodeURIComponent(departamentoFiltro)}/localidades`,
        {
          signal: controlador.signal,
        },
      )
      .then((respuesta) => setLocalidades(respuesta.data))
      .catch((error) => {
        if (!controlador.signal.aborted)
          setAviso({ tono: "danger", texto: error.message });
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCargandoLocalidades(false);
      });
    return () => controlador.abort();
  }, [departamentoFiltro]);

  const departamentosVisibles = useMemo(() => {
    const total = Math.max(1, Math.ceil(departamentos.length / porPagina));
    const pagina = Math.min(paginaDepartamentos, total);
    return {
      pagina,
      total,
      filas: departamentos.slice((pagina - 1) * porPagina, pagina * porPagina),
    };
  }, [departamentos, paginaDepartamentos]);
  const localidadesVisibles = useMemo(() => {
    const total = Math.max(1, Math.ceil(localidades.length / porPagina));
    const pagina = Math.min(paginaLocalidades, total);
    return {
      pagina,
      total,
      filas: localidades.slice((pagina - 1) * porPagina, pagina * porPagina),
    };
  }, [localidades, paginaLocalidades]);

  function seleccionarDepartamento(id) {
    setDepartamentoFiltro(id);
    setDepartamentoId(id);
    setPaginaLocalidades(1);
  }

  function limpiarDepartamento() {
    setEditandoDepartamento(null);
    setNombreDepartamento("");
    setErrorDepartamento("");
  }

  function limpiarLocalidad(departamentoPredeterminado = departamentoFiltro) {
    setEditandoLocalidad(null);
    setNombreLocalidad("");
    setCodigoPostal("");
    setDepartamentoId(departamentoPredeterminado);
    setErroresLocalidad({});
  }

  function editarDepartamento(departamento) {
    setEditandoDepartamento(departamento);
    setNombreDepartamento(departamento.nombre);
    setErrorDepartamento("");
    setAviso(null);
    campoDepartamento.current?.focus();
  }

  function editarLocalidad(localidad) {
    setEditandoLocalidad(localidad);
    setNombreLocalidad(localidad.nombre);
    setCodigoPostal(localidad.codigoPostal);
    seleccionarDepartamento(localidad.departamentoId);
    setErroresLocalidad({});
    setAviso(null);
    campoLocalidad.current?.focus();
  }

  async function guardarDepartamento(event) {
    event.preventDefault();
    if (!nombreDepartamento.trim()) {
      setErrorDepartamento("El nombre del departamento es obligatorio.");
      campoDepartamento.current?.focus();
      return;
    }
    setGuardando(true);
    try {
      const datos = { nombre: nombreDepartamento.trim() };
      const guardado = editandoDepartamento
        ? await departamentosApi.modificar(editandoDepartamento.id, datos)
        : await departamentosApi.crear(datos);
      setAviso({
        tono: "success",
        texto: editandoDepartamento
          ? `Modificaste «${guardado.nombre}».`
          : `Agregaste «${guardado.nombre}».`,
      });
      limpiarDepartamento();
      await cargarDepartamentos();
    } catch (error) {
      if (error.campo === "nombre") {
        setErrorDepartamento(error.message);
        campoDepartamento.current?.focus();
      } else setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  async function guardarLocalidad(event) {
    event.preventDefault();
    const errores = {};
    if (!nombreLocalidad.trim())
      errores.nombre = "El nombre de la localidad es obligatorio.";
    if (!/^\d{4}$/.test(codigoPostal.trim()))
      errores.codigoPostal = "El código postal debe tener 4 dígitos.";
    if (!departamentoId)
      errores.departamentoId = "Debés seleccionar un departamento.";
    if (Object.keys(errores).length) {
      setErroresLocalidad(errores);
      if (errores.nombre) campoLocalidad.current?.focus();
      else if (errores.codigoPostal) campoCodigoPostal.current?.focus();
      return;
    }
    setGuardando(true);
    try {
      const datos = {
        nombre: nombreLocalidad.trim(),
        codigoPostal: codigoPostal.trim(),
        departamentoId,
      };
      const guardada = editandoLocalidad
        ? await localidadesApi.modificar(editandoLocalidad.id, datos)
        : await localidadesApi.crear(datos);
      setAviso({
        tono: "success",
        texto: editandoLocalidad
          ? `Modificaste «${guardada.nombre}».`
          : `Agregaste «${guardada.nombre}».`,
      });
      seleccionarDepartamento(guardada.departamentoId);
      limpiarLocalidad(guardada.departamentoId);
      await cargarDepartamentos();
      const respuesta = await api.get(
        `/departamentos/${encodeURIComponent(guardada.departamentoId)}/localidades`,
      );
      setLocalidades(respuesta.data);
    } catch (error) {
      const campo = error.campo;
      if (["nombre", "codigoPostal", "departamentoId"].includes(campo)) {
        setErroresLocalidad((actuales) => ({
          ...actuales,
          [campo]: error.message,
        }));
        if (campo === "nombre") campoLocalidad.current?.focus();
        if (campo === "codigoPostal") campoCodigoPostal.current?.focus();
      } else setAviso({ tono: "danger", texto: error.message });
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarBaja() {
    const objetivo = aEliminar;
    setAEliminar(null);
    try {
      if (objetivo.tipo === "departamento") {
        await departamentosApi.eliminar(objetivo.registro.id);
        if (editandoDepartamento?.id === objetivo.registro.id)
          limpiarDepartamento();
        if (editandoLocalidad?.departamentoId === objetivo.registro.id)
          limpiarLocalidad();
        setAviso({
          tono: "success",
          texto: `Diste de baja «${objetivo.registro.nombre}».`,
        });
        await cargarDepartamentos();
      } else {
        await localidadesApi.eliminar(objetivo.registro.id);
        if (editandoLocalidad?.id === objetivo.registro.id) limpiarLocalidad();
        setAviso({
          tono: "success",
          texto: `Diste de baja «${objetivo.registro.nombre}».`,
        });
        const respuesta = await api.get(
          `/departamentos/${encodeURIComponent(departamentoFiltro)}/localidades`,
        );
        setLocalidades(respuesta.data);
      }
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  const columnasDepartamentos = [
    { key: "nombre", label: "Departamento" },
    {
      key: "acciones",
      label: "Acciones",
      render: (departamento) => (
        <div className="abm-row-actions">
          <button
            type="button"
            className="dm-button"
            aria-label={`Ver localidades de ${departamento.nombre}`}
            onClick={() => seleccionarDepartamento(departamento.id)}
          >
            Localidades
          </button>
          <button
            type="button"
            className="dm-button"
            aria-label={`Editar ${departamento.nombre}`}
            onClick={() => editarDepartamento(departamento)}
          >
            Editar
          </button>
          <button
            type="button"
            className="dm-button dm-button-danger"
            aria-label={`Dar de baja ${departamento.nombre}`}
            onClick={() =>
              setAEliminar({ tipo: "departamento", registro: departamento })
            }
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];

  const columnasLocalidades = [
    { key: "nombre", label: "Localidad" },
    { key: "codigoPostal", label: "Código postal" },
    {
      key: "acciones",
      label: "Acciones",
      render: (localidad) => (
        <div className="abm-row-actions">
          <button
            type="button"
            className="dm-button"
            aria-label={`Editar ${localidad.nombre}`}
            onClick={() => editarLocalidad(localidad)}
          >
            Editar
          </button>
          <button
            type="button"
            className="dm-button dm-button-danger"
            aria-label={`Dar de baja ${localidad.nombre}`}
            onClick={() =>
              setAEliminar({ tipo: "localidad", registro: localidad })
            }
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];

  const departamentoElegido = departamentos.find(
    (item) => item.id === departamentoFiltro,
  );

  return (
    <div className="ubicacion-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Configuración</span>
          <h1>Ubicación</h1>
          <p className="lead">
            Organizá los departamentos y las localidades que se usan en las
            direcciones de sucursales y comercios.
          </p>
        </div>
        <div className="page-head-actions">
          <a className="dm-button" href="#departamento-formulario">
            Nuevo departamento
          </a>
          <a
            className="dm-button dm-button-primary"
            href="#localidad-formulario"
          >
            <Plus size={16} aria-hidden="true" />
            Nueva localidad
          </a>
        </div>
      </header>

      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.texto}
        </Alert>
      )}

      <section
        className="design-content"
        aria-labelledby="departamentos-titulo"
      >
        <div className="design-register">
          <div className="panel-head">
            <h2 id="departamentos-titulo">Departamentos</h2>
            {!cargandoDepartamentos && (
              <p>
                {departamentos.length === 1
                  ? "1 departamento"
                  : `${departamentos.length} departamentos`}
              </p>
            )}
          </div>
          <DataTable
            caption="Departamentos activos"
            columns={columnasDepartamentos}
            rows={departamentosVisibles.filas}
            rowKey={(departamento) => departamento.id}
            empty={
              cargandoDepartamentos
                ? "Cargando departamentos…"
                : "No hay departamentos. Creá el primero con el formulario."
            }
          />
          <Pagination
            page={departamentosVisibles.pagina}
            pageCount={departamentosVisibles.total}
            onPageChange={setPaginaDepartamentos}
          />
        </div>
        <form
          id="departamento-formulario"
          className="design-form"
          onSubmit={guardarDepartamento}
          noValidate
        >
          <div>
            <h2>
              {editandoDepartamento
                ? "Editar departamento"
                : "Nuevo departamento"}
            </h2>
            <p className="muted small">
              {editandoDepartamento
                ? `Estás cambiando el nombre de «${editandoDepartamento.nombre}».`
                : "Después vas a poder cargarle localidades."}
            </p>
          </div>
          <FormField
            ref={campoDepartamento}
            label="Nombre"
            name="nombreDepartamento"
            hint="Por ejemplo, Guaymallén."
            error={errorDepartamento}
            value={nombreDepartamento}
            maxLength={120}
            onChange={(event) => {
              setNombreDepartamento(event.target.value);
              setErrorDepartamento("");
            }}
          />
          <div className="dm-actions">
            <button
              className="dm-button dm-button-primary"
              type="submit"
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Guardar departamento"}
            </button>
            {editandoDepartamento && (
              <button
                className="dm-button"
                type="button"
                onClick={limpiarDepartamento}
              >
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="design-content" aria-labelledby="localidades-titulo">
        <div className="design-register">
          <div className="panel-head">
            <h2 id="localidades-titulo">Localidades</h2>
            <p>Elegí un departamento para ver sus localidades.</p>
          </div>
          <div className="toolbar">
            <SelectField
              label="Filtrar por departamento"
              name="filtroDepartamento"
              value={departamentoFiltro}
              onChange={(event) => seleccionarDepartamento(event.target.value)}
            >
              <option value="">Seleccioná un departamento</option>
              {departamentos.map((departamento) => (
                <option key={departamento.id} value={departamento.id}>
                  {departamento.nombre}
                </option>
              ))}
            </SelectField>
          </div>
          <DataTable
            caption={`Localidades activas${departamentoElegido ? ` · ${departamentoElegido.nombre}` : ""}`}
            columns={columnasLocalidades}
            rows={localidadesVisibles.filas}
            rowKey={(localidad) => localidad.id}
            empty={
              cargandoLocalidades
                ? "Cargando localidades…"
                : departamentoElegido
                  ? "No hay localidades en este departamento."
                  : "Seleccioná un departamento para ver sus localidades."
            }
          />
          <Pagination
            page={localidadesVisibles.pagina}
            pageCount={localidadesVisibles.total}
            onPageChange={setPaginaLocalidades}
          />
        </div>
        <form
          id="localidad-formulario"
          className="design-form"
          onSubmit={guardarLocalidad}
          noValidate
        >
          <div>
            <h2>
              {editandoLocalidad ? "Editar localidad" : "Nueva localidad"}
            </h2>
            <p className="muted small">
              {editandoLocalidad
                ? `Estás editando «${editandoLocalidad.nombre}».`
                : "Completá el departamento, el nombre y el código postal."}
            </p>
          </div>
          <SelectField
            label="Departamento"
            name="departamentoId"
            value={departamentoId}
            error={erroresLocalidad.departamentoId}
            hint="La localidad queda asociada a este departamento."
            onChange={(event) => {
              seleccionarDepartamento(event.target.value);
              setErroresLocalidad((actuales) => ({
                ...actuales,
                departamentoId: "",
              }));
            }}
          >
            <option value="">Seleccioná un departamento</option>
            {departamentos.map((departamento) => (
              <option key={departamento.id} value={departamento.id}>
                {departamento.nombre}
              </option>
            ))}
          </SelectField>
          <FormField
            ref={campoLocalidad}
            label="Nombre de la localidad"
            name="nombreLocalidad"
            error={erroresLocalidad.nombre}
            value={nombreLocalidad}
            maxLength={120}
            onChange={(event) => {
              setNombreLocalidad(event.target.value);
              setErroresLocalidad((actuales) => ({ ...actuales, nombre: "" }));
            }}
          />
          <FormField
            ref={campoCodigoPostal}
            label="Código postal"
            name="codigoPostal"
            inputMode="numeric"
            pattern="[0-9]{4}"
            maxLength={4}
            hint="Ingresá los 4 dígitos del código postal."
            error={erroresLocalidad.codigoPostal}
            value={codigoPostal}
            onChange={(event) => {
              setCodigoPostal(
                event.target.value.replace(/\D/g, "").slice(0, 4),
              );
              setErroresLocalidad((actuales) => ({
                ...actuales,
                codigoPostal: "",
              }));
            }}
          />
          <div className="dm-actions">
            <button
              className="dm-button dm-button-primary"
              type="submit"
              disabled={guardando || !departamentos.length}
            >
              {guardando ? "Guardando…" : "Guardar localidad"}
            </button>
            {editandoLocalidad && (
              <button
                className="dm-button"
                type="button"
                onClick={limpiarLocalidad}
              >
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>

      <ConfirmDialog
        open={aEliminar !== null}
        title={`Dar de baja ${aEliminar?.tipo ?? "registro"}`}
        confirmLabel="Dar de baja"
        onConfirm={confirmarBaja}
        onCancel={() => setAEliminar(null)}
      >
        <p>¿Querés dar de baja «{aEliminar?.registro.nombre}»?</p>
      </ConfirmDialog>
    </div>
  );
}
