import { useEffect, useState } from "react";
import { RefreshCw } from "lucide-react";
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
import { numero, porcionesDeStock, resumirPedidos } from "./datos.js";
import "./styles.css";

// Paleta del rediseño: rojo y ámbar de la marca más tonos que combinan y se distinguen entre sí.
const colores = [
  "#e8281a",
  "#f6a623",
  "#2d6a4f",
  "#9b1a10",
  "#f4c77a",
  "#7fa892",
  "#8a5a3b",
  "#6f6a64",
];
const tinta = "#6f6a64";
const linea = "#ece4da";
const estiloTooltip = {
  border: "1px solid #ece4da",
  borderRadius: 12,
  boxShadow: "0 8px 30px rgb(26 26 26 / 8%)",
  fontSize: 13,
};
const formatoMes = new Intl.DateTimeFormat("es-AR", {
  month: "long",
  timeZone: "UTC",
});
const formatoHora = new Intl.DateTimeFormat("es-AR", {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});
/** HU-01, criterio 4: el panel se vuelve a consultar solo cada 30 s y al volver a la pestaña. */
const REFRESCO_MS = 30_000;
const porcentaje = new Intl.NumberFormat("es-AR", {
  style: "percent",
  maximumFractionDigits: 1,
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
  // HU-01, criterio 3: los productos con más stock y el resto agrupado en «Otros», con su proporción
  const porcionesStock = porcionesDeStock(data.stock, colores);

  return (
    <>
      <dl className="kpi-grid">
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
        className="panel estadisticas-panel"
        aria-labelledby="pedidos-preventor"
      >
        <div className="panel-head">
          <h2 id="pedidos-preventor">
            Pedidos por preventor y {mes ? "día" : "mes"}
          </h2>
          <p className="estadisticas-periodo">{etiquetaPeriodo}</p>
        </div>
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
              <CartesianGrid vertical={false} stroke={linea} />
              <XAxis
                dataKey="periodo"
                interval={0}
                stroke={linea}
                tick={{ fill: tinta, fontSize: 12 }}
                tickLine={false}
              />
              <YAxis
                allowDecimals={false}
                width="auto"
                stroke={linea}
                tick={{ fill: tinta, fontSize: 12 }}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip
                contentStyle={estiloTooltip}
                cursor={{ fill: "rgb(246 166 35 / 12%)" }}
              />
              <Legend iconType="circle" wrapperStyle={{ fontSize: 13 }} />
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
          className="panel estadisticas-panel"
          aria-labelledby="stock-producto"
        >
          <h2 id="stock-producto">Stock actual por producto</h2>
          {porcionesStock.length > 0 ? (
            <div className="estadisticas-torta">
              <PieChart responsive style={{ width: "100%", height: 260 }}>
                <Pie
                  data={porcionesStock}
                  dataKey="cantidadActual"
                  nameKey="etiqueta"
                  innerRadius="45%"
                  outerRadius="85%"
                  stroke="#fff"
                  strokeWidth={2}
                  isAnimationActive={false}
                />
                <Tooltip
                  contentStyle={estiloTooltip}
                  formatter={(valor, _nombre, { payload }) =>
                    `${numero.format(valor)} bultos (${porcentaje.format(payload.proporcion)})`
                  }
                />
              </PieChart>
              <ul
                className="estadisticas-leyenda"
                aria-label="Proporción del stock por producto"
              >
                {porcionesStock.map((porcion) => (
                  <li key={porcion.productoId}>
                    <span
                      className="estadisticas-color"
                      style={{ background: porcion.fill }}
                      aria-hidden="true"
                    />
                    <span>{porcion.etiqueta}</span>
                    <strong>{porcentaje.format(porcion.proporcion)}</strong>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <p className="empty-state">
              No hay productos con stock disponible.
            </p>
          )}
          {data.stock.length > 0 && (
            <details className="estadisticas-existencias">
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
          className="panel estadisticas-panel"
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
  const [refresco, setRefresco] = useState(0);
  const { data, cargando, actualizando, recibido, error } = useApiResource(
    `/estadisticas?${params}`,
    `${version}-${refresco}`,
    { conservar: true },
  );

  useEffect(() => {
    const refrescar = () => {
      if (document.visibilityState === "visible") setRefresco((n) => n + 1);
    };
    const intervalo = setInterval(refrescar, REFRESCO_MS);
    document.addEventListener("visibilitychange", refrescar);
    return () => {
      clearInterval(intervalo);
      document.removeEventListener("visibilitychange", refrescar);
    };
  }, []);

  function aplicar(event) {
    event.preventDefault();
    setPeriodo(filtros);
    setVersion((actual) => actual + 1);
  }

  return (
    <div className="estadisticas">
      <header className="page-head">
        <div>
          <p className="eyebrow">General</p>
          <h1>Estadísticas</h1>
          <p className="lead">
            Elegí un mes o un año para ver la actividad de preventa, las ventas
            y las existencias de Dulce Mayor.
          </p>
        </div>
      </header>
      <form className="panel toolbar estadisticas-filtros" onSubmit={aplicar}>
        <label className="dm-field">
          <span>Agrupar pedidos por</span>
          <select
            className="dm-input"
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
              className="dm-input"
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
        {recibido && (
          <p className="estadisticas-actualizado" role="status">
            <RefreshCw size={14} aria-hidden="true" />
            {actualizando
              ? "Actualizando…"
              : `Actualizado a las ${formatoHora.format(recibido)} · se actualiza solo cada 30 segundos`}
          </p>
        )}
      </form>
      {cargando && (
        <p className="muted" role="status">
          Cargando estadísticas…
        </p>
      )}
      {error && <Alert tone="danger">{error.message}</Alert>}
      {data && <PanelEstadisticas data={data} anio={anio} mes={mes} />}
    </div>
  );
}
