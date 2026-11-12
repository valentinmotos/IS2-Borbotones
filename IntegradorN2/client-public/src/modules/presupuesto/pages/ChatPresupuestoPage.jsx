import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  Bot,
  Check,
  CircleAlert,
  Info,
  Plus,
  ReceiptText,
  Send,
  Sparkles,
} from "lucide-react";
import { api } from "../../../lib/api.js";
import "../styles.css";

const formatoPesos = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});
const formatoDolares = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "USD",
});
const consultaSugerida = "Quiero 10 alfajores y 5 chupetines";

function Importe({ valor, dolares = false }) {
  if (valor === null || valor === undefined)
    return <span>Sin cotización disponible</span>;
  return <>{(dolares ? formatoDolares : formatoPesos).format(Number(valor))}</>;
}

export default function ChatPresupuestoPage() {
  const [mensaje, setMensaje] = useState("");
  const [respuesta, setRespuesta] = useState(null);
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [fallback, setFallback] = useState(false);
  const [catalogo, setCatalogo] = useState([]);
  const [cotizacion, setCotizacion] = useState(null);
  const [productoId, setProductoId] = useState("");
  const [cantidad, setCantidad] = useState("1");
  const [pedidoManual, setPedidoManual] = useState([]);
  const [avisoManual, setAvisoManual] = useState("");

  useEffect(() => {
    document.title = "Armar presupuesto · Dulce Mayor";
  }, []);

  const productoElegido = catalogo.find(
    (producto) => producto.id === productoId,
  );
  const totalManual = useMemo(
    () =>
      pedidoManual.reduce(
        (total, item) => total + item.precio * item.cantidad,
        0,
      ),
    [pedidoManual],
  );

  async function cargarAlternativa() {
    setFallback(true);
    const [productos, cambio] = await Promise.allSettled([
      api.get("/publico/catalogo"),
      api.get("/publico/cotizacion"),
    ]);
    if (productos.status === "fulfilled") {
      const disponibles = productos.value.data;
      setCatalogo(disponibles);
      setProductoId((actual) =>
        disponibles.some((item) => item.id === actual)
          ? actual
          : (disponibles[0]?.id ?? ""),
      );
    } else {
      setError(
        "No pudimos cargar los productos para el formulario. Probá nuevamente más tarde.",
      );
    }
    if (cambio.status === "fulfilled") setCotizacion(cambio.value.data);
    else setCotizacion(null);
  }

  async function enviar(event) {
    event.preventDefault();
    const consulta = mensaje.trim();
    if (!consulta || ocupado) return;
    setOcupado(true);
    setError("");
    setAvisoManual("");
    setRespuesta(null);
    try {
      const resultado = await api.post("/publico/chatbot/presupuesto", {
        mensaje: consulta,
      });
      setRespuesta(resultado.data);
      setFallback(false);
    } catch (fallo) {
      if (fallo.status === 503) {
        setError(
          "El asistente no está disponible ahora. Podés armar el presupuesto con el formulario.",
        );
        await cargarAlternativa();
      } else {
        setError(fallo.message);
      }
    } finally {
      setOcupado(false);
    }
  }

  function agregarProducto(event) {
    event.preventDefault();
    const unidades = Number(cantidad);
    if (!productoElegido || !Number.isInteger(unidades) || unidades < 1) {
      setAvisoManual("Elegí un producto e ingresá una cantidad válida.");
      return;
    }
    const existente = pedidoManual.find(
      (item) => item.id === productoElegido.id,
    );
    const nuevaCantidad = unidades + (existente?.cantidad ?? 0);
    if (nuevaCantidad > productoElegido.stock) {
      setAvisoManual(
        `Hay ${productoElegido.stock} bultos disponibles de ${productoElegido.nombre}.`,
      );
      return;
    }
    setPedidoManual((actual) =>
      existente
        ? actual.map((item) =>
            item.id === productoElegido.id
              ? { ...item, cantidad: nuevaCantidad }
              : item,
          )
        : [...actual, { ...productoElegido, cantidad: unidades }],
    );
    setAvisoManual("");
    setCantidad("1");
  }

  function quitarProducto(id) {
    setPedidoManual((actual) => actual.filter((item) => item.id !== id));
  }

  return (
    <section className="page quote-page" aria-labelledby="presupuesto-titulo">
      <div className="wrap">
        <header className="page-head">
          <div>
            <p className="eyebrow">Cotización estimada</p>
            <h1 id="presupuesto-titulo">Armá tu presupuesto</h1>
            <p className="lead">
              Contanos qué productos y cuántos bultos necesitás. El asistente
              revisa disponibilidad y precios vigentes, y te arma un total
              estimado.
            </p>
          </div>
        </header>

        <div className="split quote-layout">
          <section className="quote-chat" aria-labelledby="chat-titulo">
            <header className="quote-chat-head">
              <span className="quote-avatar" aria-hidden="true">
                <Bot size={22} />
              </span>
              <div>
                <h2 id="chat-titulo">Asistente de Dulce Mayor</h2>
                <p>
                  <span className="quote-status-dot" aria-hidden="true" />{" "}
                  Responde con precios y stock vigentes
                </p>
              </div>
            </header>

            <div className="quote-log">
              <div className="quote-msg is-bot">
                <p>
                  ¡Hola! ¿Qué estás buscando? Escribime los productos y las
                  cantidades en bultos, y te armo el presupuesto.
                </p>
                <p className="quote-suggestion-label">
                  Probá con este ejemplo:
                </p>
                <button
                  className="quote-suggestion"
                  type="button"
                  onClick={() => setMensaje(consultaSugerida)}
                >
                  <Sparkles size={14} aria-hidden="true" /> {consultaSugerida}
                </button>
              </div>
              {ocupado && (
                <p className="quote-msg is-bot quote-typing" role="status">
                  <span className="quote-dots" aria-hidden="true">
                    <span />
                    <span />
                    <span />
                  </span>{" "}
                  El asistente está escribiendo…
                </p>
              )}
              {error && (
                <p className="quote-msg is-error" role="alert">
                  {error}
                </p>
              )}
              {respuesta && <ResultadoPresupuesto resultado={respuesta} />}
            </div>

            <form className="quote-composer" onSubmit={enviar}>
              <label htmlFor="mensaje-presupuesto">Tu pedido</label>
              <textarea
                id="mensaje-presupuesto"
                className="dm-input"
                name="mensaje"
                rows={3}
                maxLength={500}
                required
                value={mensaje}
                onChange={(event) => setMensaje(event.target.value)}
                placeholder="Escribí productos y cantidades…"
                aria-describedby="mensaje-ayuda"
              />
              <div className="quote-composer-footer">
                <small id="mensaje-ayuda">Hasta 500 caracteres</small>
                <button
                  className="dm-button dm-button-primary"
                  type="submit"
                  disabled={ocupado || !mensaje.trim()}
                >
                  <Send size={16} aria-hidden="true" />
                  {ocupado ? "Armando presupuesto…" : "Consultar"}
                </button>
              </div>
            </form>
          </section>

          <aside
            className="panel panel-tint quote-aside"
            aria-labelledby="consejos-titulo"
          >
            <h2 id="consejos-titulo">Consejos para tu consulta</h2>
            <ul className="quote-tips" role="list">
              <li>
                <Check size={16} aria-hidden="true" /> Escribí el nombre del
                producto y cuántos bultos querés.
              </li>
              <li>
                <Check size={16} aria-hidden="true" /> Podés pedir varios
                productos en el mismo mensaje.
              </li>
              <li>
                <Check size={16} aria-hidden="true" /> Si algo no se puede
                incluir, te avisamos cuál y por qué.
              </li>
            </ul>
            <p className="quote-note">
              <Info size={16} aria-hidden="true" />
              <span>
                El presupuesto es orientativo y no reserva stock. Los precios
                pueden cambiar.
              </span>
            </p>
            <div className="quote-aside-more">
              <p>
                ¿Preferís explorar? Revisá todos los productos y sus
                presentaciones.
              </p>
              <Link className="dm-button" to="/catalogo">
                Ver catálogo
              </Link>
            </div>
          </aside>
        </div>

        {fallback && (
          <section
            className="panel quote-fallback"
            aria-labelledby="formulario-manual-titulo"
          >
            <header className="quote-fallback-head">
              <p className="eyebrow">Alternativa manual</p>
              <h2 id="formulario-manual-titulo">
                Armá el presupuesto por producto
              </h2>
              <p>
                Seleccioná productos disponibles y cantidades. Los precios
                corresponden al catálogo vigente.
              </p>
            </header>
            {catalogo.length === 0 ? (
              <p className="muted" role="status">
                {error || "Cargando productos disponibles…"}
              </p>
            ) : (
              <>
                <form className="quote-manual-form" onSubmit={agregarProducto}>
                  <label className="dm-field quote-product-field">
                    <span>Producto</span>
                    <select
                      value={productoId}
                      onChange={(event) => setProductoId(event.target.value)}
                      required
                    >
                      {catalogo.map((producto) => (
                        <option key={producto.id} value={producto.id}>
                          {producto.nombre} · {producto.presentacion} ·{" "}
                          {formatoPesos.format(producto.precio)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="dm-field quote-quantity-field">
                    <span>Bultos</span>
                    <input
                      type="number"
                      min="1"
                      max={productoElegido?.stock ?? 1}
                      step="1"
                      value={cantidad}
                      onChange={(event) => setCantidad(event.target.value)}
                      required
                    />
                  </label>
                  <button className="dm-button dm-button-primary" type="submit">
                    <Plus size={16} aria-hidden="true" /> Agregar
                  </button>
                  {productoElegido && (
                    <p className="quote-stock">
                      Disponibles: {productoElegido.stock} bultos
                    </p>
                  )}
                </form>
                {avisoManual && (
                  <p
                    className="dm-alert quote-alert"
                    data-tone="danger"
                    role="alert"
                  >
                    {avisoManual}
                  </p>
                )}
                {pedidoManual.length > 0 ? (
                  <>
                    <div className="quote-table-wrap">
                      <table className="quote-table">
                        <caption>Tu presupuesto manual</caption>
                        <thead>
                          <tr>
                            <th scope="col">Producto</th>
                            <th scope="col">Cantidad</th>
                            <th scope="col">Precio</th>
                            <th scope="col">Subtotal</th>
                            <th scope="col">
                              <span className="visually-hidden">Acciones</span>
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {pedidoManual.map((item) => (
                            <tr key={item.id}>
                              <th scope="row">
                                {item.nombre}
                                <small>{item.presentacion}</small>
                              </th>
                              <td>{item.cantidad}</td>
                              <td>{formatoPesos.format(item.precio)}</td>
                              <td>
                                {formatoPesos.format(
                                  item.precio * item.cantidad,
                                )}
                              </td>
                              <td>
                                <button
                                  className="quote-remove"
                                  type="button"
                                  onClick={() => quitarProducto(item.id)}
                                  aria-label={`Quitar ${item.nombre}`}
                                >
                                  Quitar
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div className="quote-total">
                      <span>Total estimado</span>
                      <strong>{formatoPesos.format(totalManual)}</strong>
                      <span>Total en dólares</span>
                      <strong>
                        {cotizacion?.valorVenta
                          ? formatoDolares.format(
                              totalManual / cotizacion.valorVenta,
                            )
                          : "Sin cotización disponible"}
                      </strong>
                      {cotizacion?.valorVenta && (
                        <small>
                          Dólar vendedor:{" "}
                          {formatoPesos.format(cotizacion.valorVenta)} ·
                          cotización del {cotizacion.fecha}
                        </small>
                      )}
                    </div>
                    <ProximosPasos />
                  </>
                ) : (
                  <p className="quote-empty">
                    Agregá un producto para empezar el presupuesto.
                  </p>
                )}
              </>
            )}
          </section>
        )}
      </div>
    </section>
  );
}

function ProximosPasos() {
  return (
    <div className="quote-next">
      <h3>Próximos pasos</h3>
      <p>
        Este presupuesto es una estimación. El pedido lo carga nuestro preventor
        cuando visita tu comercio; con tu cuenta después lo seguís y lo pagás
        online.
      </p>
      <div className="quote-next-actions">
        <Link
          className="dm-button dm-button-primary dm-button-sm"
          to="/registro"
        >
          Crear cuenta de comercio
        </Link>
        <Link to="/como-comprar">Ver cómo comprar</Link>
      </div>
    </div>
  );
}

function ResultadoPresupuesto({ resultado }) {
  return (
    <section
      className="quote-result"
      aria-labelledby="resultado-titulo"
      aria-live="polite"
    >
      <h3 id="resultado-titulo" className="quote-result-title">
        <ReceiptText size={18} aria-hidden="true" /> Tu presupuesto
      </h3>
      {resultado.items?.length > 0 ? (
        <div className="quote-table-wrap">
          <table className="quote-table">
            <caption className="visually-hidden">Productos incluidos</caption>
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col">Bultos</th>
                <th scope="col">Precio unitario</th>
                <th scope="col">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {resultado.items.map((item) => (
                <tr key={item.idProducto}>
                  <th scope="row">
                    {item.nombre}
                    <small>{item.presentacion}</small>
                  </th>
                  <td>{item.cantidad}</td>
                  <td>
                    <Importe valor={item.precioUnitario} />
                  </td>
                  <td>
                    <Importe valor={item.subtotal} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="quote-empty">
          No encontramos productos disponibles para incluir en este pedido.
        </p>
      )}
      {resultado.excluidos?.length > 0 && (
        <div className="quote-excluded">
          <h4>
            <CircleAlert size={16} aria-hidden="true" /> Productos que no
            pudimos incluir
          </h4>
          <ul>
            {resultado.excluidos.map((item, indice) => (
              <li key={`${item.producto}-${indice}`}>
                <strong>{item.producto}</strong> · {item.cantidad} bultos{" "}
                <span>{item.motivo}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      <div className="quote-total">
        <span>Total estimado</span>
        <strong>
          <Importe valor={resultado.total} />
        </strong>
        <span>Total en dólares</span>
        <strong>
          <Importe valor={resultado.totalDolares} dolares />
        </strong>
        {resultado.cotizacionDolar && (
          <small>
            Dólar vendedor: {formatoPesos.format(resultado.cotizacionDolar)} ·
            cotización del {resultado.fechaCotizacion}
          </small>
        )}
      </div>
      <p className="quote-disclaimer">
        Los precios y el stock pueden cambiar. Este presupuesto no reserva
        productos.
      </p>
      <ProximosPasos />
    </section>
  );
}
