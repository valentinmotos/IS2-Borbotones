import { useEffect, useState } from "react";
import {
  Link,
  matchPath,
  NavLink,
  Outlet,
  useLocation,
} from "react-router-dom";
import {
  ArrowUpRight,
  MapPin,
  Menu,
  ShoppingBag,
  Truck,
  UserRound,
  Wallet,
  X,
} from "lucide-react";
import { useSession } from "../../../packages/auth/session.js";

const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
// Rediseño: solo cambian las etiquetas del menú; los títulos de pestaña siguen iguales.
const sections = [
  { to: "/", label: "Inicio", title: "Golosinas por mayor" },
  { to: "/catalogo", label: "Catálogo", title: "Catálogo" },
  { to: "/como-comprar", label: "Cómo comprar", title: "Cómo comprar" },
  { to: "/presupuesto", label: "Presupuesto", title: "Presupuesto" },
  { to: "/empresa", label: "La empresa", title: "Nuestra empresa" },
  { to: "/contacto", label: "Contacto", title: "Contacto" },
];
const otrosTitulos = {
  "/creditos": "Créditos de fotografía",
  "/login": "Iniciar sesión",
  "/registro": "Crear cuenta",
  "/acceso/google": "Acceso con Google",
  "/cambiar-clave": "Cambiar clave",
  "/cuenta": "Mi cuenta",
};

export default function PublicLayout() {
  const { pathname, hash } = useLocation();
  const { status } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);

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

  const guest = status === "guest";

  return (
    <div className="shop">
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <div className="shop-topbar">
        <div className="wrap shop-topbar-inner">
          <p>
            <span>
              <MapPin size={14} aria-hidden="true" /> Distribución mayorista en
              Mendoza
            </span>
            <span>
              <Truck size={14} aria-hidden="true" /> Entrega en tu comercio
            </span>
            <span>
              <Wallet size={14} aria-hidden="true" /> Pagos en pesos o dólares
            </span>
          </p>
          <a href={adminUrl} target="_blank" rel="noreferrer">
            Acceso del personal <ArrowUpRight size={14} aria-hidden="true" />
          </a>
        </div>
      </div>
      <header className="shop-header">
        <div className="wrap shop-bar">
          <Link className="dm-brand shop-brand" to="/">
            <span className="shop-brand-text">
              <b>
                Dulce <span>Mayor</span>
              </b>
              <small>Golosinas por mayor</small>
            </span>
          </Link>
          <button
            className="shop-menu-toggle"
            type="button"
            aria-expanded={menuOpen}
            aria-controls="menu-principal"
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? (
              <X size={22} aria-hidden="true" />
            ) : (
              <Menu size={22} aria-hidden="true" />
            )}
            <span className="visually-hidden">
              {menuOpen ? "Cerrar menú" : "Abrir menú"}
            </span>
          </button>
          <nav
            id="menu-principal"
            aria-label="Navegación principal"
            className="shop-nav"
            data-open={menuOpen || undefined}
            onClick={(event) => {
              if (event.target.closest("a")) setMenuOpen(false);
            }}
          >
            <ul>
              {sections.map(({ to, label }) => (
                <li key={to}>
                  <NavLink to={to} end>
                    {label}
                  </NavLink>
                </li>
              ))}
            </ul>
            {status !== "loading" && (
              <NavLink
                className="shop-account-link"
                to={guest ? "/login" : "/cuenta"}
                end
              >
                <UserRound size={17} aria-hidden="true" />
                {guest ? "Ingresar" : "Mi cuenta"}
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
          <div className="shop-footer-about">
            <Link className="dm-brand" to="/">
              Dulce <span>Mayor</span>
            </Link>
            <p>
              Golosinas, alfajores y chocolates por bulto para kioscos,
              almacenes y comercios de Mendoza.
            </p>
            <Link className="shop-footer-cta" to="/catalogo">
              <ShoppingBag size={16} aria-hidden="true" /> Ver el catálogo
            </Link>
          </div>
          <nav aria-label="Comprar">
            <h2>Comprar</h2>
            <Link to="/como-comprar">Cómo comprar</Link>
            <Link to="/catalogo">Catálogo y precios</Link>
            <Link to="/presupuesto">Armar presupuesto</Link>
            <Link to="/registro">Crear cuenta de comercio</Link>
          </nav>
          <nav aria-label="Empresa">
            <h2>Empresa</h2>
            <Link to="/empresa">La empresa</Link>
            <Link to="/empresa#sucursales">Sucursales</Link>
            <Link to="/contacto">Contacto</Link>
          </nav>
          <nav aria-label="Accesos">
            <h2>Accesos</h2>
            <Link to={guest ? "/login" : "/cuenta"}>
              {guest ? "Ingresar a mi cuenta" : "Mi cuenta"}
            </Link>
            <a href={adminUrl} target="_blank" rel="noreferrer">
              Administración <ArrowUpRight size={14} aria-hidden="true" />
            </a>
          </nav>
        </div>
        <div className="wrap shop-footer-bottom">
          <p>© Dulce Mayor · Distribución mayorista en Mendoza.</p>
          <Link className="shop-credits" to="/creditos">
            Fotografías de Wikimedia Commons
          </Link>
        </div>
      </footer>
    </div>
  );
}
