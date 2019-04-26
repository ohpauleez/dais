(ns dais.server
  (:require [io.pedestal.interceptor :as ped-interceptor]
            [io.pedestal.interceptor.chain :as ped-chain]
            [sieppari.core :as sieppari])
  (:import (java.util.function Function
                               Predicate)
           (java.util Map
                      List
                      Deque
                      HashMap
                      ArrayList
                      ArrayDeque)
           (dais Interceptor
                 Engine
                 Example
                 Context
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
                 :queue (.put acc Context/QUEUE_KEY (ArrayDeque. v))
                 :stack (.put acc Context/STACK_KEY (ArrayDeque. v))
                 :terminators (.put acc Context/TERMINATORS_KEY (ArrayList. (map fn->Predicate v)))
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
  (time (Engine/execute basic-context)) ;; 0.14 - 0.30 ms
  (time (.get (Engine/completableExecute basic-context))) ;; 0.14 - 0.30 ms
  ;; 0.50 - 1.50 ms (includes construction, like the Java Example)
  (time (Engine/execute (context {:queue [(interceptor {:enter (fn [^Map ctx] (.put ctx "a" 1) ctx)
                                                       :leave (fn [^Map ctx] (.put ctx "leave-a" 11) ctx)})
                                         (interceptor {:enter (fn [^Map ctx] (.put ctx "b" 2) ctx)})
                                         (interceptor {:enter (fn [^Map ctx] (.put ctx "c" 3) ctx)})]
                                 :terminators [(fn [^Map ctx] (.get ctx "b"))]})))

  (def dynamic-context
    (context {:queue [(interceptor {:enter (fn [^Map ctx]
                                             (let [q (.get ctx Context/QUEUE_KEY)]
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
  (Engine/execute dynamic-context)

  (def dynamic-last-context
    (context {:queue [(interceptor {:enter (fn [^Map ctx]
                                             (let [q (.get ctx Context/QUEUE_KEY)]
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
  (time (Engine/execute dynamic-last-context)) ;; 0.15 - 0.37 ms

  (def ped-inters [(ped-interceptor/interceptor
                     {:enter (fn [ctx]
                               (-> ctx
                                   (assoc :a 1)
                                   (update-in [::ped-chain/queue]
                                              conj (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :ZZ 0))}))))
                      :leave (fn [ctx] (assoc ctx :leave-a 11))})
                   (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :b 2))})
                   (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :c 3))})])
  ;; 1.00 - 3.00 ms
  (time (ped-chain/execute {::ped-chain/terminators [(fn [ctx] (:ZZ ctx))]}
                           [(ped-interceptor/interceptor
                              {:enter (fn [ctx]
                                        (-> ctx
                                            (assoc :a 1)
                                            (update-in [::ped-chain/queue]
                                                       conj (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :ZZ 0))}))))
                               :leave (fn [ctx] (assoc ctx :leave-a 11))})
                            (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :b 2))})
                            (ped-interceptor/interceptor {:enter (fn [ctx] (assoc ctx :c 3))})]))
  (time (ped-chain/execute {::ped-chain/terminators [(fn [ctx] (:ZZ ctx))]}
                           ped-inters))
  ;; Sieppari doesn't seem to work -- keeps returning nil
  ;;   There's also no notion of terminators, so that's not going to work.
  ;(time (sieppari/execute [{:enter (-> ctx
  ;                                     (assoc :a 1)
  ;                                     (update-in [:queue]
  ;                                                conj {:enter (fn [ctx] (assoc ctx :ZZ 0))}))
  ;                             ;:leave (fn [ctx] (assoc ctx :leave-a 11))
  ;                             }
  ;                           ;{:enter (fn [ctx] (assoc ctx :b 2))}
  ;                          ;{:enter (fn [ctx] (assoc ctx :c 3))}
  ;                          ]
  ;                        {:hello "world"}))


  (time (Example/exampleLong))
  (time (Example/exampleLongRandom))

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

  (time (Example/exampleAsync1))
  )

