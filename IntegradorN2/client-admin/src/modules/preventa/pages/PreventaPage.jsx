import { useRef, useState } from "react";
import {
  Alert,
  FormField,
  Pagination,
  StatusBadge,
} from "../../../../../packages/design/components.jsx";
import { api } from "../../../lib/api.js";
import PuntoVentaForm from "../../puntos-venta/components/PuntoVentaForm.jsx";
import MinoristaForm from "../components/MinoristaForm.jsx";
import MiRuta from "../components/MiRuta.jsx";
import usePreventaResource from "../usePreventaResource.js";
import "../styles.css";

const moneda = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});
const estados = {
  CARGADO: ["Cargado", "neutral"],
  SIN_STOCK: ["Sin stock", "warning"],
  FACTURADO: ["Facturado", "success"],
  EN_REPARTO: ["En reparto", "neutral"],
  ENTREGADO: ["Entregado", "success"],
  ANULADO: ["Anulado", "danger"],
};
const porPagina = 8;

function Catalogo({ recurso, lineas, agregar, guardando }) {
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(1);
  const texto = busqueda.trim().toLocaleLowerCase("es-AR");
  const resultados = (recurso.data ?? []).filter((p) =>
    `${p.nombre} ${p.codigo}`.toLocaleLowerCase("es-AR").includes(texto),
  );
  const paginas = Math.max(1, Math.ceil(resultados.length / porPagina));
  const paginaVisible = Math.min(pagina, paginas);
  const productos = resultados.slice(
    (paginaVisible - 1) * porPagina,
    paginaVisible * porPagina,
  );

  return (
    <section id="catalogo-preventa" className="preventa-panel">
      <div className="preventa-encabezado">
        <h2>Catálogo</h2>
        <button className="dm-button" type="button" onClick={recurso.reload}>
          Actualizar stock
        </button>
      </div>
      <FormField
        label="Buscar por producto o código"
        type="search"
        value={busqueda}
        onChange={(event) => {
          setBusqueda(event.target.value);
          setPagina(1);
        }}
      />
      {recurso.error && <Alert tone="danger">{recurso.error}</Alert>}
      {!recurso.data && !recurso.error && (
        <p role="status">Cargando catálogo…</p>
      )}
      {recurso.data && !resultados.length && (
        <p>No hay productos disponibles para esta búsqueda.</p>
      )}
      {productos.length > 0 && (
        <ul className="preventa-lista">
          {productos.map((producto) => (
            <li className="preventa-producto" key={producto.id}>
              <div>
                <strong>{producto.nombre}</strong>
                <span>
                  {producto.codigo} · {producto.presentacion}
                </span>
                <span>{producto.stock} bultos disponibles</span>
              </div>
              <div className="preventa-producto-accion">
                <strong>{moneda.format(producto.precio)}</strong>
                <button
                  className="dm-button"
                  type="button"
                  disabled={
                    guardando ||
                    Boolean(lineas[producto.id]) ||
                    Boolean(recurso.error)
                  }
                  aria-label={
                    lineas[producto.id]
                      ? `${producto.nombre} agregado`
                      : `Agregar ${producto.nombre}`
                  }
                  onClick={() => agregar(producto)}
                >
                  {lineas[producto.id] ? "Agregado" : "Agregar"}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {paginas > 1 && (
        <Pagination
          page={paginaVisible}
          pageCount={paginas}
          onPageChange={setPagina}
        />
      )}
    </section>
  );
}

function MisPedidos({ recurso }) {
  return (
    <section id="pedidos-preventa" className="preventa-panel">
      <div className="preventa-encabezado">
        <h2>Mis pedidos</h2>
        <button className="dm-button" type="button" onClick={recurso.reload}>
          Actualizar estados
        </button>
      </div>
      {recurso.error && <Alert tone="danger">{recurso.error}</Alert>}
      {!recurso.data && !recurso.error && (
        <p role="status">Cargando pedidos…</p>
      )}
      {recurso.data?.length === 0 && <p>Todavía no cargaste pedidos.</p>}
      {recurso.data?.length > 0 && (
        <ul className="preventa-lista">
          {recurso.data.map((pedido) => {
            const [estado, tono] = estados[pedido.estado] ?? [
              pedido.estado,
              "neutral",
            ];
            return (
              <li className="preventa-pedido" key={pedido.id}>
                <div>
                  <strong>Pedido #{pedido.numeroPedido}</strong>
                  <span>
                    {pedido.comercio} ·{" "}
                    {pedido.fechaHoraCarga
                      .slice(0, 10)
                      .split("-")
                      .reverse()
                      .join("/")}
                  </span>
                  {pedido.codigoPromocion && (
                    <span>Código ingresado {pedido.codigoPromocion}</span>
                  )}
                </div>
                <StatusBadge tone={tono}>{estado}</StatusBadge>
                <strong>{moneda.format(pedido.total)}</strong>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

export default function PreventaPage() {
  const catalogo = usePreventaResource("/publico/catalogo", 10_000);
  const puntos = usePreventaResource("/preventa/puntos-de-venta");
  const minoristas = usePreventaResource("/puntos-de-venta/minoristas");
  const pedidos = usePreventaResource("/preventa/pedidos", 15_000);
  const [lineas, setLineas] = useState({});
  const [puntoId, setPuntoId] = useState("");
  const [codigo, setCodigo] = useState("");
  const [formAlta, setFormAlta] = useState(null);
  const [minoristaId, setMinoristaId] = useState("");
  const [aviso, setAviso] = useState(null);
  const [errorPedido, setErrorPedido] = useState(null);
  const [guardando, setGuardando] = useState(false);
  // E5-01: cada pedido cerrado recarga la ruta para proponer el próximo punto.
  const [ruta, setRuta] = useState({ version: 0, ultimoPuntoId: null });
  const intento = useRef(null);

  const vigentes = new Map(
    (catalogo.data ?? []).map((producto) => [producto.id, producto]),
  );
  const detalle = Object.entries(lineas).map(([id, linea]) => ({
    ...linea,
    producto: vigentes.get(id) ?? linea.producto,
    stock: vigentes.get(id)?.stock ?? 0,
  }));
  const total =
    detalle.reduce(
      (acumulado, { producto, cantidad }) =>
        acumulado + Math.round(producto.precio * Number(cantidad) * 100),
      0,
    ) / 100;
  const cantidadesValidas = detalle.every(
    ({ cantidad, stock }) =>
      Number.isInteger(Number(cantidad)) &&
      Number(cantidad) > 0 &&
      Number(cantidad) <= stock,
  );

  function cambiarCantidad(id, cantidad) {
    setLineas((previas) => ({
      ...previas,
      [id]: { ...previas[id], cantidad },
    }));
  }

  function quitar(id) {
    setLineas((previas) => {
      const siguientes = { ...previas };
      delete siguientes[id];
      return siguientes;
    });
  }

  async function guardar(event) {
    event.preventDefault();
    if (
      guardando ||
      !puntoId ||
      !detalle.length ||
      !cantidadesValidas ||
      catalogo.error
    )
      return;
    setGuardando(true);
    setErrorPedido(null);
    setAviso(null);
    try {
      const pedido = {
        puntoDeVentaId: puntoId,
        codigoPromocion: codigo,
        lineas: detalle.map(({ producto, cantidad }) => ({
          productoId: producto.id,
          cantidad: Number(cantidad),
        })),
      };
      const datos = JSON.stringify(pedido);
      if (intento.current?.datos !== datos) {
        intento.current = { datos, claveCarga: crypto.randomUUID() };
      }
      const { data } = await api.post("/preventa/pedidos", {
        ...pedido,
        claveCarga: intento.current.claveCarga,
      });
      intento.current = null;
      setLineas({});
      setCodigo("");
      setAviso(
        `Se cargó el pedido #${data.numeroPedido} por ${moneda.format(data.total)}.`,
      );
      void pedidos.reload();
      setRuta(({ version }) => ({
        version: version + 1,
        ultimoPuntoId: pedido.puntoDeVentaId,
      }));
    } catch (failure) {
      setErrorPedido(failure.message);
    } finally {
      setGuardando(false);
      void catalogo.reload();
    }
  }

  function elegirPunto(id) {
    setPuntoId(id);
    document
      .getElementById("nuevo-pedido")
      ?.scrollIntoView({ behavior: "smooth" });
  }

  function minoristaCreado(data) {
    setMinoristaId(data.id);
    setFormAlta("punto");
    setAviso(
      `Se registró a ${data.nombre} ${data.apellido}. Ya podés asociarlo a un punto de venta.`,
    );
    void minoristas.reload();
  }

  function puntoCreado(data) {
    setPuntoId(data.id);
    setFormAlta(null);
    setMinoristaId("");
    setAviso(`Se agregó ${data.nombreComercio} al pedido.`);
    void puntos.reload();
    setRuta((previa) => ({ ...previa, version: previa.version + 1 }));
  }

  return (
    <div className="preventa">
      <header className="dm-page-header">
        <div>
          <h1>Preventa</h1>
          <p>
            Consultá el stock del almacén, registrá comercios y cargá sus
            pedidos.
          </p>
        </div>
      </header>
      <nav className="preventa-nav" aria-label="Secciones de preventa">
        <a href="#ruta-preventa">Mi ruta</a>
        <a href="#catalogo-preventa">Catálogo</a>
        <a href="#nuevo-pedido">
          Nuevo pedido{detalle.length > 0 && ` (${detalle.length})`}
        </a>
        <a href="#pedidos-preventa">Mis pedidos</a>
      </nav>
      <MiRuta
        version={ruta.version}
        ultimoPuntoId={ruta.ultimoPuntoId}
        onElegir={elegirPunto}
      />
      <div className="preventa-grid">
        <Catalogo
          recurso={catalogo}
          lineas={lineas}
          guardando={guardando}
          agregar={(producto) =>
            setLineas((previas) => ({
              ...previas,
              [producto.id]: { producto, cantidad: "1" },
            }))
          }
        />
        <section id="nuevo-pedido" className="preventa-panel">
          <h2>Nuevo pedido</h2>
          {aviso && (
            <Alert tone="success" onDismiss={() => setAviso(null)}>
              {aviso}
            </Alert>
          )}
          <div className="preventa-altas">
            <button
              className="dm-button"
              type="button"
              aria-expanded={formAlta === "minorista"}
              onClick={() =>
                setFormAlta(formAlta === "minorista" ? null : "minorista")
              }
            >
              Nuevo minorista
            </button>
            <button
              className="dm-button"
              type="button"
              aria-expanded={formAlta === "punto"}
              onClick={() => setFormAlta(formAlta === "punto" ? null : "punto")}
            >
              Nuevo punto de venta
            </button>
          </div>
          {formAlta === "minorista" && (
            <MinoristaForm onCreado={minoristaCreado} />
          )}
          {formAlta === "punto" && (
            <>
              {minoristas.error && (
                <Alert tone="danger">
                  {minoristas.error}
                  <button
                    className="dm-button"
                    type="button"
                    onClick={minoristas.reload}
                  >
                    Reintentar
                  </button>
                </Alert>
              )}
              <PuntoVentaForm
                key={minoristaId}
                minoristas={minoristas.data ?? []}
                minoristaId={minoristaId}
                onCreado={puntoCreado}
              />
            </>
          )}
          <form className="preventa-form" onSubmit={guardar}>
            {puntos.error && (
              <Alert tone="danger">
                {puntos.error}
                <button
                  className="dm-button"
                  type="button"
                  onClick={puntos.reload}
                >
                  Reintentar
                </button>
              </Alert>
            )}
            <label className="dm-field">
              <span>Punto de venta</span>
              <select
                className="dm-input"
                value={puntoId}
                required
                disabled={guardando}
                onChange={(event) => setPuntoId(event.target.value)}
              >
                <option value="">
                  {puntos.data
                    ? "Seleccioná un punto de venta"
                    : "Cargando puntos de venta…"}
                </option>
                {puntos.data?.map((punto) => (
                  <option key={punto.id} value={punto.id}>
                    {punto.nombreComercio} · {punto.localidad}
                  </option>
                ))}
              </select>
            </label>
            {!detalle.length && <p>Agregá productos desde el catálogo.</p>}
            {detalle.length > 0 && (
              <ul className="preventa-lista">
                {detalle.map(({ producto, cantidad, stock }) => (
                  <li className="preventa-linea" key={producto.id}>
                    <strong>{producto.nombre}</strong>
                    <span>
                      {moneda.format(producto.precio)} por bulto · {stock}{" "}
                      disponibles
                    </span>
                    <div>
                      <FormField
                        label={`Bultos de ${producto.nombre}`}
                        type="number"
                        inputMode="numeric"
                        min={1}
                        max={stock}
                        step={1}
                        required
                        disabled={guardando}
                        value={cantidad}
                        error={
                          Number(cantidad) > stock
                            ? "La cantidad supera el stock actual."
                            : undefined
                        }
                        onChange={(event) =>
                          cambiarCantidad(producto.id, event.target.value)
                        }
                      />
                      <button
                        className="dm-button"
                        type="button"
                        disabled={guardando}
                        aria-label={`Quitar ${producto.nombre}`}
                        onClick={() => quitar(producto.id)}
                      >
                        Quitar
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
            <FormField
              label="Código de promoción (opcional)"
              value={codigo}
              maxLength={50}
              disabled={guardando}
              hint="El código queda registrado para su validación."
              onChange={(event) => setCodigo(event.target.value)}
            />
            <div className="preventa-total">
              <span>Total del pedido</span>
              <strong>{moneda.format(total)}</strong>
            </div>
            {errorPedido && <Alert tone="danger">{errorPedido}</Alert>}
            <button
              className="dm-button dm-button-primary"
              type="submit"
              disabled={
                guardando ||
                !puntoId ||
                !detalle.length ||
                !cantidadesValidas ||
                Boolean(catalogo.error)
              }
            >
              {guardando ? "Cargando pedido…" : "Cargar pedido"}
            </button>
          </form>
        </section>
      </div>
      <MisPedidos recurso={pedidos} />
    </div>
  );
}
