import HomePage from "../modules/home/pages/HomePage.jsx";
import GuidePage from "../modules/help/pages/GuidePage.jsx";
import DesignPage from "../modules/design/DesignPage.jsx";
import CategoriasPage from "../modules/categorias/pages/CategoriasPage.jsx";
import EmpresaPage from "../modules/empresa/pages/EmpresaPage.jsx";
import ProductosPage from "../modules/productos/pages/ProductosPage.jsx";
import UbicacionPage from "../modules/ubicacion/pages/UbicacionPage.jsx";
import PuntosVentaPage from "../modules/puntos-venta/pages/PuntosVentaPage.jsx";
import StockPage from "../modules/stock/pages/StockPage.jsx";
import UsuariosPage from "../modules/usuarios/pages/UsuariosPage.jsx";
import { homes } from "./access.js";

const gestion = ["JEFE", "ADMINISTRATIVO"];

export const sections = [
  {
    to: homes.JEFE,
    label: "Jefatura",
    roles: ["JEFE"],
    element: <HomePage title="Jefatura" />,
  },
  {
    to: homes.ADMINISTRATIVO,
    label: "Administración",
    roles: ["ADMINISTRATIVO"],
    element: <HomePage title="Administración" />,
  },
  {
    to: homes.PREVENTOR,
    label: "Preventa",
    roles: ["PREVENTOR"],
    element: <HomePage title="Preventa" />,
  },
  {
    to: homes.DISTRIBUIDOR,
    label: "Distribución",
    roles: ["DISTRIBUIDOR"],
    element: <HomePage title="Distribución" />,
  },
  {
    to: "/productos",
    label: "Productos",
    roles: gestion,
    element: <ProductosPage />,
  },
  {
    to: "/stock",
    label: "Stock y almacén",
    roles: gestion,
    element: <StockPage />,
  },
  {
    to: "/categorias",
    label: "Categorías",
    roles: gestion,
    element: <CategoriasPage />,
  },
  {
    to: "/empresa",
    label: "Empresa",
    roles: gestion,
    element: <EmpresaPage />,
  },
  {
    to: "/ubicacion",
    label: "Ubicación",
    roles: gestion,
    element: <UbicacionPage />,
  },
  {
    to: "/puntos-de-venta",
    label: "Puntos de venta",
    roles: ["PREVENTOR", "JEFE", "ADMINISTRATIVO"],
    element: <PuntosVentaPage />,
  },
  {
    to: "/usuarios",
    label: "Usuarios internos",
    roles: ["JEFE"],
    element: <UsuariosPage />,
  },
  {
    to: "/guia",
    label: "Guía de trabajo",
    roles: Object.keys(homes),
    element: <GuidePage />,
  },
  {
    to: "/componentes",
    label: "Sistema de diseño",
    roles: gestion,
    element: <DesignPage />,
  },
];
