import { useEffect, useId, useState } from "react";

const MAX_BYTES = 2 * 1024 * 1024;
const ACCEPTED_MIMES = new Set(["image/jpeg", "image/png", "image/webp"]);
const ACCEPT = ".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp";

export default function ImageUploader({ onFileSelected, label = "Imagen" }) {
  const inputId = useId();
  const [archivo, setArchivo] = useState(null);
  const [vistaPrevia, setVistaPrevia] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!archivo) {
      setVistaPrevia("");
      return undefined;
    }
    const url = URL.createObjectURL(archivo);
    setVistaPrevia(url);
    return () => URL.revokeObjectURL(url);
  }, [archivo]);

  function seleccionar(event) {
    const file = event.target.files?.[0] ?? null;
    setError("");
    if (!file) {
      setArchivo(null);
      onFileSelected?.(null);
      return;
    }
    const extensionPermitida = /\.(jpe?g|png|webp)$/i.test(file.name);
    if (!ACCEPTED_MIMES.has(file.type) && !(!file.type && extensionPermitida)) {
      setArchivo(null);
      setError("El archivo debe ser JPG, PNG o WEBP.");
      onFileSelected?.(null);
      event.target.value = "";
      return;
    }
    if (file.size > MAX_BYTES) {
      setArchivo(null);
      setError("La imagen no puede superar los 2 MB.");
      onFileSelected?.(null);
      event.target.value = "";
      return;
    }
    setArchivo(file);
    onFileSelected?.(file);
  }

  return (
    <section className="dm-image-uploader" aria-label={label}>
      <label className="dm-image-uploader__label" htmlFor={inputId}>
        {label}
      </label>
      <input
        id={inputId}
        type="file"
        accept={ACCEPT}
        onChange={seleccionar}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${inputId}-error` : undefined}
      />
      <p className="dm-image-uploader__hint">JPG, PNG o WEBP · hasta 2 MB.</p>
      {vistaPrevia && (
        <figure className="dm-image-uploader__preview">
          <img src={vistaPrevia} alt="Vista previa de la imagen seleccionada" />
          <figcaption>{archivo?.name}</figcaption>
        </figure>
      )}
      {error && (
        <p id={`${inputId}-error`} className="dm-image-uploader__error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
}
