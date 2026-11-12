import {
  estadosPedido,
  formatearFechaHora,
  recorridoEnvio,
} from "../estados.js";

// E4-08: estado del envío. Muestra los cambios registrados (historial de E4-01) en orden y, si el
// pedido sigue en curso, los pasos que faltan del recorrido normal como pendientes.
export default function LineaTiempo({ eventos, estadoActual }) {
  const ocurridos = new Set(eventos.map((evento) => evento.estado));
  const pendientes =
    estadoActual === "ANULADO"
      ? []
      : recorridoEnvio.filter((estado) => !ocurridos.has(estado));

  return (
    <ol className="compras-tiempo" aria-label="Estado del envío">
      {eventos.map((evento, indice) => (
        <li
          key={`${evento.estado}-${indice}`}
          data-paso="hecho"
          data-tono={estadosPedido[evento.estado]?.tono}
          aria-current={indice === eventos.length - 1 ? "step" : undefined}
        >
          <strong>
            {estadosPedido[evento.estado]?.etiqueta ?? evento.estado}
          </strong>
          <time dateTime={evento.fechaHora}>
            {formatearFechaHora(evento.fechaHora)}
          </time>
          {evento.observacion && <span>{evento.observacion}</span>}
          {evento.transportista && (
            <span>Transportista: {evento.transportista}</span>
          )}
        </li>
      ))}
      {pendientes.map((estado) => (
        <li key={estado} data-paso="pendiente">
          <strong>{estadosPedido[estado].etiqueta}</strong>
          <span>Pendiente</span>
        </li>
      ))}
    </ol>
  );
}
