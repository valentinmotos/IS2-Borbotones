const sections = [
  {
    id: "catalogo",
    title: "Productos y almacén",
    description:
      "Administración mantiene las categorías, imágenes y presentaciones por bulto. Almacén controla las existencias y registra los movimientos. El catálogo muestra los productos con stock y sus precios vigentes.",
  },
  {
    id: "comercios",
    title: "Comercios y preventa",
    description:
      "El preventor visita los puntos de venta, registra a los comercios minoristas y toma los pedidos desde el teléfono. El jefe administrativo coordina los usuarios y consulta los reportes.",
  },
  {
    id: "recorrido",
    title: "Facturación y entregas",
    description:
      "Almacén prepara el pedido y verifica el stock. La facturación precede al reparto. El distribuidor sigue su ruta y registra la entrega en cada comercio. El minorista consulta sus pedidos y facturas desde su cuenta.",
  },
];

export default function GuidePage() {
  return (
    <>
      <header className="dm-page-header">
        <div>
          <h1>Guía de trabajo</h1>
          <p>
            Responsabilidades de cada área, desde el surtido hasta la entrega.
          </p>
        </div>
      </header>
      <nav className="guide-index" aria-label="En esta guía">
        <h2>En esta guía</h2>
        <ul>
          {sections.map(({ id, title }) => (
            <li key={id}>
              <a href={`#${id}`}>{title}</a>
            </li>
          ))}
        </ul>
      </nav>
      {sections.map(({ id, title, description }) => (
        <section key={id} id={id} className="guide-section">
          <h2>{title}</h2>
          <p>{description}</p>
        </section>
      ))}
    </>
  );
}
