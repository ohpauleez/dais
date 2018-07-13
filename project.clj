(defproject dais "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :java-source-paths ["java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  ;:jvm-opts ["-D\"clojure.compiler.direct-linking=true\""]
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :resource-paths ["config", "resources"]
  :pedantic? :abort
  :global-vars {*warn-on-reflection* true
                *unchecked-math* :warn-on-boxed
                *assert* true}
  :profiles {:srepl {:jvm-opts ^:replace ["-XX:+UseG1GC"
                                          "-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}"]}
             :dev {:aliases {"crepl" ["trampoline" "run" "-m" "clojure.main/main"]
                             "srepl" ["with-profile" "srepl" "trampoline" "run" "-m" "clojure.main/main"]
                             "run-dev" ["trampoline" "run" "-m" "pedestal-lambda.server/run-dev"]}
                   :resource-paths ["config" "resources" "test/resources"]
                   :dependencies [[org.clojure/clojure "1.9.0"]
                                  [io.pedestal/pedestal.interceptor "0.5.4"]
                                  [io.vertx/vertx-core "3.5.2"]
                                  [criterium "0.4.4"]]
                   :main ^{:skip-aot true} dais.server}
             :uberjar {:jvm-opts ["-D\"clojure.compiler.direct-linking=true\""]
                       ;:aot [dais.server]
                       }})

