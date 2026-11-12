import { Link } from "react-router-dom";
import {
  ArrowRight,
  BadgeCheck,
  MessageCircle,
  PackageCheck,
  Store,
  Truck,
  UserPlus,
} from "lucide-react";
import DollarQuote from "../../company/components/DollarQuote.jsx";
import "../styles.css";

const range = [
  {
    name: "Alfajores",
    description: "De chocolate, maicena y glaseados, por caja.",
    image: "alfajores-780",
    width: 780,
    height: 517,
    alt: "Alfajores argentinos de chocolate y maicena cortados al medio",
  },
  {
    name: "Chocolates",
    description: "Bombones, tabletas y golosinas bañadas.",
    image: "chocolates",
    width: 960,
    height: 488,
    alt: "Bombones de chocolate en una caja abierta",
  },
  {
    name: "Gomitas",
    description: "Surtidos de colores para el mostrador.",
    image: "gomitas",
    width: 1600,
    height: 1200,
    alt: "Gomitas de colores con forma de ositos",
  },
  {
    name: "Caramelos",
    description: "Caramelos duros para venta por unidad.",
    image: "caramelos",
    width: 1600,
    height: 1007,
    alt: "Caramelos dorados con envoltorio transparente",
  },
];

const pasos = [
  {
    icon: Store,
    title: "Mirá el catálogo",
    text: "Precios mayoristas por bulto, sin registrarte. Para una cotización rápida, usá el chat de presupuesto.",
  },
  {
    icon: PackageCheck,
    title: "Hacé el pedido",
    text: "Nuestro preventor visita tu comercio, lo registra si es la primera vez y carga el pedido con las cantidades que elegiste.",
  },
  {
    icon: Truck,
    title: "Recibilo en tu comercio",
    text: "Almacén verifica el stock, el pedido se factura y te lo entregamos.",
  },
  {
    icon: UserPlus,
    title: "Seguí y pagá online",
    text: "Con tu cuenta ves el envío, descargás la factura y pagás con Mercado Pago desde «Mis compras».",
  },
];

export default function HomePage() {
  return (
    <>
      <section className="home-hero" data-enter>
        <div className="wrap home-hero-grid">
          <div className="enter-copy home-hero-copy">
            <p className="home-hero-badge">
              <BadgeCheck size={16} aria-hidden="true" />
              Venta mayorista para kioscos y almacenes
            </p>
            <h1>Golosinas por mayor en Mendoza</h1>
            <p className="home-hero-text">
              Dulce Mayor distribuye alfajores, chocolates y golosinas por bulto
              para kioscos, almacenes y comercios minoristas. Consultá precios,
              pedí con tu preventor y seguí tus compras online.
            </p>
            <div className="home-hero-actions">
              <Link
                className="dm-button dm-button-primary dm-button-lg"
                to="/catalogo"
              >
                Ver catálogo y precios{" "}
                <ArrowRight size={18} aria-hidden="true" />
              </Link>
              <Link className="dm-button dm-button-lg" to="/como-comprar">
                Cómo comprar
              </Link>
            </div>
            <Link className="home-hero-chat" to="/presupuesto">
              <MessageCircle size={18} aria-hidden="true" />
              <span>
                <strong>¿Querés saber cuánto te sale?</strong> Armá un
                presupuesto en el chat
              </span>
            </Link>
          </div>
          <div className="home-hero-media">
            <picture className="home-hero-photo">
              <source
                media="(max-width: 700px)"
                srcSet="/fotos/alfajores-780.webp"
              />
              <img
                src="/fotos/alfajores.webp"
                width="1920"
                height="1272"
                fetchPriority="high"
                alt="Alfajores argentinos de chocolate y maicena cortados al medio"
              />
            </picture>
            <p className="home-hero-float">
              <Truck size={20} aria-hidden="true" />
              <span>
                <strong>Entrega en tu comercio</strong>
                <small>Pedido facturado y con seguimiento</small>
              </span>
            </p>
          </div>
        </div>
      </section>

      <DollarQuote />

      <section className="section" aria-labelledby="surtido-h">
        <div className="wrap">
          <header className="section-head">
            <span className="script-label">Nuestro surtido</span>
            <h2 id="surtido-h">
              Todo para reponer tu <em>mostrador</em>
            </h2>
            <span className="sline" aria-hidden="true" />
            <p>
              Vendemos por bulto o caja cerrada, con precio mayorista vigente.
            </p>
          </header>
          <ul className="home-range" role="list">
            {range.map(({ name, description, image, width, height, alt }) => (
              <li key={image}>
                <Link to="/catalogo" className="home-range-card">
                  <img
                    src={`/fotos/${image}.webp`}
                    width={width}
                    height={height}
                    loading="lazy"
                    decoding="async"
                    alt={alt}
                  />
                  <span className="home-range-body">
                    <strong>{name}</strong>
                    <small>{description}</small>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          <p className="home-range-more">
            <Link className="dm-button" to="/catalogo">
              Ver todos los productos{" "}
              <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </p>
        </div>
      </section>

      <section className="section section-alt" aria-labelledby="pasos-h">
        <div className="wrap">
          <header className="section-head">
            <span className="script-label">Así de simple</span>
            <h2 id="pasos-h">
              Cómo comprar en <em>4 pasos</em>
            </h2>
            <span className="sline" aria-hidden="true" />
          </header>
          <ol className="home-steps" role="list">
            {pasos.map(({ icon: Icon, title, text }, index) => (
              <li key={title}>
                <span className="home-step-icon" aria-hidden="true">
                  <Icon size={24} />
                </span>
                <span className="home-step-number" aria-hidden="true">
                  {index + 1}
                </span>
                <h3>{title}</h3>
                <p>{text}</p>
              </li>
            ))}
          </ol>
          <p className="home-steps-more">
            <Link to="/como-comprar">
              Ver el detalle de cómo comprar{" "}
              <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </p>
        </div>
      </section>

      <section className="home-cta" aria-labelledby="cta-h">
        <div className="wrap home-cta-inner">
          <div>
            <span className="script-label">¿Tenés un comercio?</span>
            <h2 id="cta-h">Creá tu cuenta y seguí tus pedidos online</h2>
            <p>
              Mirá el estado de cada envío, descargá tus facturas y pagá con
              Mercado Pago desde un solo lugar.
            </p>
          </div>
          <div className="home-cta-actions">
            <Link
              className="dm-button dm-button-primary dm-button-lg"
              to="/registro"
            >
              Crear mi cuenta
            </Link>
            <Link className="home-cta-link" to="/contacto">
              Hablar con nosotros
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
