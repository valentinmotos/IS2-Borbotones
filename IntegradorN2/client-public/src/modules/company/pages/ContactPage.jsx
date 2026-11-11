import { Link } from "react-router-dom";
import Branches from "../components/Branches.jsx";

export default function ContactPage() {
  return (
    <section className="wrap shop-institutional">
      <h1>Contacto</h1>
      <p>
        Acercate a una sucursal para consultas sobre pedidos y compras
        mayoristas.
      </p>
      <div className="shop-contact-branches">
        <h2>Direcciones de atención</h2>
        <Branches />
        <Link to="/empresa#sucursales">
          Ver todas las sucursales en el mapa
        </Link>
      </div>
    </section>
  );
}
