import { ExternalLink } from "lucide-react";
import "./credits.css";

export default function CreditsPage() {
  const photographs = [
    [
      "Alfajores argentinos",
      "Mbulig",
      "Alfajores_argentinos_-_2025.jpg",
      "by-sa/4.0",
      "alfajores-780",
    ],
    [
      "Bombones de chocolate",
      "fimb",
      "Pralines_Chocolate_Tasting_Club.jpg",
      "by/2.0",
      "chocolates",
    ],
    ["Gomitas", "Thomas Rosenau", "Gummy_bears.jpg", "by-sa/2.5", "gomitas"],
    [
      "Caramelos",
      "Evan-Amos",
      "Butterscotch-Candies.jpg",
      "by-sa/3.0",
      "caramelos",
    ],
  ];

  return (
    <div className="page credits-page">
      <div className="wrap">
        <header className="page-head">
          <div>
            <p className="eyebrow">Fotografías</p>
            <h1>Créditos de fotografía</h1>
            <p className="lead">
              Fotografías de referencia de Wikimedia Commons. Se redujeron y
              convirtieron a WebP.
            </p>
          </div>
        </header>
        <ul className="panel credits-list" role="list">
          {photographs.map(([name, author, file, license, preview]) => (
            <li key={file}>
              <img
                src={`/fotos/${preview}.webp`}
                alt=""
                width="96"
                height="72"
                loading="lazy"
                decoding="async"
              />
              <div>
                <a href={`https://commons.wikimedia.org/wiki/File:${file}`}>
                  {name} <ExternalLink size={13} aria-hidden="true" />
                </a>
                <p>
                  Autor: {author}
                  {" · "}
                  <a href={`https://creativecommons.org/licenses/${license}/`}>
                    Licencia Creative Commons{" "}
                    {license.toUpperCase().replace("/", " ")}
                  </a>
                </p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
