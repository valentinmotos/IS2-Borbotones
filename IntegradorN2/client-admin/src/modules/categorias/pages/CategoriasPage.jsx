import { useCallback, useEffect, useRef, useState } from "react";
import { Plus } from "lucide-react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FormField,
  Pagination,
} from "../../../../../packages/design/components.jsx";
import { crearCrudApi } from "../../../lib/crudApi.js";
import "../styles.css";

/**
 * ABM de categorías (E0-08): pantalla de referencia para los demás ABM.
 * Listado de activas, alta y edición en el mismo formulario, baja lógica con confirmación.
 * Los errores del servidor con `campo` se muestran debajo del campo; el resto, en una alerta.
 */
const categoriasApi = crearCrudApi("/categorias");
const porPagina = 10;

export default function CategoriasPage() {
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [pagina, setPagina] = useState(1);
  const [editando, setEditando] = useState(null);
  const [nombre, setNombre] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const campoNombre = useRef(null);

  const cargar = useCallback(
    (signal) =>
      categoriasApi
        .listar({ signal })
        .then(setCategorias)
        .catch((error) => {
          if (!signal?.aborted)
            setAviso({ tono: "danger", texto: error.message });
        })
        .finally(() => {
          if (!signal?.aborted) setCargando(false);
        }),
    [],
  );

  useEffect(() => {
    const controlador = new AbortController();
    cargar(controlador.signal);
    return () => controlador.abort();
  }, [cargar]);

  const totalPaginas = Math.max(1, Math.ceil(categorias.length / porPagina));
  const paginaActual = Math.min(pagina, totalPaginas);
  const visibles = categorias.slice(
    (paginaActual - 1) * porPagina,
    paginaActual * porPagina,
  );

  function limpiarFormulario() {
    setEditando(null);
    setNombre("");
    setErrorNombre("");
  }

  function editar(categoria) {
    setEditando(categoria);
    setNombre(categoria.nombre);
    setErrorNombre("");
    setAviso(null);
    campoNombre.current?.focus();
  }

  async function guardar(event) {
    event.preventDefault();
    if (!nombre.trim()) {
      setErrorNombre("El nombre de la categoría es obligatorio.");
      campoNombre.current?.focus();
      return;
    }
    setGuardando(true);
    try {
      const datos = { nombre: nombre.trim() };
      const guardada = editando
        ? await categoriasApi.modificar(editando.id, datos)
        : await categoriasApi.crear(datos);
      setAviso({
        tono: "success",
        texto: editando
          ? `Modificaste «${guardada.nombre}».`
          : `Agregaste «${guardada.nombre}».`,
      });
      limpiarFormulario();
      await cargar();
    } catch (error) {
      if (error.campo === "nombre") {
        setErrorNombre(error.message);
        campoNombre.current?.focus();
      } else {
        setAviso({ tono: "danger", texto: error.message });
      }
    } finally {
      setGuardando(false);
    }
  }

  async function confirmarBaja() {
    const categoria = aEliminar;
    setAEliminar(null);
    try {
      await categoriasApi.eliminar(categoria.id);
      if (editando?.id === categoria.id) limpiarFormulario();
      setAviso({
        tono: "success",
        texto: `Diste de baja «${categoria.nombre}».`,
      });
      await cargar();
    } catch (error) {
      setAviso({ tono: "danger", texto: error.message });
    }
  }

  const columnas = [
    { key: "nombre", label: "Nombre" },
    {
      key: "acciones",
      label: "Acciones",
      render: (categoria) => (
        <div className="abm-row-actions">
          <button
            type="button"
            className="dm-button"
            aria-label={`Editar ${categoria.nombre}`}
            onClick={() => editar(categoria)}
          >
            Editar
          </button>
          <button
            type="button"
            className="dm-button dm-button-danger"
            aria-label={`Dar de baja ${categoria.nombre}`}
            onClick={() => setAEliminar(categoria)}
          >
            Dar de baja
          </button>
        </div>
      ),
    },
  ];

  return (
    <>
      <header className="page-head">
        <div>
          <span className="eyebrow">Catálogo</span>
          <h1>Categorías</h1>
          <p className="lead">
            Agrupan los productos del catálogo. No puede haber dos categorías
            activas con el mismo nombre.
          </p>
        </div>
        <div className="page-head-actions">
          <a
            className="dm-button dm-button-primary"
            href="#categoria-formulario"
          >
            <Plus size={16} aria-hidden="true" />
            Nueva categoría
          </a>
        </div>
      </header>
      <div className="design-content">
        <section className="design-register" aria-labelledby="listado">
          <div className="panel-head">
            <h2 id="listado">Categorías activas</h2>
            {!cargando && (
              <p>
                {categorias.length === 1
                  ? "1 categoría"
                  : `${categorias.length} categorías`}
              </p>
            )}
          </div>
          {aviso && (
            <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
              {aviso.texto}
            </Alert>
          )}
          <DataTable
            caption="Categorías activas"
            columns={columnas}
            rows={visibles}
            rowKey={(categoria) => categoria.id}
            empty={
              cargando
                ? "Cargando categorías…"
                : "Todavía no hay categorías. Creá la primera con el formulario."
            }
          />
          <Pagination
            page={paginaActual}
            pageCount={totalPaginas}
            onPageChange={setPagina}
          />
        </section>
        <form
          id="categoria-formulario"
          className="design-form"
          onSubmit={guardar}
          aria-labelledby="formulario"
          noValidate
        >
          <div>
            <h2 id="formulario">
              {editando ? "Editar categoría" : "Nueva categoría"}
            </h2>
            <p className="muted small">
              {editando
                ? `Estás cambiando el nombre de «${editando.nombre}».`
                : "Escribí el nombre y guardá para sumarla al catálogo."}
            </p>
          </div>
          <FormField
            ref={campoNombre}
            label="Nombre"
            name="nombre"
            hint="Por ejemplo, Alfajores."
            error={errorNombre}
            value={nombre}
            maxLength={120}
            onChange={(event) => {
              setNombre(event.target.value);
              setErrorNombre("");
            }}
          />
          <div className="dm-actions">
            <button
              className="dm-button dm-button-primary"
              type="submit"
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Guardar"}
            </button>
            {editando && (
              <button
                className="dm-button"
                type="button"
                onClick={limpiarFormulario}
              >
                Cancelar
              </button>
            )}
          </div>
        </form>
      </div>

      <ConfirmDialog
        open={aEliminar !== null}
        title="Dar de baja categoría"
        confirmLabel="Dar de baja"
        onConfirm={confirmarBaja}
        onCancel={() => setAEliminar(null)}
      >
        <p>¿Querés dar de baja «{aEliminar?.nombre}»?</p>
      </ConfirmDialog>
    </>
  );
}
