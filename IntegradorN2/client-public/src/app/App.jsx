import { useEffect, useState } from "react";
import {
  Link,
  NavLink,
  Outlet,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { ArrowRight, ArrowUpRight, Candy, MapPin, Menu, X } from "lucide-react";
import HomePage from "../modules/home/pages/HomePage.jsx";
import CompanyPage from "../modules/company/pages/CompanyPage.jsx";
import BuyingPage from "../modules/buying/pages/BuyingPage.jsx";

const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
const titles = {
  "/": "Golosinas por mayor",
  "/empresa": "Nuestra empresa",
  "/como-comprar": "Cómo comprar",
};

function Layout() {
  const { pathname } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  useEffect(() => {
    document.title = `${titles[pathname] || "Página no encontrada"} · Dulce Mayor`;
    window.scrollTo(0, 0);
  }, [pathname]);
  useEffect(() => {
    const expire = () => setSessionExpired(true);
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);
  return (
    <>
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <div className="announcement">
        Pequeños momentos dulces. Grandes oportunidades para tu comercio.{" "}
        <span>✳</span>
      </div>
      <header className="site-header">
        <Link
          to="/"
          className="brand"
          aria-label="Dulce Mayor, inicio"
          onClick={() => setMenuOpen(false)}
        >
          <span className="brand-icon">
            <Candy size={27} />
          </span>
          <span>
            Dulce Mayor<small>GOLOSINAS POR MAYOR</small>
          </span>
        </Link>
        <button
          className="icon-button mobile-toggle"
          aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={menuOpen}
          aria-controls="menu-principal"
          onClick={() => setMenuOpen(!menuOpen)}
        >
          {menuOpen ? <X /> : <Menu />}
        </button>
        <nav
          id="menu-principal"
          className={menuOpen ? "is-open" : ""}
          aria-label="Navegación principal"
        >
          <NavLink to="/" end onClick={() => setMenuOpen(false)}>
            Inicio
          </NavLink>
          <NavLink to="/empresa" onClick={() => setMenuOpen(false)}>
            Nuestra empresa
          </NavLink>
          <NavLink to="/como-comprar" onClick={() => setMenuOpen(false)}>
            Cómo comprar
          </NavLink>
        </nav>
        <Link className="header-cta" to="/como-comprar">
          Para tu comercio <ArrowUpRight size={17} />
        </Link>
      </header>
      <main id="contenido" tabIndex={-1}>
        {sessionExpired && (
          <div className="notice" role="alert">
            Tu sesión venció. El acceso a cuentas estará disponible
            próximamente.
            <button
              className="icon-button"
              aria-label="Cerrar aviso"
              onClick={() => setSessionExpired(false)}
            >
              <X size={17} />
            </button>
          </div>
        )}
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="footer-top">
          <Link className="brand" to="/">
            <span className="brand-icon">
              <Candy size={25} />
            </span>
            <span>
              Dulce Mayor<small>EL LADO DULCE DE TU NEGOCIO</small>
            </span>
          </Link>
          <p>
            Lo bueno se comparte.
            <br />
            Lo dulce, también.
          </p>
          <Link className="footer-link" to="/empresa">
            Conocenos <ArrowRight size={18} />
          </Link>
        </div>
        <div className="footer-bottom">
          <span>
            <MapPin size={14} /> Mendoza, Argentina
          </span>
          <span>© {new Date().getFullYear()} Dulce Mayor</span>
          <a href={adminUrl} target="_blank" rel="noreferrer">
            Acceso administrativo <ArrowUpRight size={14} />
          </a>
        </div>
      </footer>
    </>
  );
}

function NotFound() {
  return (
    <section className="error-page">
      <p className="eyebrow">ERROR 404</p>
      <h1>Nos fuimos por otro camino.</h1>
      <p>La página que buscás no existe. Volvamos al lado dulce.</p>
      <Link className="button" to="/">
        Volver al inicio <ArrowRight size={17} />
      </Link>
    </section>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="empresa" element={<CompanyPage />} />
        <Route path="como-comprar" element={<BuyingPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
