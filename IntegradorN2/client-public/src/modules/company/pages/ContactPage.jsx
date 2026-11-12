import { Link } from "react-router-dom";
import {
  ArrowRight,
  MapPinned,
  MessageCircle,
  PackageSearch,
  Store,
} from "lucide-react";
import Branches from "../components/Branches.jsx";
import "../styles.css";

const shortcuts = [
  {
    icon: MessageCircle,
    to: "/presupuesto",
    cta: "Armar presupuesto",
    title: "¿Querés saber cuánto te sale?",
    text: "Armá un presupuesto estimado en el chat.",
  },
  {
    icon: Store,
    to: "/como-comprar",
    cta: "Ver cómo comprar",
    title: "¿Querés empezar a comprar?",
    text: "Mirá los pasos para hacer tu primer pedido.",
  },
  {
    icon: PackageSearch,
    to: "/cuenta/compras",
    cta: "Ir a Mis compras",
    title: "¿Ya sos cliente?",
    text: "Seguí tus pedidos y descargá tus facturas en «Mis compras».",
  },
];

export default function ContactPage() {
  return (
    <div className="page contact-page">
      <div className="wrap">
        <header className="page-head">
          <div>
            <p className="eyebrow">Atención a comercios</p>
            <h1>Contacto</h1>
            <p className="lead">
              Atendemos consultas sobre pedidos y compras mayoristas en nuestras
              sucursales. Acercate a la que te quede más cerca, o resolvé lo más
              común desde la web.
            </p>
          </div>
        </header>

        <div className="split contact-layout">
          <section aria-labelledby="direcciones-titulo">
            <div className="contact-section-head">
              <h2 id="direcciones-titulo">Direcciones de atención</h2>
              <Link className="contact-map-link" to="/empresa#sucursales">
                <MapPinned size={16} aria-hidden="true" /> Ver todas las
                sucursales en el mapa
              </Link>
            </div>
            <Branches />
          </section>

          <aside
            className="panel panel-tint contact-aside"
            aria-labelledby="atajos-titulo"
          >
            <h2 id="atajos-titulo">Antes de acercarte</h2>
            <ul className="contact-shortcuts" role="list">
              {shortcuts.map(({ icon: Icon, to, cta, title, text }) => (
                <li key={to}>
                  <span className="contact-shortcut-icon" aria-hidden="true">
                    <Icon size={18} />
                  </span>
                  <span>
                    <strong>{title}</strong>
                    <small>{text}</small>
                    <Link to={to}>
                      {cta} <ArrowRight size={14} aria-hidden="true" />
                    </Link>
                  </span>
                </li>
              ))}
            </ul>
          </aside>
        </div>
      </div>
    </div>
  );
}
