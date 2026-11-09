import { useEffect } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { LogoutButton } from "../../../packages/auth/AuthRoutes.jsx";
import { useSession } from "../../../packages/auth/session.js";
import { publicUrl, roleLabels } from "./access.js";
import { sections } from "./sections.jsx";

export default function AdminLayout() {
  const { pathname, hash } = useLocation();
  const { status, user, home, signingOut } = useSession();
  const showMenu =
    status === "ready" && home && !user.debeCambiarClave && !signingOut;

  useEffect(() => {
    const title =
      sections.find(({ to }) => to === pathname)?.label || "Administración";
    document.title = `${title} · Dulce Mayor`;
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [pathname, hash]);

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
      <main
        className={`wrap work-main${showMenu ? " role-workspace" : ""}`}
        id="contenido"
        tabIndex={-1}
      >
        {showMenu && (
          <aside className="role-menu">
            <strong>{roleLabels[user.rol]}</strong>
            <span>{user.nombreUsuario}</span>
            <nav aria-label="Navegación principal">
              {sections
                .filter(({ roles }) => roles.includes(user.rol))
                .map(({ to, label }) => (
                  <NavLink key={to} to={to} end>
                    {label}
                  </NavLink>
                ))}
            </nav>
            <LogoutButton />
          </aside>
        )}
        <div className="role-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
