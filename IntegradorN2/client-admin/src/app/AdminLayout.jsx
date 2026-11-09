import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { Alert } from "../../../packages/design/components.jsx";

const publicUrl = import.meta.env.VITE_PUBLIC_URL || "http://localhost:5174";
const sections = [
  { to: "/", label: "Inicio" },
  { to: "/categorias", label: "Categorías" },
  { to: "/guia", label: "Guía de trabajo" },
  { to: "/componentes", label: "Sistema de diseño" },
];

export default function AdminLayout() {
  const { pathname, hash } = useLocation();
  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {
    const title =
      sections.find(({ to }) => to === pathname)?.label ||
      "Página no encontrada";
    document.title = `${title} · Dulce Mayor Administración`;
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [pathname, hash]);

  useEffect(() => {
    const expire = () => setSessionExpired(true);
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);

  return (
    <div className="workbench">
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <header className="work-header">
        <div className="wrap work-bar">
          <Link className="dm-brand" to="/">
            Dulce Mayor<span>.</span>
          </Link>
          <nav className="work-nav" aria-label="Navegación principal">
            {sections.map(({ to, label }) => (
              <NavLink key={to} to={to} end>
                {label}
              </NavLink>
            ))}
          </nav>
          <a
            className="work-site"
            href={publicUrl}
            target="_blank"
            rel="noreferrer"
          >
            Ver vidriera <ArrowUpRight size={15} aria-hidden="true" />
          </a>
        </div>
      </header>
      <main className="wrap work-main" id="contenido" tabIndex={-1}>
        {sessionExpired && (
          <Alert tone="warning" onDismiss={() => setSessionExpired(false)}>
            Tu sesión venció.
          </Alert>
        )}
        <Outlet />
      </main>
    </div>
  );
}
