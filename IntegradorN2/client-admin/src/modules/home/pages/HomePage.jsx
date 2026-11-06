import { Link } from "react-router-dom";
import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  Boxes,
  Candy,
  MapPin,
  PackageCheck,
  Store,
  Truck,
} from "lucide-react";

export default function HomePage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">TU DÍA, UN POCO MÁS SIMPLE</p>
          <h1>Bienvenido a tu espacio.</h1>
          <p>Un lugar para acompañar cada paso de Dulce Mayor.</p>
        </div>
        <span className="location">
          <MapPin size={15} /> Mendoza, Argentina
        </span>
      </div>
      <section className="welcome-banner" aria-labelledby="bienvenida">
        <div>
          <span className="banner-label">CERCA DE CADA COMERCIO</span>
          <h2 id="bienvenida">
            Grandes negocios.
            <br />
            Pequeños momentos dulces.
          </h2>
          <p>
            De nuestro almacén a cada punto de venta.
            <br />
            Empezá por conocer cómo trabajamos.
          </p>
          <Link className="button light" to="/guia">
            Explorar la guía <ArrowRight size={17} />
          </Link>
        </div>
        <div className="banner-art" aria-hidden="true">
          <span className="art-circle" />
          <Candy className="candy-big" strokeWidth={1.1} />
          <span className="art-label">
            DULCE
            <br />
            <b>MAYOR</b>
            <small>DESDE MENDOZA</small>
          </span>
          <span className="art-spark">✳</span>
        </div>
      </section>
      <div className="section-heading">
        <h2>Una operación conectada</h2>
        <Link to="/guia">
          Conocer el recorrido <ArrowRight size={16} />
        </Link>
      </div>
      <div className="workflow-cards">
        {[
          {
            icon: Boxes,
            step: "01",
            title: "El surtido",
            text: "Productos, presentaciones por bulto y disponibilidad de almacén.",
            anchor: "catalogo",
            color: "pink",
          },
          {
            icon: Store,
            step: "02",
            title: "Los comercios",
            text: "El vínculo con cada minorista y su punto de venta.",
            anchor: "comercios",
            color: "green",
          },
          {
            icon: Truck,
            step: "03",
            title: "El recorrido",
            text: "Del pedido al despacho y la entrega de mercadería.",
            anchor: "recorrido",
            color: "orange",
          },
        ].map(({ icon: Icon, step, title, text, anchor, color }) => (
          <Link className="workflow-card" to={`/guia#${anchor}`} key={step}>
            <div className="card-top">
              <span className={`feature-icon ${color}`}>
                <Icon size={24} />
              </span>
              <span className="step">{step}</span>
            </div>
            <h3>{title}</h3>
            <p>{text}</p>
            <span className="card-link">
              Ver en la guía <ArrowUpRight size={17} />
            </span>
          </Link>
        ))}
      </div>
      <section className="getting-started">
        <div className="feature-icon pale">
          <BookOpen size={23} />
        </div>
        <div>
          <h2>¿Es tu primera visita?</h2>
          <p>Conocé los roles y el circuito de trabajo de Dulce Mayor.</p>
        </div>
        <Link className="text-link" to="/guia">
          Leer la guía <ArrowRight size={17} />
        </Link>
      </section>
      <p className="quiet-note">
        <PackageCheck size={16} /> Cada buena entrega empieza con un equipo
        conectado.
      </p>
    </>
  );
}
