import { useEffect } from "react";
import { Link, matchPath, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useSession } from "../../../packages/auth/session.js";

const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
const sections = [
  { to: "/", label: "Surtido", title: "Golosinas por mayor" },
  { to: "/catalogo", label: "Catálogo", title: "Catálogo" },
  { to: "/presupuesto", label: "Armar presupuesto", title: "Presupuesto" },
  { to: "/empresa", label: "La empresa", title: "Nuestra empresa" },
  { to: "/como-comprar", label: "Ver cómo comprar", title: "Cómo comprar" },
  { to: "/contacto", label: "Contacto", title: "Contacto" },
];
const otrosTitulos = {
  "/creditos": "Créditos de fotografía",
  "/login": "Iniciar sesión",
  "/registro": "Crear cuenta",
  "/cambiar-clave": "Cambiar clave",
  "/cuenta": "Mi cuenta",
};

export default function PublicLayout() {
  const { pathname, hash } = useLocation();
  const { status } = useSession();

  useEffect(() => {
    const section = sections.find(({ to }) => to === pathname);
    const isProduct = matchPath("/catalogo/:productoId", pathname);
    // E6-01: rutas que no están en el menú (antes /cuenta quedaba como «Página no encontrada»).
    // Las páginas que definen su propio título (compras, perfil, registro…) lo pisan después.
    const title =
      section?.title ??
      (isProduct ? "Detalle de producto" : null) ??
      otrosTitulos[pathname];
    document.title = `${title ?? "Página no encontrada"} · Dulce Mayor`;
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
            <Link to="/contacto">Contacto</Link>
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
