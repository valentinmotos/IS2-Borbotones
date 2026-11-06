/** Ilustración original vectorial; no requiere servicios ni imágenes remotas. */
export default function SweetIllustration() {
  return (
    <svg
      className="sweet-illustration"
      viewBox="0 0 620 590"
      role="img"
      aria-labelledby="sweet-title"
    >
      <title id="sweet-title">
        Una bolsa de Dulce Mayor rodeada de caramelos y chocolates
      </title>
      <defs>
        <linearGradient id="bag" x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#93405a" />
          <stop offset="1" stopColor="#54263d" />
        </linearGradient>
        <linearGradient id="choc" x1="0" x2="1" y2="1">
          <stop stopColor="#754a39" />
          <stop offset="1" stopColor="#49291e" />
        </linearGradient>
      </defs>
      <circle cx="325" cy="290" r="235" fill="#ecd3b9" />
      <circle
        cx="325"
        cy="290"
        r="198"
        fill="none"
        stroke="#fff7ed"
        strokeWidth="1.5"
        strokeDasharray="4 9"
      />
      <ellipse
        cx="330"
        cy="505"
        rx="185"
        ry="23"
        fill="#b78f79"
        opacity=".17"
      />
      <g transform="rotate(-13 140 260)">
        <rect x="75" y="125" width="102" height="210" rx="9" fill="#e4a249" />
        <rect x="82" y="135" width="88" height="190" rx="5" fill="url(#choc)" />
        {[0, 1, 2, 3].map((row) =>
          [0, 1].map((col) => (
            <rect
              key={`${row}-${col}`}
              x={89 + col * 39}
              y={144 + row * 44}
              width="34"
              height="38"
              rx="3"
              fill="#87533c"
              stroke="#543020"
              strokeWidth="3"
            />
          )),
        )}
      </g>
      <g transform="rotate(11 329 300)">
        <path
          d="M207 143 L436 143 L459 470 Q328 511 190 468 Z"
          fill="url(#bag)"
        />
        <path d="M207 143 L222 117 L422 117 L436 143 Z" fill="#a65a6e" />
        <path
          d="M217 148 L213 462"
          stroke="#b27687"
          opacity=".4"
          strokeWidth="3"
        />
        <path
          d="M224 119 L421 119"
          stroke="#f3c7a6"
          strokeWidth="3"
          strokeDasharray="2 5"
        />
        <ellipse cx="325" cy="272" rx="83" ry="88" fill="#fff5e3" />
        <path
          d="M281 229 Q321 205 363 229"
          fill="none"
          stroke="#d88f4f"
          strokeWidth="2"
        />
        <text
          x="325"
          y="264"
          textAnchor="middle"
          fill="#683044"
          fontSize="31"
          fontFamily="Georgia,serif"
          fontStyle="italic"
        >
          Dulce
        </text>
        <text
          x="325"
          y="299"
          textAnchor="middle"
          fill="#683044"
          fontSize="29"
          fontFamily="Georgia,serif"
          fontWeight="bold"
        >
          MAYOR
        </text>
        <text
          x="325"
          y="324"
          textAnchor="middle"
          fill="#8c6d58"
          fontSize="8"
          letterSpacing="2.5"
        >
          PARA COMPARTIR
        </text>
        <path
          d="M260 394 Q323 371 391 394"
          fill="none"
          stroke="#f2b87c"
          strokeWidth="1.5"
        />
        <text
          x="325"
          y="421"
          textAnchor="middle"
          fill="#f5d7b0"
          fontSize="10"
          letterSpacing="2"
        >
          EL LADO DULCE
        </text>
        <text
          x="325"
          y="439"
          textAnchor="middle"
          fill="#f5d7b0"
          fontSize="10"
          letterSpacing="2"
        >
          DE TU NEGOCIO
        </text>
      </g>
      <g transform="rotate(24 484 375)">
        <path
          d="M441 357 L410 338 L414 386 L441 377 M512 357 L543 338 L539 386 L512 377"
          fill="#bd5e6e"
        />
        <rect x="436" y="342" width="82" height="55" rx="24" fill="#e9969c" />
        <path
          d="M465 345 L465 394 M484 345 L484 394"
          stroke="#f9cebb"
          strokeWidth="11"
        />
      </g>
      <g transform="rotate(-25 153 427)">
        <path
          d="M123 415 L98 401 L101 444 L123 433 M179 415 L204 401 L201 444 L179 433"
          fill="#869b68"
        />
        <rect x="118" y="402" width="66" height="44" rx="21" fill="#b6c88e" />
        <path d="M147 405 L147 443" stroke="#edf0bd" strokeWidth="12" />
      </g>
      <g transform="rotate(18 485 168)">
        <circle cx="485" cy="157" r="34" fill="#f7e8cf" />
        <path
          d="M485 157 Q463 135 477 126 Q502 127 504 147 Q505 170 483 180 Q459 176 456 155"
          fill="none"
          stroke="#d37584"
          strokeWidth="9"
          strokeLinecap="round"
        />
        <rect x="481" y="190" width="7" height="71" rx="3" fill="#fff7ed" />
      </g>
      <g fill="#874057">
        <path d="M170 68 L177 84 L194 91 L177 98 L170 115 L163 98 L146 91 L163 84 Z" />
        <path d="M521 453 L526 464 L538 469 L526 474 L521 486 L516 474 L504 469 L516 464 Z" />
      </g>
      <circle cx="98" cy="368" r="6" fill="#c57e4f" />
      <circle cx="450" cy="91" r="5" fill="#8a9d6f" />
    </svg>
  );
}
