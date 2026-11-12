import { googleStart, useGoogleAvailable } from "./google.js";

export default function GoogleButton() {
  return (
    useGoogleAvailable() && (
      <a className="dm-button" href={googleStart}>
        Continuar con Google
      </a>
    )
  );
}
