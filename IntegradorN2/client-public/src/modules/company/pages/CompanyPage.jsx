import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useTasteMoment } from "../../../../../packages/design/useTasteMoment.js";
import wordHighlight, {
  phone,
} from "../../../../../packages/design/taste/word-highlight.mjs";
import Branches from "../components/Branches.jsx";
import { usePublicData } from "../usePublicData.js";

export default function CompanyPage() {
  const statement = useTasteMoment(wordHighlight, phone);
  const company = usePublicData("/publico/empresa");
  return (
    <>
      <section className="shop-editorial shop-company hero" data-enter>
        <div className="wrap shop-editorial-grid enter-copy">
          <h1>Mayorista de golosinas en Mendoza</h1>
          <div>
            {company.status === "ready" &&
              company.data.map(({ id, razonSocial, cuit }) => (
                <p key={id} className="shop-company-record">
                  <strong>{razonSocial}</strong> · CUIT {cuit}
                </p>
              ))}
            <p>
              Dulce Mayor abastece a kioscos, almacenes y otros comercios
              minoristas que compran por bulto y venden por unidad.
            </p>
            <Link
              className="btn btn--primary dm-button dm-button-primary"
              to="/empresa#sucursales"
            >
              Ver sucursales <ArrowUpRight size={17} aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>
      <section
        id="sucursales"
        className="wrap shop-institutional"
        aria-labelledby="sucursales-title"
      >
        <h2 id="sucursales-title">Sucursales</h2>
        <Branches withMap />
      </section>
      <section
        ref={statement}
        className="shop-statement"
        data-moment="word-highlight"
      >
        <div className="wrap">
          <h2 className="mo-wh__text">
            Los preventores toman los pedidos en cada comercio.{" "}
            <strong>Almacén</strong> prepara los bultos y el distribuidor los
            entrega en el punto de venta.
          </h2>
        </div>
      </section>
    </>
  );
}
