import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import {
  ArrowUpRight,
  BarChart3,
  BookOpen,
  Boxes,
  Building2,
  ClipboardList,
  FileBarChart,
  Home,
  MapPin,
  Menu,
  Package,
  Palette,
  Receipt,
  Route,
  Store,
  Tags,
  Truck,
  Users,
  X,
} from "lucide-react";
import { LogoutButton } from "../../../packages/auth/AuthRoutes.jsx";
import { useSession } from "../../../packages/auth/session.js";
import { publicUrl, roleLabels } from "./access.js";
import { sections } from "./sections.jsx";

// Rediseño: agrupación e íconos del menú (solo presentación; los permisos siguen en sections.jsx).
const presentacion = {
  "/jefatura": { group: "General", icon: Home },
  "/administracion": { group: "General", icon: Home },
  "/estadisticas": { group: "General", icon: BarChart3 },
  "/preventa": { group: "Mi trabajo", icon: ClipboardList },
  "/distribucion": { group: "Mi trabajo", icon: Truck },
  "/productos": { group: "Catálogo", icon: Package },
  "/stock": { group: "Catálogo", icon: Boxes },
  "/categorias": { group: "Catálogo", icon: Tags },
  "/almacen": { group: "Pedidos", icon: Receipt },
  "/reportes": { group: "Pedidos", icon: FileBarChart },
  "/puntos-de-venta": { group: "Pedidos", icon: Store },
  "/empresa": { group: "Configuración", icon: Building2 },
  "/ubicacion": { group: "Configuración", icon: MapPin },
  "/usuarios": { group: "Configuración", icon: Users },
  "/guia": { group: "Ayuda", icon: BookOpen },
  "/componentes": { group: "Ayuda", icon: Palette },
};
const groupOrder = [
  "General",
  "Mi trabajo",
  "Pedidos",
  "Catálogo",
  "Configuración",
  "Ayuda",
];

function groupSections(rol) {
  const visible = sections.filter(({ roles }) => roles.includes(rol));
  return groupOrder
    .map((group) => ({
      group,
      items: visible.filter(
        ({ to }) => (presentacion[to]?.group ?? "General") === group,
      ),
    }))
    .filter(({ items }) => items.length);
}

export default function AdminLayout() {
  const { pathname, hash } = useLocation();
  const { status, user, home, signingOut } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);
  const showMenu =
    status === "ready" && home && !user.debeCambiarClave && !signingOut;
  const current = sections.find(({ to }) => to === pathname);

  useEffect(() => {
    const title = current?.label || "Administración";
    document.title = `${title} · Dulce Mayor`;
    if (hash) document.getElementById(hash.slice(1))?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [pathname, hash, current]);

  return (
    <div className="workbench" data-menu={showMenu ? "on" : "off"}>
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      {showMenu && (
        <aside className="role-menu" data-open={menuOpen || undefined}>
          <div className="role-menu-top">
            <Link className="dm-brand" to="/">
              Dulce <span>Mayor</span>
            </Link>
            <button
              className="work-icon-button role-menu-close"
              type="button"
              onClick={() => setMenuOpen(false)}
            >
              <X size={20} aria-hidden="true" />
              <span className="visually-hidden">Cerrar menú</span>
            </button>
          </div>
          <div className="role-menu-user">
            <span className="role-menu-avatar" aria-hidden="true">
              {(user.nombreUsuario || roleLabels[user.rol] || "?")
                .charAt(0)
                .toUpperCase()}
            </span>
            <span className="role-menu-who">
              <strong>{roleLabels[user.rol]}</strong>
              <span>{user.nombreUsuario}</span>
            </span>
          </div>
          <nav aria-label="Navegación principal">
            {groupSections(user.rol).map(({ group, items }) => (
              <div className="role-menu-group" key={group}>
                <p>{group}</p>
                {items.map(({ to, label }) => {
                  const Icon = presentacion[to]?.icon ?? Route;
                  return (
                    <NavLink
                      key={to}
                      to={to}
                      end
                      onClick={() => setMenuOpen(false)}
                    >
                      <Icon size={18} aria-hidden="true" />
                      {label}
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </nav>
          <div className="role-menu-bottom">
            <a href={publicUrl} target="_blank" rel="noreferrer">
              Ver el sitio público <ArrowUpRight size={15} aria-hidden="true" />
            </a>
            <LogoutButton />
          </div>
        </aside>
      )}
      {showMenu && menuOpen && (
        <div
          className="role-menu-backdrop"
          aria-hidden="true"
          onClick={() => setMenuOpen(false)}
        />
      )}
      <div className="work-area">
        <header className="work-header">
          <div className="work-bar">
            {showMenu ? (
              <button
                className="work-icon-button work-menu-toggle"
                type="button"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen(true)}
              >
                <Menu size={20} aria-hidden="true" />
                <span className="visually-hidden">Abrir menú</span>
              </button>
            ) : (
              <Link className="dm-brand" to="/">
                Dulce <span>Mayor</span>
              </Link>
            )}
            {showMenu && (
              <p className="work-crumb">
                <span>Panel de gestión</span>
                {current && <strong>{current.label}</strong>}
              </p>
            )}
            <div className="work-bar-end">
              {showMenu && (
                <span className="work-role">{roleLabels[user.rol]}</span>
              )}
              {!showMenu && (
                <a
                  className="work-site"
                  href={publicUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Ver el sitio público{" "}
                  <ArrowUpRight size={15} aria-hidden="true" />
                </a>
              )}
            </div>
          </div>
        </header>
        <main className="work-main" id="contenido" tabIndex={-1}>
          <div className="role-content">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
