import { useState } from "react";
import { ReceiptText, RefreshCw } from "lucide-react";
import {
  Alert,
  DataTable,
  Pagination,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import DetallePedido from "../components/DetallePedido.jsx";
import { estadosPedido, fechaHora, moneda } from "../formato.js";
import useApiResource from "../../../lib/useApiResource.js";
import "../styles.css";

const porPagina = 8;
const columnas = [
  { key: "numeroPedido", label: "Pedido" },
  { key: "comercio", label: "Comercio" },
  {
    key: "fechaHoraCarga",
    label: "Recibido",
    render: (p) => fechaHora(p.fechaHoraCarga),
  },
  {
    key: "estado",
    label: "Estado",
    render: (p) => (
      <StatusBadge tone={estadosPedido[p.estado][1]}>
        {estadosPedido[p.estado][0]}
      </StatusBadge>
    ),
  },
  { key: "total", label: "Total", render: (p) => moneda(p.total) },
];

export default function AlmacenPage() {
  const [estado, setEstado] = useState("");
  const [pagina, setPagina] = useState(1);
  const [pedidoId, setPedidoId] = useState(null);
  const [version, setVersion] = useState(0);
  const listado = useApiResource(
    estado ? `/pedidos?estado=${estado}` : "/pedidos",
    version,
  );
  const pedidos = listado.data ?? [];
  const paginas = Math.max(1, Math.ceil(pedidos.length / porPagina));
  const paginaVisible = Math.min(pagina, paginas);
  const inicio = (paginaVisible - 1) * porPagina;
  const refrescar = () => setVersion((actual) => actual + 1);

  return (
    <div className="almacen">
      <header className="page-head">
        <div>
          <p className="eyebrow">Pedidos</p>
          <h1>Almacén y facturación</h1>
          <p className="lead">
            Elegí un pedido de la lista para verificar el stock, facturarlo y
            registrar los cobros.
          </p>
        </div>
      </header>
      <div
        className="almacen-layout"
        data-detalle={pedidoId ? "abierto" : undefined}
      >
        <section
          className="panel almacen-lista"
          aria-labelledby="almacen-listado"
        >
          <div className="panel-head">
            <h2 id="almacen-listado">Pedidos recibidos</h2>
            {listado.data && (
              <p>
                {pedidos.length === 1
                  ? "1 pedido"
                  : `${pedidos.length} pedidos`}
              </p>
            )}
          </div>
          <div className="toolbar almacen-toolbar">
            <label className="dm-field">
              <span>Filtrar por estado</span>
              <select
                className="dm-input"
                value={estado}
                onChange={(event) => {
                  setEstado(event.target.value);
                  setPagina(1);
                }}
              >
                <option value="">Todos los estados</option>
                {Object.entries(estadosPedido).map(([id, [etiqueta]]) => (
                  <option key={id} value={id}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="dm-button"
              onClick={refrescar}
              disabled={listado.cargando}
            >
              <RefreshCw size={16} aria-hidden="true" />
              Actualizar pedidos
            </button>
          </div>
          {listado.cargando && (
            <p className="muted" role="status">
              Cargando pedidos…
            </p>
          )}
          {listado.error && (
            <Alert tone="danger">{listado.error.message}</Alert>
          )}
          {listado.data && (
            <>
              <DataTable
                caption="Listado de pedidos"
                columns={[
                  ...columnas,
                  {
                    key: "acciones",
                    label: "Acciones",
                    render: (p) => (
                      <button
                        type="button"
                        className="dm-button"
                        aria-label={`Ver pedido ${p.numeroPedido}`}
                        aria-pressed={pedidoId === p.id}
                        onClick={() => setPedidoId(p.id)}
                      >
                        Ver detalle
                      </button>
                    ),
                  },
                ]}
                rows={pedidos.slice(inicio, inicio + porPagina)}
                rowKey={(p) => p.id}
                empty="No hay pedidos para este estado."
              />
              <Pagination
                page={paginaVisible}
                pageCount={paginas}
                onPageChange={setPagina}
              />
            </>
          )}
        </section>
        {pedidoId ? (
          <DetallePedido
            key={pedidoId}
            pedidoId={pedidoId}
            version={version}
            refrescar={refrescar}
          />
        ) : (
          <aside className="panel panel-tint almacen-sin-detalle">
            <div className="empty-state">
              <ReceiptText size={32} aria-hidden="true" />
              <strong>Ningún pedido seleccionado</strong>
              <p>
                Tocá «Ver detalle» en un pedido para revisar sus productos,
                facturarlo y registrar los pagos.
              </p>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
