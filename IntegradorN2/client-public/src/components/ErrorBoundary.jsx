import { Component } from "react";

export default class ErrorBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    if (this.state.failed)
      return (
        <main className="error-page" role="alert">
          <p className="eyebrow">DULCE MAYOR</p>
          <h1>No pudimos cargar esta página.</h1>
          <p>Volvé al inicio para intentarlo nuevamente.</p>
          <a className="button" href="/">
            Volver al inicio
          </a>
        </main>
      );
    return this.props.children;
  }
}
