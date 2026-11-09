import { useEffect, useId, useState } from "react";

const maximoPorDefecto = 2 * 1024 * 1024;
const tiposPermitidos = ["image/jpeg", "image/png", "image/webp"];

/** Selector reusable con validación local y vista previa de JPG, PNG y WEBP. */
export default function ImageUploader({ onFileSelected, imageUrl = "", maxBytes = maximoPorDefecto }) {
  const inputId = useId();
  const [archivo, setArchivo] = useState(null);
  const [error, setError] = useState("");
  const [preview, setPreview] = useState("");

  useEffect(() => {
    if (!archivo) {
      setPreview("");
      return undefined;
    }
    const url = URL.createObjectURL(archivo);
    setPreview(url);
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
    if (!tiposPermitidos.includes(file.type)) {
      setError("El archivo debe ser JPG, PNG o WEBP.");
      setArchivo(null);
      onFileSelected?.(null);
      return;
    }
    if (file.size > maxBytes) {
      setError("La imagen no puede superar los 2 MB.");
      setArchivo(null);
      onFileSelected?.(null);
      return;
    }
    setArchivo(file);
    onFileSelected?.(file);
  }

  return (
    <div className="dm-field">
      <label htmlFor={inputId}>Foto de sucursal (JPG, PNG o WEBP; hasta 2 MB)</label>
      <input id={inputId} className="dm-location-select" type="file" accept="image/jpeg,image/png,image/webp" onChange={seleccionar} aria-describedby={error ? `${inputId}-error` : undefined} />
      {error && <small id={`${inputId}-error`} className="dm-error" role="alert">{error}</small>}
      {(preview || imageUrl) && <img className="e107-photo" src={preview || imageUrl} alt="Vista previa de la imagen seleccionada" />}
    </div>
  );
}
