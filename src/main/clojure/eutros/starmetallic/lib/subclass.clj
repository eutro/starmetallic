(ns eutros.starmetallic.lib.subclass
  (:import (clojure.java.api Clojure)
           (clojure.lang IFn Compiler$HostExpr)
           (clojure.asm ClassWriter Type Opcodes)
           (clojure.asm.commons GeneratorAdapter Method)
           (java.lang.reflect Field Modifier))
  (:use eutros.starmetallic.lib.type-hints
        eutros.starmetallic.lib.class-gen))

(defmacro defclass
  "Define a class, which will be imported and constructible under name.

  name - the name of the class

  options - keys and values to define the class

    :prefix - what prefix to use when looking up functions in the namespace, - by default

    :constructors - a vector of constructors to add to the class

    {

      :super - a vector of classes that the super constructor takes, [] by default

      :args - a vector of classes that this constructor takes, same as :super if not defined

      :pre - invoked before the super constructor with the arguments, should return
             a vector of arguments to the super constructor, arguments are passed directly
             if not defined

      :post - invoked after the super constructor with the arguments and the partially initialized instance

      :signature - the signature of the method

    }

    :super - the super class

    :implements - directly implemented interfaces

    :methods - methods to override, vectors

    {

      :alias - the alias for the method, the fn to invoke

      :signature - the signature of the method

      :name - the name of the method

      :types - the parameter types, a vector of classes

    }

    :exposes-methods - protected methods that will be exposed

    {

      :name - the name of the protected method

      :alias - the name of the public method that invokes it

      :types - the parameter types, a vector of classes

    }

    :exposes-fields - protected fields that will be exposed

    {

      :name - the name of the field

      :get - the getter for the field

      :set - the setter for the field

    }

    :signature - the signature of the class

  "
  [top-name & options]
  (let [opts                (reduce
                             (fn [col [k v]]
                               (assoc col k (eval v)))
                             {}
                             (apply assoc {} options))
        ns-part             (namespace-munge *ns*)
        classname           (str ns-part "." top-name)
        prefix              (str (get opts :prefix "-"))

        constructors        (or (opts :constructors) [{}])

        super               ^Class (get opts :super Object)
        interfaces          (get opts :implements [])
        methods             (get opts :methods [])

        exposed-methods     (get opts :exposes-methods [])
        exposed-fields      (get opts :exposes-fields [])

        super-type          ^Type (to-type super)
        interface-types     (to-types interfaces)

        cname               (.replace classname \. \/)
        ctype               (Type/getObjectType cname)

        rt-type             ^Type (to-type clojure.lang.RT)
        ifn-type            ^Type (to-type IFn)
        obj-type            ^Type (to-type Object)
        clojure-type        ^Type (to-type Clojure)

        arg-types           (fn [n]
                              (if (pos? n)
                                (into-array (replicate n obj-type))
                                (make-array Type 0)))

        nth-method          (Method/getMethod "Object nth(Object,int)")
        var-method          (Method/getMethod "clojure.lang.IFn var(Object,Object)")

        cv                  (ClassWriter. ClassWriter/COMPUTE_MAXS)]

    ;; start class
    (.visit cv
            Opcodes/V1_5
            (+ Opcodes/ACC_PUBLIC
               Opcodes/ACC_SUPER)
            cname
            (opts :signature)
            (.getInternalName super-type)
            (into-array String
                        (map iname
                             interface-types)))

    ;; add constructors
    (doseq [opts constructors]
      (let [super-classes      (opts :super)
            super-constructor  (Method. "<init>"
                                        Type/VOID_TYPE
                                        (to-types super-classes))
            this-classes       (or (opts :args) super-classes)
            this-constructor   (Method. "<init>"
                                        Type/VOID_TYPE
                                        (to-types this-classes))
            pre                (opts :pre)
            post               (opts :post)

            gen                (GeneratorAdapter. Opcodes/ACC_PUBLIC
                                                  this-constructor
                                                  nil
                                                  (make-array Type 0)
                                                  cv)

            local              (.newLocal gen obj-type)]

        (.visitCode gen)

        ;; invoke super constructor
        (if pre
          (do
            (doto gen
                  ;; load pre-init fn
                  (.push (str *ns*))
                  (.push (str prefix pre))

                  (.invokeStatic clojure-type var-method))

            ;; load args
            (dotimes [i (count this-classes)]
              (.loadArg gen i)
              (Compiler$HostExpr/emitBoxReturn nil gen (nth this-classes i)))

            (doto gen
                  ;; invoke fn
                  (.invokeInterface ifn-type
                                    (Method. "invoke"
                                             obj-type
                                             (arg-types (count this-classes))))
                  ;; [& super-args]
                  (.storeLocal local)

                  (.loadThis))

            (dotimes [i (count super-classes)]
              ;; load returned vector onto stack
              (doto gen
                    (.loadLocal local)
                    (.push (int i))
                    (.invokeStatic rt-type nth-method)
                    (#(Compiler$HostExpr/emitUnboxArg nil % (nth super-classes i)))))
            (.invokeConstructor gen super-type super-constructor))

          (if (= this-classes super-classes)
            (doto gen
                  (.loadThis)
                  (.loadArgs)
                  (.invokeConstructor super-type super-constructor))
            (throw (Exception. ":pre not specified, but ctor and super ctor args differ"))))

        (when post
          (doto gen
                ;; load post-init fn
                (.push (str *ns*))
                (.push (str prefix post))

                (.invokeStatic clojure-type var-method)

                (.loadThis))
          ;; unbox args
          (dotimes [i (count this-classes)]
            (.loadArg gen i)
            (Compiler$HostExpr/emitBoxReturn nil gen (nth this-classes i)))

          (doto gen
                ;; invoke fn
                (.invokeInterface ifn-type
                                  (Method. "invoke"
                                           obj-type
                                           (arg-types (+ (count this-classes) 1))))

                ;; clear stack
                (.pop)))

        (doto gen
              (.returnValue)
              (.endMethod))))

    ;; add overrided methods
    (doseq [{name :name fn-name :alias sig :signature pclasses :types} methods]
      (let [reflected ^java.lang.reflect.Method (get-target-method check-overridable super interfaces (str name) (into-array Class pclasses))

            rtype     (Type/getReturnType reflected)
            ptypes    (Type/getArgumentTypes reflected)

            m         (Method. (str name) rtype ptypes)

            gen       (GeneratorAdapter. Opcodes/ACC_PUBLIC
                                         m
                                         sig
                                         (make-array Type 0)
                                         cv)

            sm        (Method. (str "s$" name) rtype ptypes)]

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
        (doto gen
              (.visitCode)

              ;; load method fn
              (.push (str *ns*))
              (.push (str prefix fn-name))

              (.invokeStatic clojure-type var-method)

              (.loadThis))
        ;; load args
        (dotimes [i (count pclasses)]
          (.loadArg gen i)
          (Compiler$HostExpr/emitBoxReturn nil gen (nth pclasses i)))

        ;; invoke fn
        (.invokeInterface gen
                          ifn-type
                          (Method. "invoke"
                                   obj-type
                                   (arg-types (+ (count pclasses) 1))))

        (when
          (= (Type/getReturnType reflected)
             Type/VOID_TYPE)
          (.pop gen))

        (doto gen
              (.unbox rtype)

              (.returnValue)
              (.endMethod))))

    ;; add exposed methods
    (doseq [{name :name alias :alias pclasses :types} exposed-methods]
      (let [reflected ^java.lang.reflect.Method (get-target-method check-visible super interfaces (str name) (into-array Class pclasses))

            rtype     (Type/getReturnType reflected)
            ptypes    (Type/getArgumentTypes reflected)]
        (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC
                                 (Method. (str alias) rtype ptypes)
                                 nil
                                 nil
                                 cv)
              (.visitCode)

              (.loadThis)
              (.loadArgs)

              (.invokeVirtual ctype (Method. (str name) rtype ptypes))

              (.returnValue)
              (.endMethod))))

    ;; add getters and setters
    (doseq [{name :name getter :get setter :set} exposed-fields]
      (let [reflected ^Field (get-target-field super name)
            ftype     (to-type (.getType reflected))
            static?   (Modifier/isStatic (.getModifiers reflected))
            acc       (+ Opcodes/ACC_PUBLIC (if static? Opcodes/ACC_STATIC 0))]
        (when
          (and setter
               (not
                (Modifier/isFinal
                 (.getModifiers reflected))))
          ;; generate setter
          (let [gen (GeneratorAdapter. acc
                                       (Method. (str setter)
                                                Type/VOID_TYPE
                                                (into-array [ftype]))
                                       nil
                                       nil
                                       cv)]
            (.visitCode gen)

            (if static?
              (doto gen
                    (.loadArgs)
                    (.putStatic ctype (str name) ftype))
              (doto gen
                    (.loadThis)
                    (.loadArgs)
                    (.putField ctype (str name) ftype)))

            (doto gen
                  (.returnValue)
                  (.endMethod))))

        (when getter
          (let [gen (GeneratorAdapter. acc
                                       (Method. (str setter)
                                                ftype
                                                (to-types []))
                                       nil
                                       nil
                                       cv)]
            (.visitCode gen)

            (if static?
              (.getStatic gen ctype (str name) ftype)
              (doto gen
                    (.loadThis)
                    (.getField ctype (str name) ftype)))

            (doto gen
                  (.returnValue)
                  (.endMethod))))))

    (define-class classname (.toByteArray cv) super interfaces)

    `(do
      (import
       ~(symbol classname))
      ~top-name)))
