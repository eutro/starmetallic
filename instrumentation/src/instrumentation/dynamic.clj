(ns instrumentation.dynamic
  (:require [instrumentation.core :refer [INSTRUMENTATION]])
  (:import (net.bytebuddy.agent ByteBuddyAgent)
           (instrumentation Agent)
           (java.io File)
           (java.lang.management ManagementFactory)))

(defn attach []
  (println "Attaching Instrumentation agent to VM.")
  (ByteBuddyAgent/attach (-> Agent .getProtectionDomain .getCodeSource .getLocation .toURI File.)
                         ^String
                         (-> (ManagementFactory/getRuntimeMXBean) .getName
                             (as-> $
                                   (subs $ 0 (.indexOf $ (int \@))))))
  (println "Successfully attached instrumentation agent.")
  INSTRUMENTATION)
