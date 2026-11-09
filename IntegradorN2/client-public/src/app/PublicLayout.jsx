import { useEffect } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useSession } from "../../../packages/auth/session.js";

const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
const sections = [
  { to: "/", label: "Surtido", title: "Golosinas por mayor" },
  { to: "/empresa", label: "La empresa", title: "Nuestra empresa" },
  { to: "/como-comprar", label: "Ver cómo comprar", title: "Cómo comprar" },
];

export default function PublicLayout() {
  const { pathname, hash } = useLocation();
  const { status } = useSession();

  useEffect(() => {
    const section = sections.find(({ to }) => to === pathname);
    document.title = `${section?.title || (pathname === "/creditos" ? "Créditos de fotografía" : "Página no encontrada")} · Dulce Mayor`;
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [pathname, hash]);

  return (
    <div className="shop">
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <header className="shop-header">
        <div className="wrap shop-bar">
          <Link className="dm-brand" to="/">
            Dulce Mayor<span>.</span>
          </Link>
          <nav aria-label="Navegación principal" className="shop-nav">
            {sections.map(({ to, label }) => (
              <NavLink key={to} to={to} end>
                {label}
              </NavLink>
            ))}
            {status !== "loading" && (
              <NavLink to={status === "guest" ? "/login" : "/cuenta"} end>
                {status === "guest" ? "Ingresar" : "Mi cuenta"}
              </NavLink>
            )}
          </nav>
        </div>
      </header>
      <main id="contenido" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="shop-footer">
        <div className="wrap shop-footer-grid">
          <div>
            <Link className="dm-brand" to="/">
              Dulce Mayor<span>.</span>
            </Link>
            <p>Distribución mayorista en Mendoza.</p>
          </div>
          <nav aria-label="Enlaces del pie">
            <Link to="/empresa">La empresa</Link>
            <Link to="/como-comprar">Ver cómo comprar</Link>
            <a href={adminUrl} target="_blank" rel="noreferrer">
              Administración <ArrowUpRight size={16} aria-hidden="true" />
            </a>
          </nav>
          <Link className="shop-credits" to="/creditos">
            Fotografías de Wikimedia Commons
          </Link>
        </div>
      </footer>
    </div>
  );
}
