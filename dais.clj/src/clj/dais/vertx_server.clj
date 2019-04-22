(ns dais.vertx-server
  (:import (java.util Map
                      ArrayDeque)
           (java.util.function Function
                               Predicate)
           (io.vertx.core Vertx)
           (io.vertx.core.http HttpServerOptions
                               HttpServerRequest
                               HttpServerResponse)
           (dais VertxContainer
                 Server
                 Engine
                 Context
                 Interceptor)))



(def IdentityFunction (Function/identity))

(defn fn->Function [f]
  (when f
       (reify Function
         (apply [self t] (f t)))))

(defn fn->Predicate [f]
  (when f
       (reify Predicate
         (test [self t] (boolean (f t))))))

(defn interceptor [m]
  (let [{:keys [enter leave error]} m]
    ;; Using the identity function ensures Interceptors behave like Pedestal Interceptors for all stages
    (Interceptor. (or (fn->Function enter) IdentityFunction)
                  (or (fn->Function leave) IdentityFunction)
                  (or (fn->Function error) IdentityFunction))))

;(defn root-page
;  [^Map ctx]
;  (if-let [^HttpServerRequest req (.get ctx VertxContainer/VERTX_REQUEST_KEY)]
;    (if (= (.path req) "/")
;      (.end ^HttpServerResponse (.response req) "Hello World")
;      ctx)
;    ctx))

(defn root-page
  [^Map ctx]
  (if-let [^HttpServerRequest req (.get ctx VertxContainer/VERTX_REQUEST_KEY)]
    (if (= (.path req) "/")
      (do (Engine/kill ctx)
          (.end ^HttpServerResponse (.response req) "Hello World")
          ctx)
      ctx)
    ctx))

(defn about-page
  [^Map ctx]
  (if-let [^HttpServerRequest req (.get ctx VertxContainer/VERTX_REQUEST_KEY)]
    (if (= (.path req) "/about")
      (do (Engine/kill ctx)
          (.end ^HttpServerResponse (.response req) "About!")
          ctx)
      ctx)
    ctx))

(defn not-found-page
  [^Map ctx]
  (Engine/kill ctx)
  (doto ^HttpServerResponse (.response ^HttpServerRequest (.get ctx VertxContainer/VERTX_REQUEST_KEY))
    (.setStatusCode 404)
    (.end "Not Found"))
  ctx)


(def service-map {Server/SERVEROPTS_KEY (doto (HttpServerOptions.)
                                          (.setPort 8080)
                                          (.setHost "localhost"))
                  Context/QUEUE_KEY (ArrayDeque. [(interceptor {:enter root-page})
                                                  (interceptor {:enter about-page})
                                                  (interceptor {:enter not-found-page})])})

(defn run [& args]
  (VertxContainer/deploy service-map))


(comment

  (import 'io.vertx.core.json.JsonObject)
  (import 'io.vertx.core.json.Json)
  (import 'jsonista.jackson.KeywordSerializer)
  (require '[jsonista.core :as jsonista])
  (require '[cheshire.core :as cheshire])

  (def object-kw {:a 1 :b ["hello" "world"] :c {1 true 2 false 3 3}})
  (def object-str {"a" 1 "b" ["hello" "world"] "c" {1 true 2 false 3 3}})

  (def jsonista-mapper
    (jsonista/object-mapper {:encode-key-fn name}))

  (time (jsonista/write-value-as-string object-str))
  (jsonista/write-value-as-string object-kw)

  ; The default ObjectMapper in jsonista already has a KeywordDeserializer
  ;(jsonista/write-value-as-string object-kw jsonista-mapper)

  ;; Let's let the underlying ObjectMapper in Vertx know about Clojure types
  (.registerModule Json/mapper (#'jsonista/clojure-module {}))

  (time (.toString (JsonObject. ^java.util.Map object-str)))
  (time (.encode (JsonObject. ^java.util.Map object-str)))

  ;; Can't do this -- JsonObject assumes its keys are always strings
  ;; Jackson Databind is built against a very narrow set of Serializers,
  ;;  built from JsonObject, JsonArray, java.time.Instant, and Byte Arrays
  (.toString (JsonObject. ^java.util.Map object-kw))
  ;; but we can use the underlying mapper
  (.writeValueAsString Json/mapper object-kw)

  (time (cheshire/generate-string object-str))
  (cheshire/generate-string object-kw)

  (require '[criterium.core :as criterium])

  (criterium/quick-bench (JsonObject. ^java.util.Map object-str)) ;; -- 5.3 ns Construction only
  (criterium/quick-bench (.encode (JsonObject. ^java.util.Map object-str))) ;; 1.1 micros total
  (criterium/quick-bench (.writeValueAsString Json/mapper object-str)) ;; 1.1 micros
  (criterium/quick-bench (jsonista/write-value-as-string object-str)) ;; 1.1 micros
  (criterium/quick-bench (cheshire/generate-string object-str)) ;; 4.3 micros

  (criterium/quick-bench (.writeValueAsString Json/mapper object-kw)) ;; 1.1 micros
  (criterium/quick-bench (jsonista/write-value-as-string object-kw)) ;; 1.1 micros
  (criterium/quick-bench (cheshire/generate-string object-kw)) ;; 4.4 micros
  )
