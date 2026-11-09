import { Link, NavLink, Route, Routes } from "react-router-dom";
import CreditsPage from "./CreditsPage.jsx";
import PublicLayout from "./PublicLayout.jsx";
import HomePage from "../modules/home/pages/HomePage.jsx";
import CompanyPage from "../modules/company/pages/CompanyPage.jsx";
import BuyingPage from "../modules/buying/pages/BuyingPage.jsx";
import SessionProvider from "../../../packages/auth/SessionProvider.jsx";
import {
  ChangePasswordPage,
  LoginPage,
  LogoutButton,
  RequireAccess,
} from "../../../packages/auth/AuthRoutes.jsx";
import { useSession } from "../../../packages/auth/session.js";
import { api, tokenStore } from "../lib/api.js";

const homes = { MINORISTA: "/cuenta" };
const adminUrl = import.meta.env.VITE_ADMIN_URL || "http://localhost:5173";
const otherLogin = {
  href: new URL("/login", adminUrl).href,
  label: "Ingresar al portal del personal",
};

function AccountPage() {
  const { user, home } = useSession();
  return (
    <section className="wrap shop-notice role-workspace">
      <aside className="role-menu">
        <strong>Minorista</strong>
        <span>{user.nombreUsuario}</span>
        <nav aria-label="Menú de la cuenta">
          <NavLink to={home} end>
            Mi cuenta
          </NavLink>
          <Link to="/">Ver surtido</Link>
        </nav>
        <LogoutButton />
      </aside>
      <div>
        <h1>Mi cuenta</h1>
        <p>Ingresaste al portal minorista de Dulce Mayor.</p>
      </div>
    </section>
  );
}

function NotFound() {
  return (
    <section className="error-page">
      <h1>Página no encontrada</h1>
      <p>Revisá la dirección o volvé al inicio.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}

export default function App() {
  return (
    <SessionProvider api={api} tokenStore={tokenStore} homes={homes}>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<HomePage />} />
          <Route path="empresa" element={<CompanyPage />} />
          <Route path="como-comprar" element={<BuyingPage />} />
          <Route path="creditos" element={<CreditsPage />} />
          <Route path="login" element={<LoginPage otherLogin={otherLogin} />} />
          <Route path="cambiar-clave" element={<ChangePasswordPage />} />
          <Route element={<RequireAccess roles={["MINORISTA"]} />}>
            <Route path="cuenta" element={<AccountPage />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </SessionProvider>
  );
}
