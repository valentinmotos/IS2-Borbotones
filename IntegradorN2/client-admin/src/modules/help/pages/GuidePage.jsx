import { Boxes, Store, Truck } from "lucide-react";
import "../styles.css";

const sections = [
  {
    id: "catalogo",
    title: "Productos y almacén",
    icon: Boxes,
    quien: "Jefatura y administración",
    pasos: [
      "Las categorías agrupan los productos del catálogo. Cada producto tiene código, presentación, unidades por bulto o pack e imagen.",
      "Los precios mayoristas se cargan con una fecha de vigencia. Al cargar uno nuevo, el anterior se cierra el día antes.",
      "En stock se registran los ingresos de mercadería y los ajustes manuales. Cada movimiento queda en el historial del producto.",
      "Los productos sin stock no se muestran en el catálogo hasta que se reponen.",
    ],
  },
  {
    id: "comercios",
    title: "Comercios y preventa",
    icon: Store,
    quien: "Preventores, con seguimiento de jefatura",
    pasos: [
      "El preventor visita los puntos de venta y registra a los comercios minoristas de cada zona.",
      "Desde el teléfono consulta el stock del almacén y carga los pedidos de cada comercio.",
      "Jefatura y administración consultan los puntos de venta registrados. Jefatura además administra los usuarios internos y sigue las estadísticas.",
    ],
  },
  {
    id: "recorrido",
    title: "Facturación y entregas",
    icon: Truck,
    quien: "Almacén, distribución y el minorista",
    pasos: [
      "Almacén revisa cada pedido recibido, verifica que haya stock suficiente y lo factura.",
      "Sobre la factura se registran los pagos hasta saldarla. La factura se puede descargar en PDF.",
      "Los pedidos facturados y pagados quedan listos para el reparto. El distribuidor arma su ruta del día en el mapa y marca cada pedido como entregado.",
      "El minorista consulta sus pedidos y facturas desde su cuenta en el sitio.",
    ],
  },
];

export default function GuidePage() {
  return (
    <div className="guide-page">
      <header className="page-head">
        <div>
          <span className="eyebrow">Ayuda</span>
          <h1>Guía de trabajo</h1>
          <p className="lead">
            Qué hace cada área, desde que se carga un producto hasta que el
            pedido llega al comercio.
          </p>
        </div>
      </header>
      <div className="guide-layout">
        <nav className="guide-index" aria-label="En esta guía">
          <p>En esta guía</p>
          <ol>
            {sections.map(({ id, title }) => (
              <li key={id}>
                <a href={`#${id}`}>{title}</a>
              </li>
            ))}
          </ol>
        </nav>
        <div className="guide-sections">
          {sections.map(({ id, title, icon: Icon, quien, pasos }) => (
            <section
              key={id}
              id={id}
              className="panel guide-section"
              aria-labelledby={`${id}-titulo`}
            >
              <div className="guide-section-head">
                <span className="guide-section-icon" aria-hidden="true">
                  <Icon size={20} />
                </span>
                <div>
                  <h2 id={`${id}-titulo`}>{title}</h2>
                  <p>{quien}</p>
                </div>
              </div>
              <ul>
                {pasos.map((paso) => (
                  <li key={paso}>{paso}</li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      </div>
    </div>
  );
}
