import { useEffect, useId, useRef, useState } from "react";

export function DataTable({
  caption,
  columns,
  rows,
  rowKey,
  empty = "No hay registros.",
}) {
  return (
    <div
      className="dm-table-scroll"
      role="region"
      aria-label={caption}
      tabIndex={0}
    >
      <table className="dm-table">
        <caption>{caption}</caption>
        <thead>
          <tr>
            {columns.map(({ key, label }) => (
              <th key={key} scope="col">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map(({ key, render }) => (
                <td key={key}>{render ? render(row) : row[key]}</td>
              ))}
            </tr>
          ))}
          {!rows.length && (
            <tr>
              <td colSpan={columns.length}>{empty}</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export function FormField({ label, hint, error, ...inputProps }) {
  const id = useId();
  return (
    <div className="dm-field">
      <label htmlFor={id}>{label}</label>
      <input
        {...inputProps}
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? id + "-message" : undefined}
      />
      {error || hint ? (
        <small id={id + "-message"} className={error ? "dm-error" : undefined}>
          {error || hint}
        </small>
      ) : null}
    </div>
  );
}

export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel = "Confirmar",
  onConfirm,
  onCancel,
}) {
  const dialog = useRef(null);
  const cancelButton = useRef(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const element = dialog.current;
    if (open && !element.open) {
      element.showModal();
      cancelButton.current.focus();
    } else if (!open && element.open) {
      element.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialog}
      className="dm-dialog"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      onCancel={(event) => {
        event.preventDefault();
        onCancel();
      }}
    >
      <h2 id={titleId}>{title}</h2>
      <div id={descriptionId}>{children}</div>
      <div className="dm-actions">
        <button
          ref={cancelButton}
          className="dm-button"
          type="button"
          onClick={onCancel}
        >
          Cancelar
        </button>
        <button
          className="dm-button dm-button-danger"
          type="button"
          onClick={onConfirm}
        >
          {confirmLabel}
        </button>
      </div>
    </dialog>
  );
}

export function StatusBadge({ tone = "neutral", children }) {
  return (
    <span className="dm-badge" data-tone={tone}>
      {children}
    </span>
  );
}

export function Kpi({ label, value, detail }) {
  return (
    <div className="dm-kpi">
      <dt>{label}</dt>
      <dd>{value}</dd>
      {detail && <dd className="dm-kpi-detail">{detail}</dd>}
    </div>
  );
}

export function Alert({ tone = "neutral", children, onDismiss }) {
  return (
    <div className="dm-alert" data-tone={tone}>
      <div role={tone === "danger" ? "alert" : "status"}>{children}</div>
      {onDismiss && (
        <button className="dm-button" type="button" onClick={onDismiss}>
          Cerrar aviso
        </button>
      )}
    </div>
  );
}

export function Pagination({ page, pageCount, onPageChange }) {
  if (pageCount < 2) return null;
  return (
    <nav className="dm-pagination" aria-label="Paginación">
      <button
        className="dm-button"
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
      >
        Anterior
      </button>
      <span aria-live="polite">
        Página {page} de {pageCount}
      </span>
      <button
        className="dm-button"
        type="button"
        disabled={page >= pageCount}
        onClick={() => onPageChange(page + 1)}
      >
        Siguiente
      </button>
    </nav>
  );
}

export function FilePicker({ label, accept, maxBytes, hint, onChange }) {
  const id = useId();
  const input = useRef(null);
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");

  function selectFile(nextFile) {
    let message = "";
    if (
      nextFile &&
      accept &&
      !accept.split(",").some((type) => {
        const rule = type.trim().toLowerCase();
        if (rule.startsWith("."))
          return nextFile.name.toLowerCase().endsWith(rule);
        if (rule.endsWith("/*"))
          return nextFile.type.startsWith(rule.slice(0, -1));
        return nextFile.type === rule;
      })
    ) {
      message = "El tipo de archivo no está permitido.";
    } else if (nextFile && maxBytes && nextFile.size > maxBytes) {
      message = "El archivo supera el tamaño permitido.";
    }

    const validFile = message ? null : nextFile;
    if (!validFile) input.current.value = "";
    setFile(validFile);
    setError(message);
    onChange(validFile, message);
  }

  return (
    <div className="dm-field">
      <label htmlFor={id}>{label}</label>
      <div className="dm-file-control">
        <span className="dm-button" aria-hidden="true">
          Elegir archivo
        </span>
        <input
          ref={input}
          id={id}
          type="file"
          accept={accept}
          aria-invalid={error ? true : undefined}
          aria-describedby={hint || error ? id + "-message" : undefined}
          onChange={(event) => selectFile(event.target.files[0] ?? null)}
        />
      </div>
      <small className="dm-file-name">
        {file?.name ?? "Sin archivo seleccionado"}
      </small>
      {hint || error ? (
        <small
          id={id + "-message"}
          className={error ? "dm-error" : undefined}
          aria-live="polite"
        >
          {error || hint}
        </small>
      ) : null}
      {(file || error) && (
        <button
          className="dm-button dm-file-clear"
          type="button"
          onClick={() => {
            selectFile(null);
            input.current.focus();
          }}
        >
          {file ? "Quitar archivo" : "Limpiar selección"}
        </button>
      )}
    </div>
  );
}
