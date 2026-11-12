import { Link } from "react-router-dom";
import {
  ArrowRight,
  Building2,
  ClipboardList,
  PackageCheck,
  Truck,
} from "lucide-react";
import Branches from "../components/Branches.jsx";
import { usePublicData } from "../usePublicData.js";
import "../styles.css";

const flow = [
  {
    icon: ClipboardList,
    title: "Preventa en tu comercio",
    text: "Los preventores visitan cada comercio y toman los pedidos.",
  },
  {
    icon: PackageCheck,
    title: "Preparación en almacén",
    text: "Almacén verifica el stock y prepara los bultos de cada pedido.",
  },
  {
    icon: Truck,
    title: "Entrega en el punto de venta",
    text: "El distribuidor lleva el pedido facturado hasta tu comercio.",
  },
];

export default function CompanyPage() {
  const company = usePublicData("/publico/empresa");
  return (
    <>
      <div className="page company-page">
        <div className="wrap">
          <header className="page-head">
            <div>
              <p className="eyebrow">La empresa</p>
              <h1>Mayorista de golosinas en Mendoza</h1>
              <p className="lead">
                Dulce Mayor abastece a kioscos, almacenes y otros comercios
                minoristas que compran por bulto y venden por unidad.
              </p>
            </div>
            <div className="page-head-actions">
              <Link
                className="dm-button dm-button-primary"
                to="/empresa#sucursales"
              >
                Ver sucursales <ArrowRight size={17} aria-hidden="true" />
              </Link>
              <Link className="dm-button" to="/como-comprar">
                Cómo comprar
              </Link>
            </div>
          </header>

          <div className="company-overview">
            <section className="panel" aria-labelledby="datos-titulo">
              <h2 id="datos-titulo">Datos de la empresa</h2>
              {company.status === "loading" && (
                <p className="muted" role="status">
                  Cargando datos…
                </p>
              )}
              {company.status === "error" && (
                <p className="muted" role="status">
                  No pudimos cargar los datos de la empresa.
                </p>
              )}
              {company.status === "ready" && (
                <ul className="company-records" role="list">
                  {company.data.map(({ id, razonSocial, cuit }) => (
                    <li key={id} className="shop-company-record">
                      <Building2 size={20} aria-hidden="true" />
                      <span>
                        <strong>{razonSocial}</strong>
                        <small>CUIT {cuit}</small>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="panel" aria-labelledby="trabajo-titulo">
              <h2 id="trabajo-titulo">Cómo trabajamos</h2>
              <ol className="company-flow" role="list">
                {flow.map(({ icon: Icon, title, text }) => (
                  <li key={title}>
                    <span className="company-flow-icon" aria-hidden="true">
                      <Icon size={20} />
                    </span>
                    <span>
                      <strong>{title}</strong>
                      <small>{text}</small>
                    </span>
                  </li>
                ))}
              </ol>
            </section>
          </div>
        </div>
      </div>

      <section
        id="sucursales"
        className="section section-alt company-branches"
        aria-labelledby="sucursales-title"
      >
        <div className="wrap">
          <header className="section-head is-left">
            <span className="script-label">Dónde estamos</span>
            <h2 id="sucursales-title">Sucursales</h2>
            <span className="sline" aria-hidden="true" />
            <p>
              Acercate a cualquiera de nuestras sucursales para consultas sobre
              pedidos y compras mayoristas.
            </p>
          </header>
          <Branches withMap />
        </div>
      </section>
    </>
  );
}
