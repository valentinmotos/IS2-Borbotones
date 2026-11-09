import { useEffect, useRef } from "react";
import * as motion from "./taste/lib.mjs";

export function useTasteMoment(mount, runsOnPhone) {
  const ref = useRef(null);
  useEffect(() => {
    const host = ref.current;
    if (!host) return;
    const staticMode = runsOnPhone ? motion.reduced : motion.still;
    let active = true;
    let instance;
    queueMicrotask(() => {
      if (active)
        instance = mount(host, {}, { ...motion, reduced: staticMode });
    });
    return () => {
      active = false;
      instance?.destroy?.();
    };
  }, [mount, runsOnPhone]);
  return ref;
}
