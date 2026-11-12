import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Pie,
  PieChart,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  Alert,
  DataTable,
  FormField,
  Kpi,
} from "../../../../packages/design/components.jsx";
import useApiResource from "../../lib/useApiResource.js";
import { numero, resumirPedidos } from "./datos.js";
import "./styles.css";

const colores = [
  "#a9321c",
  "#28685b",
  "#9b630d",
  "#57518c",
  "#247294",
  "#923762",
  "#5a6429",
  "#855942",
];
const formatoMes = new Intl.DateTimeFormat("es-AR", {
  month: "long",
  timeZone: "UTC",
});
const meses = Array.from({ length: 12 }, (_, indice) => ({
  valor: String(indice + 1).padStart(2, "0"),
  etiqueta: formatoMes.format(new Date(Date.UTC(2024, indice, 1))),
}));

const columnaCantidad = (key, label) => ({
  key,
  label,
  render: (fila) => numero.format(fila[key]),
});
const columnasPreventores = [
  { key: "nombre", label: "Preventor" },
  columnaCantidad("cantidad", "Pedidos cargados"),
  columnaCantidad("facturados", "Pedidos facturados"),
];
const columnasProductos = [
  { key: "codigo", label: "Código" },
  { key: "nombre", label: "Producto" },
];
const columnasStock = [
  ...columnasProductos,
  columnaCantidad("cantidadActual", "Stock actual"),
];
const columnasVendidos = [
  ...columnasProductos,
  columnaCantidad("cantidad", "Cantidad facturada"),
];

function PanelEstadisticas({ data, anio, mes }) {
  const etiquetaPeriodo = mes
    ? `${meses[Number(mes) - 1].etiqueta} de ${anio}`
    : anio;
  const { barras, rendimiento, totalPedidos, totalFacturados } = resumirPedidos(
    data.pedidos,
    anio,
    mes,
  );
  const totalStock = data.stock.reduce(
    (total, producto) => total + producto.cantidadActual,
    0,
  );
  const porcionesStock = data.stock
    .filter((producto) => producto.cantidadActual > 0)
    .map((producto, indice) => ({
      ...producto,
      etiqueta: `${producto.nombre} (${producto.codigo})`,
      fill: colores[indice % colores.length],
    }));

  return (
    <>
      <dl className="estadisticas-kpis">
        <Kpi
          label="Pedidos cargados"
          value={numero.format(totalPedidos)}
          detail="En el período"
        />
        <Kpi
          label="Pedidos facturados"
          value={numero.format(totalFacturados)}
          detail="De los pedidos del período"
        />
        <Kpi
          label="Stock actual"
          value={numero.format(totalStock)}
          detail="Cantidad total en almacén"
        />
      </dl>

      <section
        className="design-register estadisticas-panel"
        aria-labelledby="pedidos-preventor"
      >
        <h2 id="pedidos-preventor">
          Pedidos por preventor y {mes ? "día" : "mes"}
        </h2>
        <p className="estadisticas-periodo">{etiquetaPeriodo}</p>
        {rendimiento.length > 0 && (
          <div
            className="estadisticas-grafico-scroll"
            tabIndex={0}
            role="region"
            aria-label="Gráfico de pedidos por preventor"
          >
            <BarChart
              responsive
              data={barras}
              style={{ width: "100%", minWidth: mes ? 720 : 440, height: 300 }}
            >
              <CartesianGrid vertical={false} stroke="var(--dm-line)" />
              <XAxis dataKey="periodo" interval={0} />
              <YAxis allowDecimals={false} width="auto" />
              <Tooltip />
              <Legend />
              {rendimiento.map(({ id, nombre }, indice) => (
                <Bar
                  key={id}
                  dataKey={id}
                  name={nombre}
                  stackId="pedidos"
                  fill={colores[indice % colores.length]}
                  isAnimationActive={false}
                />
              ))}
            </BarChart>
          </div>
        )}
        <DataTable
          caption="Rendimiento de preventores en el período"
          columns={columnasPreventores}
          rows={rendimiento}
          rowKey={(fila) => fila.id}
          empty="No hay pedidos cargados en el período."
        />
      </section>

      <div className="estadisticas-detalle">
        <section
          className="design-register estadisticas-panel"
          aria-labelledby="stock-producto"
        >
          <h2 id="stock-producto">Stock actual por producto</h2>
          {porcionesStock.length > 0 ? (
            <PieChart responsive style={{ width: "100%", height: 260 }}>
              <Pie
                data={porcionesStock}
                dataKey="cantidadActual"
                nameKey="etiqueta"
                outerRadius="85%"
                isAnimationActive={false}
              />
              <Tooltip formatter={(valor) => numero.format(valor)} />
            </PieChart>
          ) : (
            <p>No hay productos con stock disponible.</p>
          )}
          {data.stock.length > 0 && (
            <details>
              <summary>Ver existencias por producto</summary>
              <DataTable
                caption="Existencias actuales"
                columns={columnasStock}
                rows={data.stock}
                rowKey={(producto) => producto.productoId}
              />
            </details>
          )}
        </section>

        <section
          className="design-register estadisticas-panel"
          aria-labelledby="productos-vendidos"
        >
          <h2 id="productos-vendidos">Productos más vendidos</h2>
          <DataTable
            caption="Diez productos con mayor cantidad facturada en el período"
            columns={columnasVendidos}
            rows={data.productosMasVendidos}
            rowKey={(producto) => producto.productoId}
            empty="No hay productos facturados en el período."
          />
        </section>
      </div>
    </>
  );
}

