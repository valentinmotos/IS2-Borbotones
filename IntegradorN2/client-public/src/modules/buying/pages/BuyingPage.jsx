import { Link } from "react-router-dom";
import { ArrowRight, ClipboardList, Handshake, Truck } from "lucide-react";

export default function BuyingPage() {
  return (
    <>
      <section className="inner-hero">
        <p className="eyebrow">PARA TU COMERCIO</p>
        <h1>
          Un recorrido simple.
          <br />
          <em>Un negocio más dulce.</em>
        </h1>
        <p>
          Conocé cómo se conecta tu comercio con Dulce Mayor, desde la visita
          del preventor hasta la entrega.
        </p>
      </section>
      <section className="buying-steps" aria-label="Cómo comprar">
        {[
          {
            icon: Handshake,
            title: "Nos encontramos en tu comercio",
            text: "El preventor visita el punto de venta y acompaña al minorista. Los datos del comercio y su ubicación permiten organizar el recorrido.",
          },
          {
            icon: ClipboardList,
            title: "El preventor toma tu pedido",
            text: "Los productos se eligen por presentación y disponibilidad. El preventor registra las cantidades y las promociones que correspondan.",
          },
          {
            icon: Truck,
            title: "Del almacén a tu mostrador",
            text: "El pedido pasa por el control de stock y la facturación antes de la distribución y entrega en el punto de venta.",
          },
        ].map(({ icon: Icon, title, text }, i) => (
          <article key={title}>
            <div className="step-heading">
              <span className="feature-icon">
                <Icon size={27} />
              </span>
              <span>0{i + 1}</span>
            </div>
            <h2>{title}</h2>
            <p>{text}</p>
          </article>
        ))}
      </section>
      <section className="buying-note">
        <div>
          <p className="eyebrow">ESTAMOS PREPARANDO MÁS</p>
          <h2>Muy pronto, más cerca.</h2>
          <p>
            El catálogo online y el acceso a tu cuenta estarán disponibles
            próximamente. Esta página presenta nuestro circuito de compra;
            todavía no recibe pedidos.
          </p>
        </div>
        <Link className="text-link" to="/empresa">
          Conocé nuestra empresa <ArrowRight size={17} />
        </Link>
      </section>
    </>
  );
}
