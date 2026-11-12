import { useEffect, useRef, useState } from "react";
import { X } from "lucide-react";
import {
  Alert,
  DataTable,
  FormField,
  Kpi,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";

const formatoPrecio = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});

function hoyLocal() {
  const hoy = new Date();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${hoy.getFullYear()}-${mes}-${dia}`;
}

function formatoFecha(fecha) {
  if (!fecha) return "—";
  const [anio, mes, dia] = fecha.split("-");
  return `${dia}/${mes}/${anio}`;
}

const columnas = [
  {
    key: "fechaDesde",
    label: "Desde",
    render: (v) => formatoFecha(v.fechaDesde),
  },
  {
    key: "fechaHasta",
    label: "Hasta",
    render: (v) => (v.fechaHasta ? formatoFecha(v.fechaHasta) : "Sin cierre"),
  },
  {
    key: "precio",
    label: "Precio",
    render: (v) => formatoPrecio.format(v.precio),
  },
  {
    key: "estado",
    label: "Estado",
    render: (v) =>
      v.vigente ? (
        <StatusBadge tone="success">Vigente</StatusBadge>
      ) : (
        <StatusBadge>Cerrado</StatusBadge>
      ),
  },
];

/**
 * E2-03: historial de precios mayoristas de un producto y alta de un precio nuevo.
 * Al cargar un precio, el servidor cierra el vigente el día anterior a la nueva fecha desde.
 */
export default function PreciosProducto({ producto, onClose }) {
  const recurso = `/productos/${producto.id}/precios`;
  const [resultado, setResultado] = useState(null);
  const [version, setVersion] = useState(0);
  const [versionFormulario, setVersionFormulario] = useState(0);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);
  const titulo = useRef(null);

  useEffect(() => {
    titulo.current?.focus();
  }, []);

  useEffect(() => {
    const solicitud = new AbortController();
    api.get(recurso, { signal: solicitud.signal }).then(
      ({ data }) => {
        if (!solicitud.signal.aborted) setResultado({ version, data });
      },
      (failure) => {
        if (!solicitud.signal.aborted)
          setResultado({ version, error: failure.message });
      },
    );
    return () => solicitud.abort();
  }, [recurso, version]);

  const actual = resultado?.version === version ? resultado : null;
  const historial = actual?.data ?? [];
  const vigente = historial.find((v) => v.vigente);

  async function guardar(event) {
    event.preventDefault();
    if (guardando) return;
    const datos = new FormData(event.currentTarget);
    setGuardando(true);
    setError(null);
    setAviso(null);
    try {
      const creada = await api
        .post(recurso, {
          fechaDesde: datos.get("fechaDesde"),
          precio: Number(datos.get("precio")),
        })
        .then((r) => r.data);
      setAviso(
        `Cargaste ${formatoPrecio.format(creada.precio)} desde el ${formatoFecha(creada.fechaDesde)}.`,
      );
      setVersionFormulario((anterior) => anterior + 1);
      setVersion((anterior) => anterior + 1);
    } catch (failure) {
      setError(failure);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section
      className="panel precios-producto"
      aria-labelledby="precios-producto-titulo"
    >
      <div className="panel-head">
        <div className="precios-producto-titulo">
          <span className="eyebrow">Producto seleccionado</span>
          <h2 id="precios-producto-titulo" ref={titulo} tabIndex={-1}>
            Precios de {producto.nombre}
          </h2>
          <p>
            {producto.codigo} · {producto.presentacion} ·{" "}
            {producto.unidadesPorBulto} u.
          </p>
        </div>
        <button className="dm-button" type="button" onClick={onClose}>
          <X size={16} aria-hidden="true" />
          Cerrar precios
        </button>
      </div>
      {aviso && (
        <Alert tone="success" onDismiss={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}
      {actual?.error && <Alert tone="danger">{actual.error}</Alert>}
      <div className="precios-producto-cuerpo">
        <div className="precios-producto-historial">
          <dl className="precios-producto-kpi">
            <Kpi
              label="Precio vigente"
              value={
                vigente ? formatoPrecio.format(vigente.precio) : "Sin precio"
              }
              detail={
                vigente
                  ? `Desde el ${formatoFecha(vigente.fechaDesde)}`
                  : undefined
              }
            />
          </dl>
          <DataTable
            caption={`Historial de precios de ${producto.nombre}`}
            columns={columnas}
            rows={historial}
            rowKey={(v) => v.id}
            empty={
              !actual
                ? "Cargando precios…"
                : actual.error
                  ? "Historial no disponible."
                  : "Este producto todavía no tiene precios. Cargá el primero."
            }
          />
        </div>
        <form
          key={versionFormulario}
          className="precios-producto-form"
          onSubmit={guardar}
          aria-labelledby="precio-nuevo-titulo"
        >
          <h3 id="precio-nuevo-titulo">Nuevo precio</h3>
          {error && <Alert tone="danger">{error.message}</Alert>}
          <fieldset disabled={guardando}>
            <div className="form-grid precios-producto-campos">
              <FormField
                name="fechaDesde"
                type="date"
                label="Vigente desde"
                required
                min={hoyLocal()}
                defaultValue={hoyLocal()}
                hint="El precio anterior se cierra el día antes."
                error={
                  error?.campo === "fechaDesde" ? error.message : undefined
                }
              />
              <FormField
                name="precio"
                type="number"
                label="Precio mayorista ($)"
                required
                min={0.01}
                step={0.01}
                error={error?.campo === "precio" ? error.message : undefined}
              />
              <div className="form-actions">
                <button className="dm-button dm-button-primary" type="submit">
                  {guardando ? "Guardando…" : "Guardar precio"}
                </button>
              </div>
            </div>
          </fieldset>
        </form>
      </div>
    </section>
  );
}
