import { Link, NavLink, Outlet, Route, Routes } from "react-router-dom";
import {
  ArrowRight,
  LayoutDashboard,
  MapPin,
  Receipt,
  Store,
  UserRound,
} from "lucide-react";
import CreditsPage from "./CreditsPage.jsx";
import PublicLayout from "./PublicLayout.jsx";
import HomePage from "../modules/home/pages/HomePage.jsx";
import CatalogoPage from "../modules/catalog/pages/CatalogoPage.jsx";
import DetalleProductoPage from "../modules/catalog/pages/DetalleProductoPage.jsx";
import ChatPresupuestoPage from "../modules/presupuesto/pages/ChatPresupuestoPage.jsx";
import CompanyPage from "../modules/company/pages/CompanyPage.jsx";
import ContactPage from "../modules/company/pages/ContactPage.jsx";
import BuyingPage from "../modules/buying/pages/BuyingPage.jsx";
import RegistroMinoristaPage from "../modules/registro/pages/RegistroMinoristaPage.jsx";
import GoogleAccessPage from "../modules/acceso/GoogleAccessPage.jsx";
import GoogleButton from "../modules/acceso/GoogleButton.jsx";
import PerfilPage from "../modules/cuenta/pages/PerfilPage.jsx";
import ComprasPage from "../modules/compras/pages/ComprasPage.jsx";
import CompraDetallePage from "../modules/compras/pages/CompraDetallePage.jsx";
import SessionProvider from "../../../packages/auth/SessionProvider.jsx";
import {
  ChangePasswordPage,
  LoginPage,
  LogoutButton,
  RequireAccess,
} from "../../../packages/auth/AuthRoutes.jsx";
import { useSession } from "../../../packages/auth/session.js";
import { api, tokenStore } from "../lib/api.js";
import "../modules/cuenta/styles.css";

const homes = { MINORISTA: "/cuenta" };
const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
const otherLogin = {
  href: new URL("/login", adminUrl).href,
  label: "Ingresar al portal del personal",
};

// E2-07: la cuenta del minorista es un layout con menú; cada sección se muestra en el Outlet.
function AccountLayout() {
  const { user, home } = useSession();
  const nombre = user.nombreUsuario || "Minorista";
  return (
    <div className="wrap page account">
      <header className="account-head">
        <span className="account-avatar" aria-hidden="true">
          {nombre.charAt(0).toUpperCase()}
        </span>
        <p className="account-who">
          <span>Cuenta de comercio</span>
          <strong>{nombre}</strong>
        </p>
        <LogoutButton />
      </header>
      <nav className="account-nav" aria-label="Menú de la cuenta">
        <NavLink to={home} end>
          <LayoutDashboard size={17} aria-hidden="true" />
          Resumen
        </NavLink>
        <NavLink to={`${home}/compras`}>
          <Receipt size={17} aria-hidden="true" />
          Mis compras
        </NavLink>
        <NavLink to={`${home}/datos`}>
          <UserRound size={17} aria-hidden="true" />
          Mis datos
        </NavLink>
        <Link to="/catalogo">
          <Store size={17} aria-hidden="true" />
          Ver catálogo
        </Link>
      </nav>
      <div className="account-body">
        <Outlet />
      </div>
    </div>
  );
}

function AccountHome() {
  return (
    <div className="account-home">
      <header className="page-head">
        <div>
          <h1>Mi cuenta</h1>
          <p className="lead">
            Seguí tus pedidos, descargá y pagá tus facturas, y mantené al día la
            dirección donde recibís la mercadería.
          </p>
        </div>
      </header>
      <div className="account-shortcuts">
        <Link className="account-shortcut" to="compras">
          <span className="account-shortcut-icon" aria-hidden="true">
            <Receipt size={22} />
          </span>
          <h2>Mis compras</h2>
          <p>
            Mirá tus pedidos, el estado del envío y tus facturas. Las que estén
            pendientes las pagás desde ahí.
          </p>
          <span className="account-shortcut-cta">
            Ver mis compras <ArrowRight size={16} aria-hidden="true" />
          </span>
        </Link>
        <Link className="account-shortcut" to="datos">
          <span className="account-shortcut-icon" aria-hidden="true">
            <MapPin size={22} />
          </span>
          <h2>Mis datos</h2>
          <p>
            Completá tus datos y marcá en el mapa la dirección de entrega.
            También podés cambiar tu clave.
          </p>
          <span className="account-shortcut-cta">
            Completar mis datos <ArrowRight size={16} aria-hidden="true" />
          </span>
        </Link>
      </div>
      <section
        className="panel panel-tint account-howto"
        aria-labelledby="account-howto-title"
      >
        <h2 id="account-howto-title">¿Cómo hago un pedido?</h2>
        <ol className="account-steps">
          <li>
            <strong>El preventor visita tu comercio</strong>
            <span>Le contás qué necesitás y él carga el pedido.</span>
          </li>
          <li>
            <strong>Lo seguís desde Mis compras</strong>
            <span>
              Ves cuándo se recibe, se factura, sale a reparto y llega.
            </span>
          </li>
          <li>
            <strong>Pagás la factura</strong>
            <span>
              La descargás y, si quedó saldo pendiente, la pagás con «Pagar
              ahora».
            </span>
          </li>
        </ol>
        <p className="small muted">
          Mientras tanto, podés mirar precios en el{" "}
          <Link to="/catalogo">catálogo</Link> o armar un{" "}
          <Link to="/presupuesto">presupuesto</Link>.
        </p>
      </section>
    </div>
  );
}

function NotFound() {
  return (
    <section className="error-page">
      <p className="error-code" aria-hidden="true">
        404
      </p>
      <h1>Página no encontrada</h1>
      <p>Revisá la dirección o volvé al inicio.</p>
      <div className="error-actions">
        <Link to="/">Volver al inicio</Link>
        <Link className="error-secondary" to="/catalogo">
          Ver el catálogo
        </Link>
      </div>
    </section>
  );
}

export default function App() {
  return (
    <SessionProvider api={api} tokenStore={tokenStore} homes={homes}>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<HomePage />} />
          <Route path="catalogo" element={<CatalogoPage />} />
          <Route
            path="catalogo/:productoId"
            element={<DetalleProductoPage />}
          />
          <Route path="presupuesto" element={<ChatPresupuestoPage />} />
          <Route path="empresa" element={<CompanyPage />} />
          <Route path="contacto" element={<ContactPage />} />
          <Route path="como-comprar" element={<BuyingPage />} />
          <Route path="creditos" element={<CreditsPage />} />
          <Route
            path="login"
            element={
              <LoginPage
                otherLogin={otherLogin}
                registrationPath="/registro"
                alternateLogin={<GoogleButton />}
              />
            }
          />
          <Route path="registro" element={<RegistroMinoristaPage />} />
          <Route path="acceso/google" element={<GoogleAccessPage />} />
          <Route path="cambiar-clave" element={<ChangePasswordPage />} />
          <Route element={<RequireAccess roles={["MINORISTA"]} />}>
            <Route path="cuenta" element={<AccountLayout />}>
              <Route index element={<AccountHome />} />
              {/* E4-08: compras y facturas del minorista */}
              <Route path="compras" element={<ComprasPage />} />
              <Route path="compras/:pedidoId" element={<CompraDetallePage />} />
              <Route path="datos" element={<PerfilPage />} />
            </Route>
          </Route>
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </SessionProvider>
  );
}
