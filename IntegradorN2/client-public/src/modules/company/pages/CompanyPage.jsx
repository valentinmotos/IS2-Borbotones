import { Link } from "react-router-dom";
import { ArrowRight, Candy, MapPin } from "lucide-react";

export default function CompanyPage() {
  return (
    <>
      <section className="inner-hero">
        <p className="eyebrow">NUESTRA EMPRESA</p>
        <h1>
          De Mendoza.
          <br />
          <em>Del lado de tu negocio.</em>
        </h1>
        <p>
          Somos Dulce Mayor, una empresa mayorista de golosinas con una
          propuesta para los comercios de nuestra provincia.
        </p>
      </section>
      <section className="story-grid">
        <div className="story-art" aria-hidden="true">
          <Candy size={120} strokeWidth={1} />
          <span>Dulce Mayor</span>
          <small>EL LADO DULCE DE MENDOZA</small>
        </div>
        <div>
          <p className="eyebrow">CERCA DEL MOSTRADOR</p>
          <h2>
            Los grandes vínculos
            <br />
            empiezan en lo cotidiano.
          </h2>
          <p>
            Un kiosco que abre cada mañana. Un almacén de barrio. Un comercio
            que sigue creciendo. Nuestra propuesta está pensada para quienes
            hacen que las cosas pasen todos los días.
          </p>
          <p>
            Conectamos el surtido de golosinas y chocolates con los comercios
            minoristas, a través de la atención de preventores y la distribución
            de pedidos.
          </p>
          <div className="origin-note">
            <MapPin size={20} />
            <span>Mendoza, Argentina</span>
          </div>
          <Link className="text-link" to="/como-comprar">
            Cómo trabajamos <ArrowRight size={17} />
          </Link>
        </div>
      </section>
    </>
  );
}
