// E4-08: etiquetas y tonos (StatusBadge) de los estados que ve el minorista, y formatos.

export const estadosPedido = {
  CARGADO: { etiqueta: "Recibido", tono: "neutral" },
  SIN_STOCK: { etiqueta: "Esperando stock", tono: "warning" },
  FACTURADO: { etiqueta: "Facturado", tono: "neutral" },
  EN_REPARTO: { etiqueta: "En reparto", tono: "warning" },
  ENTREGADO: { etiqueta: "Entregado", tono: "success" },
  ANULADO: { etiqueta: "Anulado", tono: "danger" },
};

export const estadosFactura = {
  PENDIENTE_PAGO: { etiqueta: "Pendiente de pago", tono: "warning" },
  PAGADA: { etiqueta: "Pagada", tono: "success" },
  ANULADA: { etiqueta: "Anulada", tono: "danger" },
};

// Recorrido normal del envío; SIN_STOCK y ANULADO solo aparecen si ocurrieron.
export const recorridoEnvio = [
  "CARGADO",
  "FACTURADO",
  "EN_REPARTO",
  "ENTREGADO",
];

const fechaHora = new Intl.DateTimeFormat("es-AR", {
  dateStyle: "short",
  timeStyle: "short",
});
const fecha = new Intl.DateTimeFormat("es-AR", { dateStyle: "short" });

export function formatearFechaHora(valor) {
  return valor ? fechaHora.format(new Date(valor)) : "";
}

// "2026-11-12" es una fecha sin hora: se arma en hora local para que no corra un día.
export function formatearFecha(valor) {
  if (!valor) return "";
  const [anio, mes, dia] = valor.split("-").map(Number);
  return fecha.format(new Date(anio, mes - 1, dia));
}
