import { useEffect, useId, useState } from "react";

const MAX_BYTES = 2 * 1024 * 1024;
const ACCEPTED_MIMES = new Set(["image/jpeg", "image/png", "image/webp"]);
const ACCEPT = ".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp";

export default function ImageUploader({
  onFileSelected,
  label = "Imagen",
  imageUrl = "",
  maxBytes = MAX_BYTES,
}) {
  const inputId = useId();
  const [archivo, setArchivo] = useState(null);
  const [vistaPrevia, setVistaPrevia] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!vistaPrevia) return;
    return () => URL.revokeObjectURL(vistaPrevia);
  }, [vistaPrevia]);

  function seleccionar(event) {
    const file = event.target.files?.[0] ?? null;
    setError("");
    setVistaPrevia("");
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
    if (file.size > maxBytes) {
      setArchivo(null);
      setError(
        `La imagen no puede superar los ${maxBytes / (1024 * 1024)} MB.`,
      );
      onFileSelected?.(null);
      event.target.value = "";
      return;
    }
    setArchivo(file);
    setVistaPrevia(URL.createObjectURL(file));
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
      <p className="dm-image-uploader__hint">
        JPG, PNG o WEBP · hasta {maxBytes / (1024 * 1024)} MB.
      </p>
      {(vistaPrevia || imageUrl) && (
        <figure className="dm-image-uploader__preview">
          <img
            src={vistaPrevia || imageUrl}
            alt={
              vistaPrevia
                ? "Vista previa de la imagen seleccionada"
                : "Imagen guardada"
            }
          />
          {archivo && <figcaption>{archivo.name}</figcaption>}
        </figure>
      )}
      {error && (
        <p
          id={`${inputId}-error`}
          className="dm-image-uploader__error"
          role="alert"
        >
          {error}
        </p>
      )}
    </section>
  );
}