export default function EstadisticasPage() {
  const [filtros, setFiltros] = useState(() => {
    const hoy = new Date();
    return {
      anio: String(hoy.getFullYear()),
      mes: String(hoy.getMonth() + 1).padStart(2, "0"),
      grupo: "dia",
    };
  });
  const [periodo, setPeriodo] = useState(filtros);
  const [version, setVersion] = useState(0);
  const anio = String(Number(periodo.anio)).padStart(4, "0");
  const mes = periodo.grupo === "dia" ? periodo.mes : null;
  const params = new URLSearchParams({ anio });
  if (mes) params.set("mes", mes);
  const { data, cargando, error } = useApiResource(
    `/estadisticas?${params}`,
    version,
  );

  function aplicar(event) {
    event.preventDefault();
    setPeriodo(filtros);
    setVersion((actual) => actual + 1);
  }

  return (
    <div className="estadisticas">
      <header className="dm-page-header">
        <div>
          <h1>Estadísticas</h1>
          <p>
            Consultá la actividad de preventa, las ventas y las existencias de
            Dulce Mayor.
          </p>
        </div>
      </header>
      <form className="design-register estadisticas-filtros" onSubmit={aplicar}>
        <label className="dm-field">
          <span>Agrupar pedidos por</span>
          <select
            value={filtros.grupo}
            onChange={(event) =>
              setFiltros({ ...filtros, grupo: event.target.value })
            }
          >
            <option value="dia">Día</option>
            <option value="mes">Mes</option>
          </select>
        </label>
        {filtros.grupo === "dia" && (
          <label className="dm-field">
            <span>Mes</span>
            <select
              value={filtros.mes}
              onChange={(event) =>
                setFiltros({ ...filtros, mes: event.target.value })
              }
            >
              {meses.map(({ valor, etiqueta }) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </label>
        )}
        <FormField
          label="Año"
          type="number"
          min={1}
          max={9999}
          required
          value={filtros.anio}
          onChange={(event) =>
            setFiltros({ ...filtros, anio: event.target.value })
          }
        />
        <button type="submit" className="dm-button dm-button-primary">
          Consultar
        </button>
      </form>
      {cargando && <p role="status">Cargando estadísticas…</p>}
      {error && <Alert tone="danger">{error.message}</Alert>}
      {data && <PanelEstadisticas data={data} anio={anio} mes={mes} />}
    </div>
  );
}
