(ns eutros.starmetallic.lib.functions
  (:import (java.util.function BiConsumer Function
                               Supplier Predicate
                               Consumer BiPredicate)))

(defn emit-reify
  [class method bindings body]
  (list `reify class
        (->> body
             (cons (vec (cons (gensym "this")
                              (map #(with-meta % nil)
                                   bindings))))
             (cons method))))

(defmacro deffunctional [macro-name class method]
  (let [bindings (gensym "bindings")
        body (gensym "body")]
    (list `defmacro macro-name
          [bindings '& body]
          (list `emit-reify class method bindings body))))

(deffunctional biconsumer `BiConsumer 'accept)
(deffunctional bipredicate `BiPredicate 'test)
(deffunctional consumer `Consumer 'accept)
(deffunctional supplier `Supplier 'get)
(deffunctional predicate `Predicate 'test)
(deffunctional function `Function 'apply)
