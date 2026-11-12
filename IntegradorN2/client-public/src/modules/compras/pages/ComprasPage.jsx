import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronRight, ShoppingBag } from "lucide-react";
import {
  Alert,
  DataTable,
  Kpi,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import { pesos } from "../../catalog/precios.js";
import {
  DescargarFacturaButton,
  PagarAhoraButton,
} from "../components/AccionesFactura.jsx";
import {
  estadosFactura,
  estadosPedido,
  formatearFechaHora,
} from "../estados.js";
import "../styles.css";

// E4-08: historial de pedidos del minorista autenticado con su factura. El minorista sale del
// token: el backend nunca devuelve pedidos de otro.

export default function ComprasPage() {
  const [compras, setCompras] = useState(null);
  const [aviso, setAviso] = useState(null);

  useEffect(() => {
    document.title = "Mis compras · Dulce Mayor";
    const controller = new AbortController();
    api.get("/minorista/compras", { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted) setCompras(data);
      },
      (fallo) => {
        if (!controller.signal.aborted) {
          setCompras([]);
          setAviso(fallo.message);
        }
      },
    );
    return () => controller.abort();
  }, []);

  const columnas = [
    {
      key: "numeroPedido",
      label: "Pedido",
      render: (c) => <Link to={c.pedidoId}>N° {c.numeroPedido}</Link>,
    },
    {
      key: "fechaHoraCarga",
      label: "Fecha",
      render: (c) => formatearFechaHora(c.fechaHoraCarga),
    },
    { key: "comercio", label: "Comercio" },
    { key: "total", label: "Total", render: (c) => pesos.format(c.total) },
    {
      key: "estadoPedido",
      label: "Envío",
      render: (c) => {
        const estado = estadosPedido[c.estadoPedido];
        return (
          <StatusBadge tone={estado?.tono}>
            {estado?.etiqueta ?? c.estadoPedido}
          </StatusBadge>
        );
      },
    },
    {
      key: "estadoFactura",
      label: "Factura",
      render: (c) => {
        if (!c.facturaId) return "Todavía no se facturó";
        const estado = estadosFactura[c.estadoFactura];
        return (
          <StatusBadge tone={estado?.tono}>
            {estado?.etiqueta ?? c.estadoFactura}
          </StatusBadge>
        );
      },
    },
    {
      key: "acciones",
      label: "Acciones",
      render: (c) => (
        <div className="compras-acciones">
          {c.puedePagar && <PagarAhoraButton compra={c} onError={setAviso} />}
          {c.facturaId && (
            <DescargarFacturaButton compra={c} onError={setAviso} />
          )}
          <Link className="dm-button dm-button-ghost" to={c.pedidoId}>
            Ver detalle
            <ChevronRight size={14} aria-hidden="true" />
          </Link>
        </div>
      ),
    },
  ];

  // Resumen para la cabecera: solo cuenta lo que ya trajo la lista.
  const porPagar = compras?.filter((c) => c.puedePagar).length ?? 0;
  const enReparto =
    compras?.filter((c) => c.estadoPedido === "EN_REPARTO").length ?? 0;

  return (
    <div className="compras">
      <header className="page-head">
        <div>
          <h1>Mis compras</h1>
          <p className="lead">
            Tus pedidos con el estado del envío y la factura. Las facturas
            pendientes las pagás desde acá con «Pagar ahora».
          </p>
        </div>
      </header>
      {aviso && (
        <Alert tone="danger" onDismiss={() => setAviso(null)}>
          {aviso}
        </Alert>
      )}
      {compras === null ? (
        <p className="compras-cargando" role="status">
          Cargando tus pedidos…
        </p>
      ) : compras.length === 0 ? (
        <div className="panel empty-state">
          <span className="compras-vacio-icono" aria-hidden="true">
            <ShoppingBag size={26} />
          </span>
          <strong>Todavía no tenés pedidos</strong>
          <p>
            Tu preventor los carga cuando visita tu comercio. Apenas lo haga,
            vas a verlos acá.
          </p>
          <Link className="dm-button" to="/catalogo">
            Mirar el catálogo
          </Link>
        </div>
      ) : (
        <>
          <dl className="kpi-grid compras-resumen">
            <Kpi label="Pedidos" value={compras.length} />
            <Kpi label="Facturas por pagar" value={porPagar} />
            <Kpi label="En reparto" value={enReparto} />
          </dl>
          <div className="compras-lista">
            <DataTable
              caption="Tus pedidos, del más nuevo al más viejo"
              columns={columnas}
              rows={compras}
              rowKey={(c) => c.pedidoId}
              empty="Todavía no tenés pedidos. Tu preventor los carga cuando visita tu comercio."
            />
          </div>
        </>
      )}
    </div>
  );
}
