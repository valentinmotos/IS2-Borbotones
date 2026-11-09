import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useTasteMoment } from "../../../../../packages/design/useTasteMoment.js";
import lettersAssemble from "../../../../../packages/design/taste/letters-assemble.mjs";

const workflow = [
  {
    to: "/guia#catalogo",
    title: "Productos y almacén",
    text: "Surtido y existencias.",
  },
  {
    to: "/guia#comercios",
    title: "Comercios y preventa",
    text: "Puntos de venta y pedidos.",
  },
  {
    to: "/guia#recorrido",
    title: "Facturación y entregas",
    text: "Despacho y confirmación.",
  },
];

export default function HomePage() {
  const statement = useTasteMoment(lettersAssemble, false);
  return (
    <>
      <section className="work-intro hero" data-enter>
        <div className="enter-copy">
          <h1>Administración</h1>
          <p>Herramientas y guías para el equipo mayorista de Dulce Mayor.</p>
        </div>
        <div className="work-tools">
          <Link className="work-tool" to="/componentes">
            <span>
              <strong>Sistema de diseño</strong>
              <small>Probá los componentes en el registro de categorías.</small>
            </span>
            <ArrowUpRight size={22} aria-hidden="true" />
          </Link>
          <Link className="work-tool" to="/guia">
            <span>
              <strong>Guía de trabajo</strong>
              <small>Consultá las funciones de cada área.</small>
            </span>
            <ArrowUpRight size={22} aria-hidden="true" />
          </Link>
          <Link className="work-tool" to="/ubicacion">
            <span>
              <strong>Ubicación</strong>
              <small>Administrá departamentos y localidades.</small>
            </span>
            <ArrowUpRight size={22} aria-hidden="true" />
          </Link>
        </div>
      </section>
      <section
        ref={statement}
        className="work-flow"
        data-moment="letters-assemble"
      >
        <h2 className="mo-la__type">Circuito mayorista</h2>
        <ol>
          {workflow.map(({ to, title, text }, index) => (
            <li key={to}>
              <Link to={to}>
                <span className="work-number" aria-hidden="true">
                  0{index + 1}
                </span>
                <strong>{title}</strong>
                <small>{text}</small>
                <ArrowUpRight size={18} aria-hidden="true" />
              </Link>
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}
