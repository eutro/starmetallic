(ns eutros.starmetallic.lib.specific-proxy
  (:import (java.lang.reflect Modifier)
           (clojure.asm ClassWriter ClassVisitor Opcodes Type)
           (clojure.lang IProxy
                         IPersistentMap
                         RT)
           (clojure.asm.commons Method GeneratorAdapter))
  (:use [clojure.string :only [join]]
        eutros.starmetallic.lib.type-hints
        eutros.starmetallic.lib.class-gen))

(defmacro sproxy
  [class-and-interfaces args & fs]
  (let [[^Class super interfaces]
        (get-super-and-interfaces
         (map
          #(or (eval %)
            (throw (Exception. (str "Can't evaluate: " %))))
          class-and-interfaces))
        classes             (cons super interfaces)
        constructor-classes (map get-type-hint args)
        constructor         (let [ctr
                                  (.getDeclaredConstructor super
                                                           (into-array constructor-classes))]
                              (if (-> (.getModifiers ctr)
                                      (Modifier/isPrivate))
                                (throw
                                  (NoSuchMethodException.
                                    (str (.getName super)
                                         "<init>("
                                         (join ", "
                                               (map #(.getName ^Class %)
                                                    constructor-classes))
                                         ")")))
                                ctr))
        target-methods
        (map
         #(get-target-method
           super
           interfaces
           (-> (first %)
               eval
               str)
           (->> (second %)
                (map get-type-hint)
                into-array))
         fs)

        cv                  (ClassWriter. ClassWriter/COMPUTE_MAXS)
        pname               (proxy-name super interfaces)
        cname               (.replace pname \. \/)
        ctype               (Type/getObjectType cname)
        fmap                "__clojureFnMap"
        super-type          ^Type (to-type super)
        imap-type           ^Type (to-type IPersistentMap)
        ifn-type            (to-type clojure.lang.IFn)
        obj-type            (to-type Object)
        sym-type            (to-type clojure.lang.Symbol)
        rt-type             (to-type clojure.lang.RT)
        ex-type             (to-type java.lang.UnsupportedOperationException)]

    ;; start class definition
    (.visit cv
            Opcodes/V1_5
            (+ Opcodes/ACC_PUBLIC
               Opcodes/ACC_SUPER)
            cname
            nil
            (iname super)
            (into-array
             (map iname
                  (cons IProxy interfaces))))

    ;; add field for fn mappings
    (.visitField cv
                 (+ Opcodes/ACC_PRIVATE
                    Opcodes/ACC_VOLATILE)
                 fmap
                 (.getDescriptor imap-type)
                 nil
                 nil)

    ;; add methods
    (doseq [^java.lang.reflect.Method meth target-methods]
      (let [rtype    ^Type (to-type (.getReturnType meth))
            pclasses (.getParameterTypes meth)
            ptypes   (to-types pclasses)
            m        (Method. (.getName meth)
                              rtype
                              ptypes)

            sm       (Method. (str "s$" (.getName meth))
                              rtype
                              ptypes)]

        ;; super invoker
        (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC sm nil nil cv)
              (.visitCode)

              (.loadThis)
              (.loadArgs)

              (.visitMethodInsn Opcodes/INVOKESPECIAL
                                (.getInternalName super-type)
                                (.getName m)
                                (.getDescriptor m))

              (.returnValue)
              (.endMethod))

        ;; binding invoker
        (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
              (.visitCode)

              ;; load and invoke bindings
              (.loadThis)
              (.getField ctype fmap imap-type)
              (.push (str meth))
              (.invokeStatic rt-type (Method/getMethod "Object get(Object, Object)"))
              (.checkCast ifn-type)
              (.loadThis)

              ;; box args
              (#(dotimes [i (count ptypes)]
                 (.loadArg ^GeneratorAdapter % i)
                 (clojure.lang.Compiler$HostExpr/emitBoxReturn nil % (nth pclasses i))))
              ;; call fn
              (.invokeInterface ifn-type
                                (Method. "invoke"
                                         obj-type
                                         (into-array
                                          (cons obj-type
                                                (replicate (count ptypes)
                                                           obj-type)))))

              (.unbox rtype)
              (#(when
                 (= (.getSort rtype)
                    Type/VOID)
                 (.pop ^GeneratorAdapter %)))

              (.returnValue)
              (.endMethod))))

    ;; add constructor
    (let [m (Method. "<init>"
                     Type/VOID_TYPE
                     (to-types constructor-classes))]
      (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
            (.visitCode)
            ;; call super ctor
            (.loadThis)
            (.dup)
            (.loadArgs)
            (.invokeConstructor super-type m)

            (.returnValue)
            (.endMethod)))

    ;; add IProxy methods
    (let [m (Method/getMethod "void __initClojureFnMappings(clojure.lang.IPersistentMap)")]
      (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
            (.visitCode)
            (.loadThis)
            (.loadArgs)
            (.putField ctype fmap imap-type)

            (.returnValue)
            (.endMethod)))
    (let [m   (Method/getMethod "void __updateClojureFnMappings(clojure.lang.IPersistentMap)")]
      (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
            (.visitCode)
            (.loadThis)
            (.dup)
            (.getField ctype fmap imap-type)
            (.checkCast (to-type clojure.lang.IPersistentCollection))
            (.loadArgs)
            (.invokeInterface (to-type clojure.lang.IPersistentCollection)
                              (Method/getMethod "clojure.lang.IPersistentCollection cons(Object)"))
            (.checkCast imap-type)
            (.putField ctype fmap imap-type)

            (.returnValue)
            (.endMethod)))
    (let [m (Method/getMethod "clojure.lang.IPersistentMap __getClojureFnMappings()")]
      (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)
            (.visitCode)
            (.loadThis)
            (.getField ctype fmap imap-type)
            (.returnValue)
            (.endMethod)))

    (.visitEnd cv)

    (let [pclass (define-class pname (.toByteArray cv) super interfaces)]
      `(let [p# (new ~(symbol pname) ~@args)]
        (init-proxy p#
                    ~(loop [fmap {}
                            fs   (map vector fs target-methods)]
                      (if-not (empty? fs)
                        (let [[[_ & meths] reflected]
                              (first fs)
                              binding-name (str reflected)
                              meths        (if (vector? (first meths))
                                             (list meths)
                                             meths)
                              meths        (map
                                            (fn [[params & body]]
                                              (cons
                                               (apply vector
                                                      (with-meta 'this
                                                        {:tag (symbol pname)})
                                                      (map fn-hint-safe params))
                                               body))
                                            meths)]
                          (recur
                            (assoc fmap
                                   binding-name
                                   (cons 'fn meths))
                            (next fs)))
                        fmap)))
        p#))))
