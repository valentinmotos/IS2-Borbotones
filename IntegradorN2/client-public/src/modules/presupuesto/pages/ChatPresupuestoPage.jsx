import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
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
  if (valor === null || valor === undefined) return <span>Sin cotización disponible</span>;
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

  const productoElegido = catalogo.find((producto) => producto.id === productoId);
  const totalManual = useMemo(
    () => pedidoManual.reduce((total, item) => total + item.precio * item.cantidad, 0),
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
      setProductoId((actual) => disponibles.some((item) => item.id === actual)
        ? actual
        : (disponibles[0]?.id ?? ""));
    } else {
      setError("No pudimos cargar los productos para el formulario. Probá nuevamente más tarde.");
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
      const resultado = await api.post("/publico/chatbot/presupuesto", { mensaje: consulta });
      setRespuesta(resultado.data);
      setFallback(false);
    } catch (fallo) {
      if (fallo.status === 503) {
        setError("El asistente no está disponible ahora. Podés armar el presupuesto con el formulario.");
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
    const existente = pedidoManual.find((item) => item.id === productoElegido.id);
    const nuevaCantidad = unidades + (existente?.cantidad ?? 0);
    if (nuevaCantidad > productoElegido.stock) {
      setAvisoManual(`Hay ${productoElegido.stock} bultos disponibles de ${productoElegido.nombre}.`);
      return;
    }
    setPedidoManual((actual) => existente
      ? actual.map((item) => item.id === productoElegido.id ? { ...item, cantidad: nuevaCantidad } : item)
      : [...actual, { ...productoElegido, cantidad: unidades }]);
    setAvisoManual("");
    setCantidad("1");
  }

  function quitarProducto(id) {
    setPedidoManual((actual) => actual.filter((item) => item.id !== id));
  }

  return (
    <section className="quote-page wrap" aria-labelledby="presupuesto-titulo">
      <header className="quote-heading">
        <p className="quote-kicker">Atención mayorista</p>
        <h1 id="presupuesto-titulo">Armá tu presupuesto</h1>
        <p>Contanos qué productos y cantidades necesitás. El asistente revisa disponibilidad y precios vigentes.</p>
      </header>

      <div className="quote-grid">
        <section className="quote-chat" aria-labelledby="chat-titulo">
          <div className="quote-chat-heading">
            <div><span className="quote-status-dot" aria-hidden="true" /><span>Asistente de Dulce Mayor</span></div>
            <h2 id="chat-titulo">¿Qué estás buscando?</h2>
            <p>Por ejemplo: “{consultaSugerida}”.</p>
          </div>
          <form className="quote-composer" onSubmit={enviar}>
            <label htmlFor="mensaje-presupuesto">Tu pedido</label>
            <textarea
              id="mensaje-presupuesto"
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
              <button className="dm-button dm-button-primary" type="submit" disabled={ocupado || !mensaje.trim()}>
                {ocupado ? "Armando presupuesto…" : "Consultar"}
              </button>
            </div>
          </form>
          {ocupado && <p className="quote-typing" role="status"><span aria-hidden="true" /> El asistente está escribiendo…</p>}
          {error && <p className="quote-alert" role="alert">{error}</p>}
          {respuesta && <ResultadoPresupuesto resultado={respuesta} />}
        </section>

        <aside className="quote-aside">
          <h2>¿Preferís explorar?</h2>
          <p>También podés revisar todos los productos disponibles y sus presentaciones.</p>
          <Link className="dm-button" to="/catalogo">Ver catálogo</Link>
          <p className="quote-note">El presupuesto es orientativo y no reserva stock.</p>
        </aside>
      </div>

      {fallback && <section className="quote-fallback" aria-labelledby="formulario-manual-titulo">
        <div className="quote-fallback-heading">
          <p className="quote-kicker">Alternativa manual</p>
          <h2 id="formulario-manual-titulo">Armá el presupuesto por producto</h2>
          <p>Seleccioná productos disponibles y cantidades. Los precios corresponden al catálogo vigente.</p>
        </div>
        {catalogo.length === 0 ? <p role="status">{error || "Cargando productos disponibles…"}</p> : <>
          <form className="quote-manual-form" onSubmit={agregarProducto}>
            <label className="quote-product-field">Producto
              <select value={productoId} onChange={(event) => setProductoId(event.target.value)} required>
                {catalogo.map((producto) => <option key={producto.id} value={producto.id}>{producto.nombre} · {producto.presentacion} · {formatoPesos.format(producto.precio)}</option>)}
              </select>
            </label>
            <label className="quote-quantity-field">Bultos
              <input type="number" min="1" max={productoElegido?.stock ?? 1} step="1" value={cantidad} onChange={(event) => setCantidad(event.target.value)} required />
            </label>
            {productoElegido && <p className="quote-stock">Disponibles: {productoElegido.stock} bultos</p>}
            <button className="dm-button dm-button-primary" type="submit">Agregar</button>
          </form>
          {avisoManual && <p className="quote-alert" role="alert">{avisoManual}</p>}
          {pedidoManual.length > 0 ? <>
            <div className="quote-table-wrap"><table className="quote-table">
              <caption>Tu presupuesto manual</caption>
              <thead><tr><th scope="col">Producto</th><th scope="col">Cantidad</th><th scope="col">Precio</th><th scope="col">Subtotal</th><th scope="col"><span className="visually-hidden">Acciones</span></th></tr></thead>
              <tbody>{pedidoManual.map((item) => <tr key={item.id}>
                <th scope="row">{item.nombre}<small>{item.presentacion}</small></th>
                <td>{item.cantidad}</td><td>{formatoPesos.format(item.precio)}</td>
                <td>{formatoPesos.format(item.precio * item.cantidad)}</td>
                <td><button className="quote-remove" type="button" onClick={() => quitarProducto(item.id)} aria-label={`Quitar ${item.nombre}`}>Quitar</button></td>
              </tr>)}</tbody>
            </table></div>
            <div className="quote-manual-total"><span>Total estimado</span><strong>{formatoPesos.format(totalManual)}</strong>
              <span>Total en dólares</span><strong>{cotizacion?.valorVenta ? formatoDolares.format(totalManual / cotizacion.valorVenta) : "Sin cotización disponible"}</strong>
              {cotizacion?.valorVenta && <small>Dólar vendedor: {formatoPesos.format(cotizacion.valorVenta)} · cotización del {cotizacion.fecha}</small>}
            </div>
          </> : <p>Agregá un producto para empezar el presupuesto.</p>}
        </>}
      </section>}
    </section>
  );
}

