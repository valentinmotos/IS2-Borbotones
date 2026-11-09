import { useRef, useState } from "react";
import {
  Alert,
  ConfirmDialog,
  DataTable,
  FilePicker,
  FormField,
  Kpi,
  Pagination,
  StatusBadge,
} from "../../../../packages/design/components.jsx";

const initialRows = [
  { id: 1, name: "Alfajores", active: true },
  { id: 2, name: "Chocolates", active: true },
  { id: 3, name: "Caramelos", active: false },
  { id: 4, name: "Gomitas", active: true },
  { id: 5, name: "Chicles", active: true },
];

const componentGuide = [
  {
    name: "Tabla",
    usage: "Revisá los nombres, estados y archivos del listado.",
  },
  {
    name: "Formulario",
    usage: "Enviá el nombre vacío o repetido para ver la validación.",
  },
  { name: "Modal de confirmación", usage: "Elegí Quitar en una fila." },
  {
    name: "Badge de estado",
    usage: "Compará las etiquetas Activa e Inactiva.",
  },
  { name: "KPI", usage: "Agregá o quitá una categoría y observá los totales." },
  {
    name: "Alerta",
    usage: "Guardá una categoría para ver el aviso y cerralo.",
  },
  {
    name: "Paginación",
    usage: "Usá Anterior y Siguiente para recorrer el listado.",
  },
  {
    name: "Selector de archivo",
    usage: "Elegí una imagen o probá un tipo de archivo inválido.",
  },
];

const pageSize = 3;
const imageLimit = 2 * 1024 * 1024;

export default function DesignPage() {
  const [rows, setRows] = useState(initialRows);
  const [page, setPage] = useState(1);
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState("");
  const [attachment, setAttachment] = useState({ file: null, error: "" });
  const [notice, setNotice] = useState(null);
  const [pending, setPending] = useState(null);
  const heading = useRef(null);
  const [formVersion, setFormVersion] = useState(0);
  const nextId = useRef(initialRows.length + 1);
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
  const visibleRows = rows.slice((page - 1) * pageSize, page * pageSize);

  function save(event) {
    event.preventDefault();
    const value = name.trim();
    if (
      !value ||
      rows.some(
        (row) =>
          row.name.localeCompare(value, "es-AR", { sensitivity: "base" }) === 0,
      )
    ) {
      setNameError(
        value
          ? "Ya existe una categoría con ese nombre."
          : "Ingresá un nombre.",
      );
      event.currentTarget.elements.nombre.focus();
      return;
    }
    if (attachment.error) {
      setNotice({
        text: "Corregí la selección del archivo antes de agregar la categoría.",
        tone: "danger",
      });
      return;
    }
    const id = nextId.current++;
    setRows([
      ...rows,
      { id, name: value, active: true, fileName: attachment.file?.name },
    ]);
    setPage(Math.ceil((rows.length + 1) / pageSize));
    setName("");
    setNameError("");
    setAttachment({ file: null, error: "" });
    setFormVersion(formVersion + 1);
    setNotice({ text: `Agregaste «${value}».`, tone: "success" });
  }

  function remove() {
    const remaining = rows.filter((row) => row.id !== pending.id);
    setRows(remaining);
    setPage(
      Math.min(page, Math.max(1, Math.ceil(remaining.length / pageSize))),
    );
    setNotice({ text: `Quitaste «${pending.name}».`, tone: "success" });
    setPending(null);
    requestAnimationFrame(() => heading.current?.focus());
  }

  const columns = [
    { key: "name", label: "Categoría" },
    {
      key: "status",
      label: "Estado",
      render: (row) => (
        <StatusBadge tone={row.active ? "success" : "warning"}>
          {row.active ? "Activa" : "Inactiva"}
        </StatusBadge>
      ),
    },
    {
      key: "action",
      label: "Acción",
      render: (row) => (
        <button
          type="button"
          className="dm-button"
          aria-label={`Quitar ${row.name}`}
          onClick={() => setPending(row)}
        >
          Quitar
        </button>
      ),
    },
    {
      key: "fileName",
      label: "Archivo",
      render: (row) => row.fileName ?? "Sin archivo",
    },
  ];

  return (
    <>
      <header className="dm-page-header">
        <div>
          <h1>Sistema de diseño</h1>
          <p>
            Probá los componentes con categorías de muestra. Los cambios duran
            hasta que salís de esta página.
          </p>
        </div>
      </header>
      <div className="design-content">
        <section className="design-register" aria-labelledby="listado">
          <h2 id="listado" ref={heading} tabIndex={-1}>
            Registro de muestra
          </h2>
          <dl className="design-totals">
            <Kpi label="Categorías" value={rows.length} />
            <Kpi
              label="Activas"
              value={rows.filter((row) => row.active).length}
            />
          </dl>
          {notice && (
            <Alert
              tone={notice.tone}
              onDismiss={() => {
                setNotice(null);
                heading.current?.focus();
              }}
            >
              {notice.text}
            </Alert>
          )}
          <DataTable
            caption="Listado de categorías"
            columns={columns}
            rows={visibleRows}
            rowKey={(row) => row.id}
          />
          <p className="design-scroll-note">
            Deslizá la tabla para ver el archivo.
          </p>
          <Pagination
            page={page}
            pageCount={pageCount}
            onPageChange={setPage}
          />
        </section>
        <form className="design-form" onSubmit={save} aria-labelledby="alta">
          <h2 id="alta">Agregar categoría</h2>
          <FormField
            label="Nombre"
            name="nombre"
            hint="Por ejemplo, confites."
            error={nameError}
            value={name}
            onChange={(event) => {
              setName(event.target.value);
              setNameError("");
            }}
            maxLength={60}
          />
          <FilePicker
            key={formVersion}
            label="Imagen (opcional)"
            accept="image/jpeg,image/png,image/webp"
            maxBytes={imageLimit}
            hint="JPG, PNG o WebP · hasta 2 MB."
            onChange={(file, error) => setAttachment({ file, error })}
          />
          <button className="dm-button dm-button-primary" type="submit">
            Agregar categoría
          </button>
        </form>
      </div>
      <details className="design-catalog">
        <summary>Referencia de componentes</summary>
        <DataTable
          caption="Componentes usados en esta página"
          columns={[
            { key: "name", label: "Componente" },
            { key: "usage", label: "Dónde probarlo" },
          ]}
          rows={componentGuide}
          rowKey={(row) => row.name}
        />
      </details>

      <ConfirmDialog
        open={pending !== null}
        title="Quitar categoría"
        confirmLabel="Quitar categoría"
        onConfirm={remove}
        onCancel={() => setPending(null)}
      >
        <p>¿Querés quitar «{pending?.name}» del listado de muestra?</p>
      </ConfirmDialog>
    </>
  );
}
