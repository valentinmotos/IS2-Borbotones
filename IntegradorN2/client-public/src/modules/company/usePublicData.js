import { useEffect, useState } from "react";
import { api } from "../../lib/api.js";

export function usePublicData(path) {
  const [result, setResult] = useState({ status: "loading" });

  useEffect(() => {
    const request = new AbortController();
    api.get(path, { signal: request.signal, skipAuth: true }).then(
      ({ data }) => {
        if (!request.signal.aborted) setResult({ status: "ready", data });
      },
      () => {
        if (!request.signal.aborted) setResult({ status: "error" });
      },
    );
    return () => request.abort();
  }, [path]);

  return result;
}
