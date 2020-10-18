(ns instrumentation.core
  (:import (java.lang.instrument Instrumentation)))

(gen-class
  :name instrumentation.Premain
  :main false
  :methods [^:static
            [premain [String java.lang.instrument.Instrumentation] void]])

(gen-class
  :name instrumentation.Agent
  :main false
  :methods [^:static
            [agentmain [String java.lang.instrument.Instrumentation] void]])

(def ^Instrumentation INSTRUMENTATION (reify Instrumentation))

(defn -premain
  [_agent-args inst]
  (intern 'instrumentation.core 'INSTRUMENTATION inst))

(def -agentmain -premain)

#_`-agentmain
