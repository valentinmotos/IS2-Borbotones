export default function CreditsPage() {
  const photographs = [
    [
      "Alfajores argentinos",
      "Mbulig",
      "Alfajores_argentinos_-_2025.jpg",
      "by-sa/4.0",
    ],
    [
      "Bombones de chocolate",
      "fimb",
      "Pralines_Chocolate_Tasting_Club.jpg",
      "by/2.0",
    ],
    ["Gomitas", "Thomas Rosenau", "Gummy_bears.jpg", "by-sa/2.5"],
    ["Caramelos", "Evan-Amos", "Butterscotch-Candies.jpg", "by-sa/3.0"],
  ];

  return (
    <section className="wrap photo-credits">
      <h1>Créditos de fotografía</h1>
      <p>
        Fotografías de referencia de Wikimedia Commons. Se redujeron y
        convirtieron a WebP.
      </p>
      <ul>
        {photographs.map(([name, author, file, license]) => (
          <li key={file}>
            <a href={`https://commons.wikimedia.org/wiki/File:${file}`}>
              {name}
            </a>
            {" · "}
            {author}
            {" · "}
            <a href={`https://creativecommons.org/licenses/${license}/`}>
              Licencia Creative Commons{" "}
              {license.toUpperCase().replace("/", " ")}
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}