function ResultadoPresupuesto({ resultado }) {
  return (
    <section className="quote-result" aria-labelledby="resultado-titulo" aria-live="polite">
      <div className="quote-result-heading"><p className="quote-kicker">Resultado</p><h2 id="resultado-titulo">Tu presupuesto</h2></div>
      {resultado.items?.length > 0 ? <div className="quote-table-wrap"><table className="quote-table">
        <caption>Productos incluidos</caption>
        <thead><tr><th scope="col">Producto</th><th scope="col">Bultos</th><th scope="col">Precio unitario</th><th scope="col">Subtotal</th></tr></thead>
        <tbody>{resultado.items.map((item) => <tr key={item.idProducto}><th scope="row">{item.nombre}<small>{item.presentacion}</small></th><td>{item.cantidad}</td><td><Importe valor={item.precioUnitario} /></td><td><Importe valor={item.subtotal} /></td></tr>)}</tbody>
      </table></div> : <p>No encontramos productos disponibles para incluir en este pedido.</p>}
      {resultado.excluidos?.length > 0 && <div className="quote-excluded"><h3>Productos que no pudimos incluir</h3><ul>{resultado.excluidos.map((item, indice) => <li key={`${item.producto}-${indice}`}><strong>{item.producto}</strong> · {item.cantidad} bultos <span>{item.motivo}</span></li>)}</ul></div>}
      <div className="quote-total">
        <span>Total estimado</span><strong><Importe valor={resultado.total} /></strong>
        <span>Total en dólares</span><strong><Importe valor={resultado.totalDolares} dolares /></strong>
        {resultado.cotizacionDolar && <small>Dólar vendedor: {formatoPesos.format(resultado.cotizacionDolar)} · cotización del {resultado.fechaCotizacion}</small>}
      </div>
      <p className="quote-note">Los precios y el stock pueden cambiar. Este presupuesto no reserva productos.</p>
    </section>
  );
}
