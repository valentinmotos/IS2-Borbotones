export const estadosPedido = {
  CARGADO: ["Cargado", "neutral"],
  SIN_STOCK: ["Sin stock", "warning"],
  FACTURADO: ["Facturado", "success"],
  EN_REPARTO: ["En reparto", "neutral"],
  ENTREGADO: ["Entregado", "success"],
  ANULADO: ["Anulado", "danger"],
};

export const formasPago = {
  EFECTIVO: "Efectivo",
  TRANSFERENCIA: "Transferencia",
  BILLETERA_VIRTUAL: "Billetera virtual",
};

const formatosMoneda = {
  ARS: new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }),
  USD: new Intl.NumberFormat("es-AR", { style: "currency", currency: "USD" }),
};

export function moneda(monto, currency = "ARS") {
  return formatosMoneda[currency].format(monto);
}

export function fechaHora(valor) {
  return new Date(valor).toLocaleString("es-AR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}
