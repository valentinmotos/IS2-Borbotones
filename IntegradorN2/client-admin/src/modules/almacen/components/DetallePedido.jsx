import { useEffect, useRef, useState } from "react";
import {
  Alert,
  DataTable,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { estadosPedido, formasPago, moneda } from "../formato.js";
import useApiResource from "../../../lib/useApiResource.js";
import PagoForm from "./PagoForm.jsx";

const columnasDetalle = [
  { key: "codigo", label: "Código" },
  { key: "producto", label: "Producto" },
  { key: "cantidad", label: "Cantidad" },
  {
    key: "precioUnitario",
    label: "Precio unitario",
    render: (linea) => moneda(linea.precioUnitario),
  },
  {
    key: "subtotal",
    label: "Importe",
    render: (linea) => moneda(linea.subtotal),
  },
];
const columnasFaltantes = [
  { key: "producto", label: "Producto" },
  { key: "cantidadPedida", label: "Pedido" },
  { key: "stockDisponible", label: "Disponible" },
  {
    key: "faltan",
    label: "Faltan",
    render: (linea) => linea.cantidadPedida - linea.stockDisponible,
  },
];
const columnasPagos = [
  {
    key: "fechaPago",
    label: "Fecha",
    render: (pago) => pago.fechaPago.split("-").reverse().join("/"),
  },
  {
    key: "formaDePago",
    label: "Forma de pago",
    render: (pago) => formasPago[pago.formaDePago] ?? pago.formaDePago,
  },
  {
    key: "monto",
    label: "Monto",
    render: (pago) => `${moneda(pago.monto, pago.moneda)} ${pago.moneda}`,
  },
  {
    key: "equivalenteEnPesos",
    label: "Equivalente en pesos",
    render: (pago) => moneda(pago.equivalenteEnPesos),
  },
  {
    key: "observacion",
    label: "Observación",
    render: (pago) => pago.observacion || "—",
  },
];
const estadosFactura = {
  PENDIENTE_PAGO: ["Pendiente de pago", "warning"],
  PAGADA: ["Pagada", "success"],
  ANULADA: ["Anulada", "danger"],
};

function VerificacionStock({ faltantes }) {
  return (
    <div className="almacen-bloque">
      <h3>Verificación de stock</h3>
      {faltantes.length ? (
        <DataTable
          caption="Productos con stock insuficiente"
          columns={columnasFaltantes}
          rows={faltantes}
          rowKey={(linea) => linea.productoId}
        />
      ) : (
        <Alert tone="success">
          Hay stock suficiente para procesar el pedido.
        </Alert>
      )}
    </div>
  );
}

function Factura({ factura, pagos, ocupado, descargar, registrar }) {
  const [etiqueta, tono] = estadosFactura[factura.estado];
  const pagado = pagos.reduce(
    (total, pago) => total + Math.round(pago.equivalenteEnPesos * 100),
    0,
  );
  const saldo = Math.max(0, Math.round(factura.total * 100) - pagado) / 100;
  return (
    <div className="almacen-bloque">
      <div className="almacen-encabezado">
        <h3>Factura N° {factura.numeroVisible}</h3>
        <StatusBadge tone={tono}>{etiqueta}</StatusBadge>
        <button
          className="dm-button"
          type="button"
          onClick={descargar}
          disabled={ocupado}
        >
          Descargar PDF
        </button>
      </div>
      {factura.estado === "PENDIENTE_PAGO" && (
        <p>
          Saldo pendiente <strong>{moneda(saldo)}</strong>
        </p>
      )}
      <DataTable
        caption="Pagos registrados"
        columns={columnasPagos}
        rows={pagos}
        rowKey={(pago) => pago.id}
        empty="Todavía no hay pagos registrados."
      />
      {factura.estado === "PENDIENTE_PAGO" && saldo > 0 && (
        <PagoForm saldo={saldo} ocupado={ocupado} registrar={registrar} />
      )}
    </div>
  );
}

export default function DetallePedido({ pedidoId, version, refrescar }) {
  const recurso = useApiResource(`/pedidos/${pedidoId}`, version);
  const [aviso, setAviso] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const panel = useRef(null);
  useEffect(() => panel.current?.focus(), []);
  const detalle = recurso.data;

  async function ejecutar(accion) {
    if (ocupado) return;
    setOcupado(true);
    setAviso(null);
    try {
      await accion();
    } catch (error) {
      setAviso({ tono: "danger", mensaje: error.message });
    } finally {
      setOcupado(false);
    }
  }

  function facturar() {
    ejecutar(async () => {
      const { data } = await api.post(`/pedidos/${pedidoId}/facturar`);
      setAviso({
        tono: data.estadoPedido === "FACTURADO" ? "success" : "warning",
        mensaje: data.mensaje,
      });
      refrescar();
    });
  }

  function registrar(pago) {
    ejecutar(async () => {
      await api.post("/pagos", { ...pago, facturaId: detalle.factura.id });
      setAviso({ tono: "success", mensaje: "Pago registrado." });
      refrescar();
    });
  }

  function descargar() {
    ejecutar(async () => {
      const factura = detalle.factura;
      const { data } = await api.get(`/facturas/${factura.id}/pdf`, {
        responseType: "blob",
      });
      const url = URL.createObjectURL(data);
      const enlace = document.createElement("a");
      enlace.href = url;
      enlace.download = `factura-${factura.numeroVisible}.pdf`;
      enlace.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
  }

  const procesable =
    detalle && ["CARGADO", "SIN_STOCK"].includes(detalle.pedido.estado);
  return (
    <section
      className="design-register almacen-panel"
      aria-labelledby="almacen-detalle"
      tabIndex={-1}
      ref={panel}
    >
      <div className="almacen-encabezado">
        <h2 id="almacen-detalle">
          {detalle
            ? `Pedido #${detalle.pedido.numeroPedido}`
            : "Detalle del pedido"}
        </h2>
        <button
          type="button"
          className="dm-button"
          onClick={refrescar}
          disabled={ocupado || recurso.cargando}
        >
          Actualizar detalle
        </button>
      </div>
      {aviso && (
        <Alert tone={aviso.tono} onDismiss={() => setAviso(null)}>
          {aviso.mensaje}
        </Alert>
      )}
      {recurso.cargando && <p role="status">Cargando detalle…</p>}
      {recurso.error && <Alert tone="danger">{recurso.error.message}</Alert>}
      {detalle && (
        <>
          <p>
            {detalle.pedido.comercio}{" "}
            <StatusBadge tone={estadosPedido[detalle.pedido.estado][1]}>
              {estadosPedido[detalle.pedido.estado][0]}
            </StatusBadge>
          </p>
          <DataTable
            caption="Productos del pedido"
            columns={columnasDetalle}
            rows={detalle.detalles}
            rowKey={(linea) => linea.id}
          />
          <dl className="almacen-totales">
            <div>
              <dt>Subtotal</dt>
              <dd>{moneda(detalle.subtotal)}</dd>
            </div>
            <div>
              <dt>Descuento</dt>
              <dd>{moneda(detalle.descuento)}</dd>
            </div>
            <div>
              <dt>Total</dt>
              <dd>{moneda(detalle.pedido.total)}</dd>
            </div>
          </dl>
          {procesable && (
            <>
              <VerificacionStock faltantes={detalle.faltantes} />
              <button
                className="dm-button dm-button-primary"
                type="button"
                onClick={facturar}
                disabled={ocupado || !detalle.detalles.length}
              >
                {ocupado ? "Procesando…" : "Procesar y facturar"}
              </button>
            </>
          )}
          {detalle.factura && (
            <Factura
              factura={detalle.factura}
              pagos={detalle.pagos}
              ocupado={ocupado}
              descargar={descargar}
              registrar={registrar}
            />
          )}
        </>
      )}
    </section>
  );
}
