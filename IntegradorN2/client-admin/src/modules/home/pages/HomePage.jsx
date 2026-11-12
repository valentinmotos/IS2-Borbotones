import { Link } from "react-router-dom";
import {
  ArrowRight,
  BarChart3,
  BookOpen,
  Boxes,
  Building2,
  ClipboardList,
  FileBarChart,
  MapPin,
  Package,
  Receipt,
  Store,
  Tags,
  Truck,
  Users,
  Wallet,
} from "lucide-react";
import { useSession } from "../../../../../packages/auth/session.js";
import "../styles.css";

const gestion = ["JEFE", "ADMINISTRATIVO"];

// Mismas rutas y permisos que app/sections.jsx (solo accesos de gestión).
const grupos = [
  {
    nombre: "Pedidos",
    tareas: [
      {
        to: "/almacen",
        label: "Almacén y facturación",
        icon: Receipt,
        texto:
          "Revisá los pedidos, verificá el stock, facturá y registrá cobros.",
        roles: gestion,
      },
      {
        to: "/reportes",
        label: "Reportes",
        icon: FileBarChart,
        texto: "Consultá clientes, pedidos facturados y montos por período.",
        roles: gestion,
      },
      {
        to: "/puntos-de-venta",
        label: "Puntos de venta",
        icon: Store,
        texto: "Mirá los comercios que registraron los preventores por zona.",
        roles: gestion,
      },
    ],
  },
  {
    nombre: "Catálogo",
    tareas: [
      {
        to: "/productos",
        label: "Productos",
        icon: Package,
        texto: "Cargá productos con su imagen y actualizá sus precios.",
        roles: gestion,
      },
      {
        to: "/stock",
        label: "Stock y almacén",
        icon: Boxes,
        texto: "Registrá ingresos o ajustes y revisá qué falta reponer.",
        roles: gestion,
      },
      {
        to: "/categorias",
        label: "Categorías",
        icon: Tags,
        texto: "Ordená el catálogo en grupos de productos.",
        roles: gestion,
      },
    ],
  },
  {
    nombre: "Configuración",
    tareas: [
      {
        to: "/empresa",
        label: "Empresa",
        icon: Building2,
        texto: "Datos de la empresa, sucursales y formas de pago.",
        roles: gestion,
      },
      {
        to: "/ubicacion",
        label: "Ubicación",
        icon: MapPin,
        texto: "Departamentos y localidades que usan las direcciones.",
        roles: gestion,
      },
      {
        to: "/usuarios",
        label: "Usuarios internos",
        icon: Users,
        texto: "Creá cuentas del equipo, cambiá roles o restablecé claves.",
        roles: ["JEFE"],
      },
    ],
  },
  {
    nombre: "Estadísticas",
    tareas: [
      {
        to: "/estadisticas",
        label: "Estadísticas",
        icon: BarChart3,
        texto: "Seguí la preventa, las ventas y las existencias.",
        roles: ["JEFE"],
      },
    ],
  },
];

const circuito = [
  {
    to: "/guia#comercios",
    titulo: "Preventa",
    texto: "El preventor toma el pedido en el comercio.",
    icon: ClipboardList,
  },
  {
    to: "/guia#recorrido",
    titulo: "Almacén y facturación",
    texto: "Se verifica el stock y se emite la factura.",
    icon: Receipt,
  },
  {
    to: "/guia#recorrido",
    titulo: "Cobro",
    texto: "Se registran los pagos hasta saldar la factura.",
    icon: Wallet,
  },
  {
    to: "/guia#recorrido",
    titulo: "Distribución",
    texto: "El distribuidor reparte y marca cada entrega.",
    icon: Truck,
  },
];

export default function HomePage({ title }) {
  const { user } = useSession();
  const visibles = grupos
    .map((grupo) => ({
      ...grupo,
      tareas: grupo.tareas.filter(({ roles }) => roles.includes(user.rol)),
    }))
    .filter(({ tareas }) => tareas.length);

  return (
    <div className="home-panel">
      <header className="page-head">
        <div>
          <span className="eyebrow">Panel de gestión</span>
          <h1>{title}</h1>
          <p className="lead">
            Elegí una tarea para empezar. Desde acá llegás a los pedidos, el
            catálogo y la configuración de Dulce Mayor.
          </p>
        </div>
        <div className="page-head-actions">
          <Link className="dm-button" to="/guia">
            <BookOpen size={16} aria-hidden="true" />
            Guía de trabajo
          </Link>
        </div>
      </header>

      <div className="home-grupos">
        {visibles.map(({ nombre, tareas }, indice) => (
          <section
            key={nombre}
            className="home-grupo"
            aria-labelledby={`home-grupo-${indice}`}
          >
            <h2 id={`home-grupo-${indice}`}>{nombre}</h2>
            <ul className="home-tiles">
              {tareas.map(({ to, label, icon: Icon, texto }) => (
                <li key={to}>
                  <Link className="home-tile" to={to}>
                    <span className="home-tile-icon" aria-hidden="true">
                      <Icon size={20} />
                    </span>
                    <span className="home-tile-copy">
                      <strong>{label}</strong>
                      <span>{texto}</span>
                    </span>
                    <ArrowRight
                      className="home-tile-arrow"
                      size={18}
                      aria-hidden="true"
                    />
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>

      <section className="panel home-circuito" aria-labelledby="home-circuito">
        <div className="panel-head">
          <h2 id="home-circuito">Circuito del pedido</h2>
          <p>Cada paso enlaza a su explicación en la guía.</p>
        </div>
        <ol className="home-circuito-pasos">
          {circuito.map(({ to, titulo, texto, icon: Icon }, indice) => (
            <li key={titulo}>
              <Link to={to}>
                <span className="home-circuito-num" aria-hidden="true">
                  {indice + 1}
                </span>
                <span className="home-circuito-copy">
                  <strong>
                    <Icon size={16} aria-hidden="true" />
                    {titulo}
                  </strong>
                  <span>{texto}</span>
                </span>
              </Link>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}
