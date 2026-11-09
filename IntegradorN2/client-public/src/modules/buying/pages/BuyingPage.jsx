import { useTasteMoment } from "../../../../../packages/design/useTasteMoment.js";
import wordHighlight, {
  phone,
} from "../../../../../packages/design/taste/word-highlight.mjs";

const steps = [
  {
    title: "Visita al comercio",
    description:
      "El preventor visita el punto de venta y registra los datos del negocio.",
  },
  {
    title: "Armado del pedido",
    description:
      "Elegí los productos y las cantidades por presentación. El preventor registra el pedido.",
  },
  {
    title: "Preparación y entrega",
    description:
      "Almacén verifica el stock. El pedido se factura antes de la entrega.",
  },
];

export default function BuyingPage() {
  const statement = useTasteMoment(wordHighlight, phone);
  return (
    <>
      <section className="shop-editorial hero" data-enter>
        <div className="wrap shop-buying enter-copy">
          <header>
            <h1>Cómo comprar</h1>
            <p>Hacé el pedido con el preventor que visita tu comercio.</p>
          </header>
          <ol className="shop-steps" role="list">
            {steps.map(({ title, description }, index) => (
              <li key={title}>
                <span aria-hidden="true">0{index + 1}</span>
                <div>
                  <h2>{title}</h2>
                  <p>{description}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </section>
      <section
        ref={statement}
        className="shop-statement"
        data-moment="word-highlight"
      >
        <div className="wrap">
          <h2 className="mo-wh__text">
            Elegí el surtido por <strong>bulto</strong> según lo que necesitás
            reponer en tu mostrador.
          </h2>
        </div>
      </section>
    </>
  );
}
