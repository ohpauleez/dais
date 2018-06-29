(ns dais.server
  (:require [io.pedestal.interceptor :as ped-interceptor]
            [io.pedestal.interceptor.chain :as ped-chain])
  (:import (java.util.function Function
                               Predicate)
           (java.util.concurrent CompletableFuture)
           (java.util Map
                      List
                      Deque
                      HashMap
                      ArrayList
                      ArrayDeque)
           (dais Interceptor
                 Chain
                 AsyncChain
                 Example
                 Maps)))

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

(defn context [ctx-map]
  (reduce-kv (fn [^Map acc k v]
               (case k
                 :queue (.put acc "dais.queue" (ArrayDeque. v))
                 :stack (.put acc "dais.stack" (ArrayDeque. v))
                 :terminators (.put acc "dais.terminators" (ArrayList. (map fn->Predicate v)))
                 (.put acc (name k) v))
               acc)
             (HashMap.)
             ctx-map))

(comment

  (def basic-context
    (context {:queue [(interceptor {:enter (fn [^Map ctx] (.put ctx "a" 1) ctx)
                                    :leave (fn [^Map ctx] (.put ctx "leave-a" 11) ctx)})
                      (interceptor {:enter (fn [^Map ctx] (.put ctx "b" 2) ctx)})
                      (interceptor {:enter (fn [^Map ctx] (.put ctx "c" 3) ctx)})]
              :terminators [(fn [^Map ctx] (.get ctx "b"))]}))

  ;; We should only see a and b processed
  (time (Chain/execute basic-context)) ;; 0.14 - 0.30 ms
  ;; 0.50 - 1.50 ms (includes construction, like the Java Example)
  (time (Chain/execute (context {:queue [(interceptor {:enter (fn [^Map ctx] (.put ctx "a" 1) ctx)
                                                       :leave (fn [^Map ctx] (.put ctx "leave-a" 11) ctx)})
                                         (interceptor {:enter (fn [^Map ctx] (.put ctx "b" 2) ctx)})
                                         (interceptor {:enter (fn [^Map ctx] (.put ctx "c" 3) ctx)})]
                                 :terminators [(fn [^Map ctx] (.get ctx "b"))]})))

  (def dynamic-context
    (context {:queue [(interceptor {:enter (fn [^Map ctx]
                                             (let [q (.get ctx "dais.queue")]
                                               (.addFirst ^Deque q (interceptor
                                                                     {:enter (fn [^Map ctx]
                                                                               (Maps/put ctx "ZZ" 0))}))
                                               (.put ctx "a" 1)
                                               ctx))
                                    :leave (fn [^Map ctx] (Maps/put ctx "leave-a" 11))})
                      (interceptor {:enter (fn [^Map ctx] (Maps/put ctx "b" 2))})
                      (interceptor {:enter (fn [^Map ctx] (Maps/put ctx "c" 3))})]
              :terminators [(fn [^Map ctx] (.get ctx "ZZ"))]}))

  ;; We should only see A and ZZ processed.
  (Chain/execute dynamic-context)

  (def dynamic-last-context
    (context {:queue [(interceptor {:enter (fn [^Map ctx]
                                             (let [q (.get ctx "dais.queue")]
                                               (.addLast ^Deque q (interceptor
                                                                     {:enter (fn [^Map ctx]
                                                                               (.put ctx "ZZ" 0)
                                                                               ctx)}))
                                               (.put ctx "a" 1)
                                               ctx))
                                    :leave (fn [^Map ctx] (.put ctx "leave-a" 11) ctx)})
                      (interceptor {:enter (fn [^Map ctx] (.put ctx "b" 2) ctx)})
                      (interceptor {:enter (fn [^Map ctx] (.put ctx "c" 3) ctx)})]
              :terminators [(fn [^Map ctx] (.get ctx "ZZ"))]}))

  ;; We should see all interceptors processed.
  (time (Chain/execute dynamic-last-context)) ;; 0.37 ms

  ;; 1.00 - 3.00 ms
  (time (ped-chain/execute {::ped-chain/terminators [(fn [ctx] (:ZZ ctx))]}
                     [{:enter (fn [ctx]
                                (-> ctx
                                    (assoc :a 1)
                                    (update-in [::ped-chain/queue]
                                               conj (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :ZZ 0))}))))
                       :leave (fn [ctx] (assoc ctx :leave-a 11))}
                      {:enter (fn [ctx] (assoc ctx :b 2))}
                      {:enter (fn [ctx] (assoc ctx :c 3))}]))

  ;; 0.02 - 0.22 ms
  ;; All Examples
  (= (time (Example/example))
     (time (Example/example1))
     (time (Example/example2))
     (time (Example/example2S))
     (time (Example/example3))
     (time (Example/example4))
     (time (Example/exampleStatic))
     (time (Example/exampleStatic1)))

  ;; Interceptors allocated inline
  (= (time (Example/example))
     (time (Example/example2))
     (time (Example/example3))
     (time (Example/example4))
     (time (Example/exampleStatic)))

  ;; Static interceptors (and 'example' as a baseline)
  (= (time (Example/example))
     (time (Example/example1))
     (time (Example/example2S))
     (time (Example/exampleStatic1)))
  )

