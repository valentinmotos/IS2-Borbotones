const formatoMes = new Intl.DateTimeFormat("es-AR", {
  month: "short",
  timeZone: "UTC",
});

export const numero = new Intl.NumberFormat("es-AR");

export function resumirPedidos(pedidos, anio, mes) {
  const preventores = new Map();
  const periodo = mes ? `${anio}-${mes}` : anio;
  const finDeMes = new Date(`${periodo}-01T00:00:00Z`);
  finDeMes.setUTCMonth(finDeMes.getUTCMonth() + 1, 0);
  const cantidadPeriodos = mes ? finDeMes.getUTCDate() : 12;
  const barras = Array.from({ length: cantidadPeriodos }, (_, indice) => {
    const numeroPeriodo = String(indice + 1).padStart(2, "0");
    const fecha = `${periodo}-${numeroPeriodo}`;
    return {
      periodo: mes
        ? numeroPeriodo
        : formatoMes.format(new Date(`${fecha}-01T00:00:00Z`)),
    };
  });

  for (const fila of pedidos) {
    const { preventorId, nombre, apellido, legajo, cantidad, facturados } =
      fila;
    if (!preventores.has(preventorId)) {
      preventores.set(preventorId, {
        id: preventorId,
        nombre: `${nombre} ${apellido} (${legajo})`,
        cantidad: 0,
        facturados: 0,
      });
    }
    const preventor = preventores.get(preventorId);
    preventor.cantidad += cantidad;
    preventor.facturados += facturados;
    const indice =
      Number(mes ? fila.dia.slice(8, 10) : fila.dia.slice(5, 7)) - 1;
    barras[indice][preventorId] = (barras[indice][preventorId] ?? 0) + cantidad;
  }

  const rendimiento = [...preventores.values()].sort(
    (a, b) =>
      b.cantidad - a.cantidad || a.nombre.localeCompare(b.nombre, "es-AR"),
  );
  return {
    barras,
    rendimiento,
    totalPedidos: rendimiento.reduce((total, fila) => total + fila.cantidad, 0),
    totalFacturados: rendimiento.reduce(
      (total, fila) => total + fila.facturados,
      0,
    ),
  };
}

/**
 * HU-01: porciones de la torta de stock. Los `maximo - 1` productos con más bultos van solos y el resto
 * se agrupa en «Otros», así los colores no se repiten. Cada porción lleva su proporción sobre el total.
 */
export function porcionesDeStock(stock, colores, maximo = 8) {
  const conStock = stock
    .filter((producto) => producto.cantidadActual > 0)
    .sort((a, b) => b.cantidadActual - a.cantidadActual);
  const total = conStock.reduce((suma, p) => suma + p.cantidadActual, 0);
  const visibles =
    conStock.length > maximo ? conStock.slice(0, maximo - 1) : conStock;
  const porciones = visibles.map((producto) => ({
    productoId: producto.productoId,
    etiqueta: `${producto.nombre} (${producto.codigo})`,
    cantidadActual: producto.cantidadActual,
  }));
  const resto = conStock.slice(visibles.length);
  if (resto.length > 0) {
    porciones.push({
      productoId: "otros",
      etiqueta: `Otros (${resto.length} productos)`,
      cantidadActual: resto.reduce((suma, p) => suma + p.cantidadActual, 0),
    });
  }
  return porciones.map((porcion, indice) => ({
    ...porcion,
    proporcion: total ? porcion.cantidadActual / total : 0,
    fill: colores[indice % colores.length],
  }));
}
