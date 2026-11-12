import { useEffect, useState } from "react";
import { SessionContext } from "./session.js";

export default function SessionProvider({ api, tokenStore, homes, children }) {
  const [session, setSession] = useState(() =>
    tokenStore.get() ? { status: "loading" } : { status: "guest" },
  );
  const [attempt, setAttempt] = useState(0);
  const [signingOut, setSigningOut] = useState(false);

  useEffect(() => {
    const expire = () =>
      setSession({
        status: "guest",
        notice: "Tu sesión venció. Volvé a ingresar.",
      });
    window.addEventListener("session-expired", expire);
    return () => window.removeEventListener("session-expired", expire);
  }, []);

  useEffect(() => {
    if (!tokenStore.get()) return;
    const controller = new AbortController();
    api.get("/auth/yo", { signal: controller.signal }).then(
      ({ data }) => setSession({ status: "ready", user: data }),
      (error) => {
        if (!controller.signal.aborted && error.status !== 401) {
          setSession({ status: "error", notice: error.message });
        }
      },
    );
    return () => controller.abort();
  }, [api, tokenStore, attempt]);

  function save({ token, rol, nombreUsuario, debeCambiarClave }) {
    tokenStore.set(token);
    setSession({
      status: "ready",
      user: { rol, nombreUsuario, debeCambiarClave },
    });
  }

  async function login(credentials) {
    const { data } = await api.post("/auth/login", credentials, {
      skipAuth: true,
    });
    return acceptLogin(data);
  }

  function acceptLogin(data) {
    if (!Object.hasOwn(homes, data.rol)) return false;
    save(data);
    return true;
  }

  async function changePassword(passwords) {
    const { data } = await api.put("/auth/clave", passwords);
    save(data);
  }

  async function logout() {
    setSigningOut(true);
    let notice;
    try {
      await api.post("/auth/logout");
    } catch (error) {
      if (error.status !== 401) {
        notice =
          "Cerraste la sesión en este navegador. No pudimos revocarla en el servidor.";
      }
    } finally {
      tokenStore.clear();
      setSession({ status: "guest", notice, signedOut: true });
      setSigningOut(false);
    }
  }

  function retry() {
    setSession({ status: "loading" });
    setAttempt((current) => current + 1);
  }

  const home = Object.hasOwn(homes, session.user?.rol)
    ? homes[session.user.rol]
    : null;
  return (
    <SessionContext.Provider
      value={{
        ...session,
        home,
        login,
        acceptLogin,
        logout,
        signingOut,
        changePassword,
        retry,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}
