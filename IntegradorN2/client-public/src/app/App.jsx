import CreditsPage from "./CreditsPage.jsx";
import { Link, Route, Routes } from "react-router-dom";
import PublicLayout from "./PublicLayout.jsx";
import HomePage from "../modules/home/pages/HomePage.jsx";
import CompanyPage from "../modules/company/pages/CompanyPage.jsx";
import BuyingPage from "../modules/buying/pages/BuyingPage.jsx";

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
      <Route element={<PublicLayout />}>
        <Route index element={<HomePage />} />
        <Route path="empresa" element={<CompanyPage />} />
        <Route path="como-comprar" element={<BuyingPage />} />
        <Route path="creditos" element={<CreditsPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
