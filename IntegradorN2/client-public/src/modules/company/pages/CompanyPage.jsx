import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useTasteMoment } from "../../../../../packages/design/useTasteMoment.js";
import wordHighlight, {
  phone,
} from "../../../../../packages/design/taste/word-highlight.mjs";

export default function CompanyPage() {
  const statement = useTasteMoment(wordHighlight, phone);
  return (
    <>
      <section className="shop-editorial hero" data-enter>
        <div className="wrap shop-editorial-grid enter-copy">
          <h1>Mayorista de golosinas en Mendoza</h1>
          <div>
            <p>
              Dulce Mayor abastece a kioscos, almacenes y otros comercios
              minoristas que compran por bulto y venden por unidad.
            </p>
            <Link
              className="btn btn--primary dm-button dm-button-primary"
              to="/como-comprar"
            >
              Ver cómo comprar <ArrowUpRight size={17} aria-hidden="true" />
            </Link>
          </div>
        </div>
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
