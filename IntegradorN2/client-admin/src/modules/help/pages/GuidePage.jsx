import { Boxes, Store, Truck } from "lucide-react";

export default function GuidePage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">CONOCÉ TU ESPACIO</p>
          <h1>Un equipo. Todo un recorrido.</h1>
          <p>Así se conecta el trabajo de cada área de Dulce Mayor.</p>
        </div>
      </div>
      <div className="guide-intro">
        <span className="banner-label">GUÍA DE TRABAJO</span>
        <h2>
          Del almacén al comercio,
          <br />
          cada paso cuenta.
        </h2>
        <p>
          Esta guía presenta el circuito de la empresa. Las herramientas de
          gestión se incorporan en las próximas etapas.
        </p>
      </div>
      <div className="guide-sections">
        <section id="catalogo" className="guide-card">
          <span className="feature-icon pink">
            <Boxes />
          </span>
          <div>
            <p className="eyebrow">01 · EL SURTIDO</p>
            <h2>Productos y almacén</h2>
            <p>
              El equipo administrativo mantiene los productos, sus categorías,
              imágenes y presentaciones por bulto. Almacén controla las
              existencias y registra cada movimiento.
            </p>
            <p>
              El catálogo público muestra los productos con stock disponible y
              sus precios mayoristas vigentes.
            </p>
          </div>
        </section>
        <section id="comercios" className="guide-card">
          <span className="feature-icon green">
            <Store />
          </span>
          <div>
            <p className="eyebrow">02 · LOS COMERCIOS</p>
            <h2>Cerca de cada minorista</h2>
            <p>
              Los preventores visitan los puntos de venta, registran comercios y
              toman los pedidos desde su teléfono. Cada punto de venta tiene su
              dirección y ubicación.
            </p>
            <p>
              El jefe administrativo coordina los usuarios y consulta los
              reportes de la operación.
            </p>
          </div>
        </section>
        <section id="recorrido" className="guide-card">
          <span className="feature-icon orange">
            <Truck />
          </span>
          <div>
            <p className="eyebrow">03 · EL RECORRIDO</p>
            <h2>Del pedido a la entrega</h2>
            <p>
              La operación conecta el control de stock, la facturación y la
              distribución. El distribuidor sigue su ruta y registra la entrega
              al comercio.
            </p>
            <p>
              Los minoristas consultan sus pedidos y facturas desde su cuenta en
              el sitio público.
            </p>
          </div>
        </section>
      </div>
    </>
  );
}
