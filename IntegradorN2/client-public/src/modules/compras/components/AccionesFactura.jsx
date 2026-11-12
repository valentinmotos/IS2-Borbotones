import { useState } from "react";
import { CreditCard, Download } from "lucide-react";
import { api } from "../../../lib/api.js";

// E4-08: acciones sobre la factura de una compra. Los errores se informan con onError(mensaje).

function nombreDesdeEncabezado(disposicion, porDefecto) {
  const coincidencia = /filename="?([^";]+)"?/i.exec(disposicion ?? "");
  return coincidencia ? coincidencia[1] : porDefecto;
}

// Descarga el PDF con el token (no sirve un <a href> directo porque la ruta exige Authorization).
export function DescargarFacturaButton({ compra, onError }) {
  const [descargando, setDescargando] = useState(false);

  async function descargar() {
    setDescargando(true);
    try {
      const respuesta = await api.get(
        `/minorista/facturas/${encodeURIComponent(compra.facturaId)}/pdf`,
        { responseType: "blob" },
      );
      const url = URL.createObjectURL(respuesta.data);
      const enlace = document.createElement("a");
      enlace.href = url;
      enlace.download = nombreDesdeEncabezado(
        respuesta.headers["content-disposition"],
        `factura-${compra.numeroFactura}.pdf`,
      );
      document.body.append(enlace);
      enlace.click();
      enlace.remove();
      setTimeout(() => URL.revokeObjectURL(url), 0);
    } catch (fallo) {
      onError?.(fallo.message);
    } finally {
      setDescargando(false);
    }
  }

  return (
    <button
      className="dm-button"
      type="button"
      onClick={descargar}
      disabled={descargando}
    >
      <Download size={15} aria-hidden="true" />
      {descargando ? "Descargando…" : "Descargar factura"}
    </button>
  );
}

// NOTA (E4-08): «Pagar ahora» usa el Checkout Pro de E4-05. El backend crea (o reutiliza) la
// orden por el saldo de la factura y devuelve checkoutUrl, una página de Mercado Pago a la que se
// redirige. Se muestra para toda factura impaga con saldo (compra.puedePagar): el pedido no guarda
// la forma de pago elegida, así que no se puede filtrar solo las "de billetera".
export function PagarAhoraButton({ compra, onError }) {
  const [iniciando, setIniciando] = useState(false);

  async function pagar() {
    setIniciando(true);
    try {
      const { data } = await api.post(
        `/minorista/facturas/${encodeURIComponent(compra.facturaId)}/checkout`,
      );
      window.location.assign(data.checkoutUrl);
    } catch (fallo) {
      onError?.(fallo.message);
      setIniciando(false);
    }
  }

  return (
    <button
      className="dm-button dm-button-primary"
      type="button"
      onClick={pagar}
      disabled={iniciando}
    >
      <CreditCard size={15} aria-hidden="true" />
      {iniciando ? "Abriendo el pago…" : "Pagar ahora"}
    </button>
  );
}
