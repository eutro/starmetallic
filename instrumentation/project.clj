(defproject instrumentation "0.1.0"
  :license "MIT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [net.bytebuddy/byte-buddy-agent "1.10.17"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "Instrumentation-Clj.jar"
  :manifest {Premain-Class           "instrumentation.Premain"
             Agent-Class             "instrumentation.Agent"
             Can-Redefine-Classes    true
             Can-Retransform-Classes true})
