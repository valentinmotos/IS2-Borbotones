import { Link } from "react-router-dom";
import {
  ArrowRight,
  CreditCard,
  FileText,
  PackageSearch,
  Truck,
  Wallet,
} from "lucide-react";
import "../styles.css";

const steps = [
  {
    title: "Mirá el catálogo y los precios",
    description:
      "Consultá productos y precios mayoristas por bulto sin registrarte. Si querés una cotización estimada, pedila en el chat de presupuesto (no reserva stock).",
    links: [
      { to: "/catalogo", label: "Ver catálogo" },
      { to: "/presupuesto", label: "Armar presupuesto" },
    ],
  },
  {
    title: "El preventor visita tu comercio",
    description:
      "Si es la primera vez, registra tu comercio y tu cuenta. Después carga el pedido con los productos y las cantidades por bulto que elegiste.",
    links: [],
  },
  {
    title: "Preparamos, facturamos y entregamos",
    description:
      "Almacén verifica el stock, el pedido se factura y lo entregamos en tu comercio.",
    links: [],
  },
  {
    title: "Seguí y pagá online",
    description:
      "Con tu cuenta ves el estado del envío, descargás la factura y pagás con Mercado Pago. Si el preventor no te la creó, podés crearla vos y activarla con el código que te llega por correo.",
    links: [{ to: "/registro", label: "Crear cuenta" }],
  },
];

const accountFeatures = [
  {
    icon: Truck,
    title: "Seguir el envío",
    text: "Mirá en qué estado está cada pedido, desde que se carga hasta que llega a tu comercio.",
  },
  {
    icon: FileText,
    title: "Descargar facturas",
    text: "Tenés las facturas de tus compras disponibles para descargar cuando las necesites.",
  },
  {
    icon: CreditCard,
    title: "Pagar online",
    text: "Pagá tus pedidos con Mercado Pago sin moverte del mostrador.",
  },
];

export default function BuyingPage() {
  return (
    <>
      <div className="page buying-page">
        <div className="wrap">
          <header className="page-head">
            <div>
              <p className="eyebrow">Compras mayoristas</p>
              <h1>Cómo comprar</h1>
              <p className="lead">
                Comprás por bulto a través de nuestro preventor. Estos son los
                pasos, desde que mirás los precios hasta que recibís el pedido.
              </p>
            </div>
            <div className="page-head-actions">
              <Link className="dm-button dm-button-primary" to="/registro">
                Crear cuenta de comercio
              </Link>
            </div>
          </header>

          <section aria-labelledby="pasos-titulo">
            <h2 id="pasos-titulo" className="visually-hidden">
              Paso a paso
            </h2>
            <ol className="steps buying-steps" role="list">
              {steps.map(({ title, description, links }) => (
                <li key={title}>
                  <h3>{title}</h3>
                  <p>{description}</p>
                  {links.length > 0 && (
                    <p className="buying-step-links">
                      {links.map(({ to, label }) => (
                        <Link key={to} to={to}>
                          {label} <ArrowRight size={14} aria-hidden="true" />
                        </Link>
                      ))}
                    </p>
                  )}
                </li>
              ))}
            </ol>
          </section>
        </div>
      </div>

      <section
        className="section section-alt buying-account"
        aria-labelledby="cuenta-titulo"
      >
        <div className="wrap">
          <header className="section-head is-left">
            <span className="script-label">Con tu cuenta</span>
            <h2 id="cuenta-titulo">Desde tu cuenta podés…</h2>
            <span className="sline" aria-hidden="true" />
            <p>
              Todo está en <Link to="/cuenta/compras">Mis compras</Link>, dentro
              de tu cuenta de comercio.
            </p>
          </header>
          <ul className="buying-features" role="list">
            {accountFeatures.map(({ icon: Icon, title, text }) => (
              <li key={title}>
                <span className="buying-feature-icon" aria-hidden="true">
                  <Icon size={22} />
                </span>
                <h3>{title}</h3>
                <p>{text}</p>
              </li>
            ))}
          </ul>
          <aside className="buying-payment" aria-labelledby="pagos-titulo">
            <span className="buying-feature-icon" aria-hidden="true">
              <Wallet size={22} />
            </span>
            <div>
              <h3 id="pagos-titulo">Medios de pago</h3>
              <p>
                Aceptamos pagos en pesos o en dólares, al valor del dólar
                oficial (venta). El pago online se hace con Mercado Pago desde
                «Mis compras».
              </p>
            </div>
          </aside>
        </div>
      </section>

      <section className="buying-cta" aria-labelledby="cta-titulo">
        <div className="wrap buying-cta-inner">
          <div>
            <span className="script-label">¿Empezamos?</span>
            <h2 id="cta-titulo">Creá la cuenta de tu comercio</h2>
            <p>
              Con tu cuenta seguís tus pedidos, descargás las facturas y pagás
              online desde un solo lugar.
            </p>
          </div>
          <div className="buying-cta-actions">
            <Link
              className="dm-button dm-button-primary dm-button-lg"
              to="/registro"
            >
              Crear cuenta
            </Link>
            <Link className="buying-cta-link" to="/catalogo">
              <PackageSearch size={18} aria-hidden="true" /> Ver catálogo
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
