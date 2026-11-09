import { Link, Route, Routes } from "react-router-dom";
import AdminLayout from "./AdminLayout.jsx";
import HomePage from "../modules/home/pages/HomePage.jsx";
import GuidePage from "../modules/help/pages/GuidePage.jsx";
import DesignPage from "../modules/design/DesignPage.jsx";
import CategoriasPage from "../modules/categorias/pages/CategoriasPage.jsx";
import UbicacionPage from "../modules/ubicacion/pages/UbicacionPage.jsx";

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
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<HomePage />} />
        <Route path="categorias" element={<CategoriasPage />} />
        <Route path="ubicacion" element={<UbicacionPage />} />
        <Route path="guia" element={<GuidePage />} />
        <Route path="componentes" element={<DesignPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
