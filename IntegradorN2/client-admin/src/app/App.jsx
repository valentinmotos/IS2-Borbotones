import { Link, Navigate, Route, Routes } from "react-router-dom";
import SessionProvider from "../../../packages/auth/SessionProvider.jsx";
import {
  ChangePasswordPage,
  LoginPage,
  RequireAccess,
} from "../../../packages/auth/AuthRoutes.jsx";
import { useSession } from "../../../packages/auth/session.js";
import { api, tokenStore } from "../lib/api.js";
import { homes, publicUrl } from "./access.js";
import { sections } from "./sections.jsx";
import AdminLayout from "./AdminLayout.jsx";

const otherLogin = {
  href: new URL("/login", publicUrl).href,
  label: "Ingresar al portal minorista",
};

function HomeRedirect() {
  const { home } = useSession();
  return <Navigate to={home} replace />;
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

export function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route path="login" element={<LoginPage otherLogin={otherLogin} />} />
        <Route path="cambiar-clave" element={<ChangePasswordPage />} />
        <Route element={<RequireAccess />}>
          <Route index element={<HomeRedirect />} />
          {sections.map(({ to, roles, element }) => (
            <Route key={to} element={<RequireAccess roles={roles} />}>
              <Route path={to} element={element} />
            </Route>
          ))}
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <SessionProvider api={api} tokenStore={tokenStore} homes={homes}>
      <AdminRoutes />
    </SessionProvider>
  );
}
