import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../lib/api.js";

export default function usePreventaResource(path, interval = 0) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const request = useRef(null);

  const reload = useCallback(() => {
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    return api.get(path, { signal: controller.signal }).then(
      ({ data }) => {
        if (!controller.signal.aborted) {
          setData(data);
          setError(null);
        }
      },
      (failure) => {
        if (!controller.signal.aborted) setError(failure.message);
      },
    );
  }, [path]);

  useEffect(() => {
    void reload();
    const refreshVisible = () => {
      if (!document.hidden) void reload();
    };
    const timer = interval
      ? window.setInterval(refreshVisible, interval)
      : null;
    if (interval) window.addEventListener("focus", refreshVisible);
    return () => {
      request.current?.abort();
      if (interval) {
        window.clearInterval(timer);
        window.removeEventListener("focus", refreshVisible);
      }
    };
  }, [interval, reload]);

  return { data, error, reload };
}
