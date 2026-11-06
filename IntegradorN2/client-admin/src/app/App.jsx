import { useEffect, useState } from "react";
import {
  NavLink,
  Link,
  Outlet,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import {
  ArrowUpRight,
  BookOpen,
  Candy,
  ChevronRight,
  House,
  Menu,
  X,
} from "lucide-react";
import HomePage from "../modules/home/pages/HomePage.jsx";
import GuidePage from "../modules/help/pages/GuidePage.jsx";

const publicUrl = import.meta.env.VITE_PUBLIC_URL || "http://localhost:5174";
const titles = { "/": "Inicio", "/guia": "Guía de trabajo" };

function Layout() {
  const { pathname, hash } = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  useEffect(() => {
    document.title = `${titles[pathname] || "Página no encontrada"} · Dulce Mayor Administración`;
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [pathname, hash]);
  useEffect(() => {
    const expire = () => setSessionExpired(true);
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);
  return (
    <div className="admin-shell">
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <aside className={`sidebar ${menuOpen ? "is-open" : ""}`} id="navegacion">
        <Link
          className="brand"
          to="/"
          onClick={() => setMenuOpen(false)}
          aria-label="Dulce Mayor, inicio"
        >
          <span className="brand-icon">
            <Candy size={25} />
          </span>
          <span>
            Dulce Mayor<small>ADMINISTRACIÓN</small>
          </span>
        </Link>
        <p className="nav-label">ESPACIO DE TRABAJO</p>
        <nav aria-label="Navegación principal">
          <NavLink to="/" end onClick={() => setMenuOpen(false)}>
            <House size={19} /> Inicio <ChevronRight size={15} />
          </NavLink>
          <NavLink to="/guia" onClick={() => setMenuOpen(false)}>
            <BookOpen size={19} /> Guía de trabajo <ChevronRight size={15} />
          </NavLink>
        </nav>
        <div className="sidebar-bottom">
          <span className="sidebar-candy">
            <Candy size={32} />
          </span>
          <h3>
            El lado más dulce
            <br />
            de tu negocio.
          </h3>
          <a href={publicUrl} target="_blank" rel="noreferrer">
            Visitar sitio público <ArrowUpRight size={17} />
          </a>
          <p>Mendoza, Argentina</p>
        </div>
      </aside>
      <div className="workspace">
        <header className="workspace-header">
          <div className="breadcrumb">
            <button
              className="icon-button mobile-toggle"
              aria-label={menuOpen ? "Cerrar menú" : "Abrir menú"}
              aria-expanded={menuOpen}
              aria-controls="navegacion"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X /> : <Menu />}
            </button>
            <span>Mi espacio</span>
            <ChevronRight size={14} />
            <strong>{titles[pathname] || "Página no encontrada"}</strong>
          </div>
          <span className="workspace-tag">
            <span /> Dulce Mayor
          </span>
        </header>
        <main id="contenido" tabIndex={-1}>
          {sessionExpired && (
            <div className="notice" role="alert">
              Tu sesión venció. Volvé a ingresar cuando el acceso esté
              disponible.
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
        <footer className="workspace-footer">
          <span>Dulce Mayor · Gestión mayorista</span>
          <span>Hecho para crecer juntos.</span>
        </footer>
      </div>
    </div>
  );
}

function NotFound() {
  return (
    <section className="error-page">
      <p className="eyebrow">ERROR 404</p>
      <h1>Esta página no está en tu recorrido.</h1>
      <p>Revisá la dirección o regresá a tu espacio de trabajo.</p>
      <Link className="button" to="/">
        Volver al inicio
      </Link>
    </section>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="guia" element={<GuidePage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
