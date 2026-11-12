export const pesos = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});

export const dolares = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "USD",
  currencyDisplay: "code",
});
