(ns eutros.starmetallic.lib.specific-proxy
  (:import [java.lang.reflect Modifier]
           [clojure.asm ClassWriter ClassVisitor Opcodes Type]
           [clojure.lang
            IProxy
            Reflector
            DynamicClassLoader
            IPersistentMap
            PersistentHashMap
            RT]
           [clojure.asm.commons Method GeneratorAdapter])
  (:use [clojure.string :only [join]]
        eutros.starmetallic.lib.type-hints))

(defn- get-super-and-interfaces [bases]
  "Copied"
  (if (.isInterface ^Class (first bases))
    [Object bases]
    [(first bases) (next bases)]))

(defn- check-method
  [^java.lang.reflect.Method method]
  (let [mods (.getModifiers method)]
    (if-not
      (and
       (not
        (or (Modifier/isPublic mods)
            (Modifier/isProtected mods)))
       (Modifier/isStatic mods)
       (Modifier/isFinal mods)
       (= "finalize" (.getName method)))
      method)))

(defn- get-target-method
  [super
   interfaces
   given-name
   params]
  (let [param-classes (into-array (map get-type-hint params))
        name          (str (eval given-name))]
    (or
     (loop [^Class cls super]
       (if cls
         (or
          (try (check-method (.getDeclaredMethod cls name param-classes))
            (catch NoSuchMethodException e nil))
          (some
           #(try (check-method (.getMethod ^Class % name param-classes))
             (catch NoSuchMethodException e nil))
           (.getInterfaces cls))
          (recur (.getSuperclass cls)))
         nil))
     (some
      #(try (check-method (.getMethod ^Class % name param-classes))
        (catch NoSuchMethodException e nil))
      interfaces)
     (throw
       (NoSuchMethodException.
         (str name
              "("
              (join ", "
                    (map #(.getName ^Class %)
                         param-classes))
              ")"))))))

(defmacro sproxy
  [class-and-interfaces
   args
   &
   fs]
  (let [bases                      (map
                                    #(or (eval %)
                                      (throw (Exception. (str "Can't evaluate: " %))))
                                    class-and-interfaces)
        [^Class super interfaces]  (get-super-and-interfaces bases)
        classes                    (cons super interfaces)
        constructor-classes        (map get-type-hint args)
        constructor                (let [ctr
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
         #(get-target-method super interfaces (first %) (second %))
         fs)

        cv                         (ClassWriter. ClassWriter/COMPUTE_MAXS)
        pname                      (proxy-name super interfaces)
        cname                      (.replace pname \. \/)
        ctype                      (Type/getObjectType cname)
        iname                      (fn [^Class c]
                                     (.. Type
                                         (getType c)
                                         (getInternalName)))
        fmap                       "__clojureFnMap"
        totype                     (fn [^Class c] (Type/getType c))
        to-types                   (fn [cs]
                                     (if (pos? (count cs))
                                       (into-array (map totype cs))
                                       (make-array Type 0)))
        super-type                 ^Type (totype super)
        imap-type                  ^Type (totype IPersistentMap)
        ifn-type                   (totype clojure.lang.IFn)
        obj-type                   (totype Object)
        sym-type                   (totype clojure.lang.Symbol)
        rt-type                    (totype clojure.lang.RT)
        ex-type                    (totype java.lang.UnsupportedOperationException)]

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
      (let [rtype    ^Type (totype (.getReturnType meth))
            pclasses (.getParameterTypes meth)
            ptypes   (to-types pclasses)
            m        (Method. (.getName meth)
                              rtype
                              ptypes)
            gen      (GeneratorAdapter. Opcodes/ACC_PUBLIC m nil nil cv)

            sm       (Method. (str "s$" (.getName meth))
                              rtype
                              ptypes)
            sgen     (GeneratorAdapter. Opcodes/ACC_PUBLIC sm nil nil cv)]

        ;; call super
        (doto sgen
              (.visitCode)

              (.loadThis)
              (.loadArgs)

              (.visitMethodInsn Opcodes/INVOKESPECIAL
                                (.getInternalName super-type)
                                (.getName m)
                                (.getDescriptor m))

              (.returnValue)
              (.endMethod))

        (doto gen
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
            (.checkCast (totype clojure.lang.IPersistentCollection))
            (.loadArgs)
            (.invokeInterface (totype clojure.lang.IPersistentCollection)
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

    (let [pclass
          (.defineClass ^DynamicClassLoader (deref clojure.lang.Compiler/LOADER)
                        pname
                        (.toByteArray cv)
                        [super interfaces])]
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
