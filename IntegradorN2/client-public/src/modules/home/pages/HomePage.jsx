import { Link } from "react-router-dom";
import { ArrowUpRight } from "lucide-react";
import { useTasteMoment } from "../../../../../packages/design/useTasteMoment.js";
import photoChapters, {
  phone,
} from "../../../../../packages/design/taste/photo-chapters.mjs";

const range = [
  {
    name: "Chocolates",
    description: "Bombones y golosinas bañadas.",
    image: "chocolates",
    width: 960,
    height: 488,
    alt: "Bombones de chocolate en una caja abierta",
  },
  {
    name: "Gomitas",
    description: "Surtidos para el mostrador.",
    image: "gomitas",
    width: 1600,
    height: 1200,
    alt: "Gomitas de colores con forma de ositos",
  },
  {
    name: "Caramelos",
    description: "Caramelos duros para venta por unidad.",
    image: "caramelos",
    width: 1600,
    height: 1007,
    alt: "Caramelos dorados con envoltorio transparente",
  },
];

export default function HomePage() {
  const chapters = useTasteMoment(photoChapters, phone);
  return (
    <>
      <section className="hero shop-hero" data-enter>
        <picture className="shop-hero-photo">
          <source
            media="(max-width: 700px)"
            srcSet="/fotos/alfajores-780.webp"
          />
          <img
            src="/fotos/alfajores.webp"
            width="1920"
            height="1272"
            fetchPriority="high"
            alt="Alfajores argentinos de chocolate y maicena cortados al medio"
          />
        </picture>
        <div className="wrap shop-hero-grid">
          <div className="enter-copy shop-hero-copy">
            <h1>Golosinas por mayor en Mendoza</h1>
            <p>
              Alfajores, chocolates y golosinas por bulto para kioscos,
              almacenes y comercios minoristas.
            </p>
            <Link
              className="btn btn--primary dm-button dm-button-primary"
              to="/como-comprar"
            >
              Ver cómo comprar <ArrowUpRight size={17} aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>
      <section
        ref={chapters}
        id="surtido"
        className="shop-range"
        data-moment="photo-chapters"
        aria-labelledby="surtido-h"
      >
        <div className="wrap mo-ph">
          <div className="mo-ph__head">
            <h2 id="surtido-h">Surtido por bulto</h2>
          </div>
          <ol className="mo-ph__list">
            {range.map(({ name, description, image, width, height, alt }) => (
              <li className="mo-ph__ch" key={image}>
                <figure className="mo-ph__pic">
                  <img
                    src={`/fotos/${image}.webp`}
                    width={width}
                    height={height}
                    loading="lazy"
                    decoding="async"
                    alt={alt}
                  />
                </figure>
                <h3>{name}</h3>
                <p>{description}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>
    </>
  );
}
