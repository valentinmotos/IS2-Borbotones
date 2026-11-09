import { useEffect, useState } from "react";

export function useLocationOptions(api, path) {
  const [response, setResponse] = useState(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!path) return;
    const request = new AbortController();
    api.get(path, { signal: request.signal }).then(
      ({ data }) => {
        if (!request.signal.aborted) setResponse({ path, attempt, data });
      },
      (error) => {
        if (!request.signal.aborted)
          setResponse({ path, attempt, error: error.message });
      },
    );
    return () => request.abort();
  }, [api, path, attempt]);

  const current =
    response?.path === path && response.attempt === attempt ? response : null;
  return {
    data: current?.data,
    error: current?.error,
    loading: Boolean(path && !current),
    retry: () => setAttempt((value) => value + 1),
  };
}
