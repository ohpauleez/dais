(ns dais.server
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
                 Chain
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
      (do (Chain/kill ctx)
          (.end ^HttpServerResponse (.response req) "Hello World")
          ctx)
      ctx)
    ctx))

(defn about-page
  [^Map ctx]
  (if-let [^HttpServerRequest req (.get ctx VertxContainer/VERTX_REQUEST_KEY)]
    (if (= (.path req) "/about")
      (do (Chain/kill ctx)
          (.end ^HttpServerResponse (.response req) "About!")
          ctx)
      ctx)
    ctx))

(defn not-found-page
  [^Map ctx]
  (Chain/kill ctx)
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
