import { Link } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  Boxes,
  HeartHandshake,
  MapPin,
  Store,
} from "lucide-react";
import SweetIllustration from "../components/SweetIllustration.jsx";

export default function HomePage() {
  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">
            <span /> DE MENDOZA, PARA TU COMERCIO
          </p>
          <h1>
            Lo bueno
            <br />
            se comparte.
            <br />
            <em>Lo dulce, también.</em>
          </h1>
          <p className="hero-description">
            Golosinas, chocolates y pequeños antojos con una gran misión: hacer
            crecer tu negocio.
          </p>
          <div className="hero-actions">
            <Link className="button" to="/empresa">
              Conocé Dulce Mayor <ArrowUpRight size={19} />
            </Link>
            <Link className="text-link" to="/como-comprar">
              Cómo comprar <ArrowRight size={17} />
            </Link>
          </div>
          <div className="hero-origin">
            <MapPin size={17} />
            <span>
              El lado dulce de Mendoza.
              <br />
              <strong>Cerca de cada comercio.</strong>
            </span>
          </div>
        </div>
        <div className="hero-art">
          <div className="round-label">
            PARA TU KIOSCO
            <br />
            <b>Y MUCHO MÁS</b>
            <span>✳</span>
          </div>
          <SweetIllustration />
          <span className="art-caption">
            Un mundo de cosas ricas, por mayor.
          </span>
        </div>
      </section>
      <div className="ticker" aria-hidden="true">
        <span>GOLOSINAS</span> ✳ <span>CHOCOLATES</span> ✳{" "}
        <span>ALFAJORES</span> ✳ <span>POR BULTO, POR MAYOR</span> ✳{" "}
        <span>PARA COMPARTIR</span>
      </div>
      <section className="value-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">PENSAMOS EN TU NEGOCIO</p>
            <h2>
              Más que golosinas.
              <br />
              <em>Un aliado para tu comercio.</em>
            </h2>
          </div>
          <p>
            Sabemos que detrás de cada mostrador
            <br />
            hay alguien con ganas de crecer.
          </p>
        </div>
        <div className="value-grid">
          <article>
            <span className="feature-icon pink">
              <Boxes size={27} />
            </span>
            <h3>El surtido que buscás</h3>
            <p>
              Golosinas y chocolates en presentaciones por bulto o pack,
              pensados para la venta en tu comercio.
            </p>
          </article>
          <article>
            <span className="feature-icon green">
              <HeartHandshake size={27} />
            </span>
            <h3>Un trato cercano</h3>
            <p>
              El preventor acompaña a cada minorista y recibe sus pedidos en el
              punto de venta.
            </p>
          </article>
          <article>
            <span className="feature-icon orange">
              <Store size={27} />
            </span>
            <h3>Espacio para crecer</h3>
            <p>
              Una propuesta mayorista para kioscos, almacenes y los comercios de
              nuestra provincia.
            </p>
          </article>
        </div>
      </section>
      <section className="public-cta">
        <span className="eyebrow">SEAMOS PARTE DE TU DÍA A DÍA</span>
        <h2>
          Tu próximo paso
          <br />
          tiene un lado dulce.
        </h2>
        <Link className="button light" to="/como-comprar">
          Conocé cómo comprar <ArrowRight size={18} />
        </Link>
        <span className="cta-star" aria-hidden="true">
          ✳
        </span>
      </section>
    </>
  );
}
