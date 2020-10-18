(ns eutros.starmetallic.compilerhack.clinitfilter)

(when *compile-files*
  (require '[instrumentation.core :refer [INSTRUMENTATION]]
           '[instrumentation.dynamic :refer [attach]])
  (import (java.lang.instrument Instrumentation ClassFileTransformer)
          (org.objectweb.asm ClassReader ClassWriter Opcodes)
          (org.objectweb.asm.tree ClassNode MethodNode MethodInsnNode)))

(when *compile-files*
  (attach)

  (defn transform
    [class func]
    (let [transformer
          (reify ClassFileTransformer
            (transform [_ _ _ cbr _ buf]
              (try
                (when (= class cbr)
                  (let [cn (ClassNode.)]
                    (.accept (ClassReader. buf) cn 0)
                    (func cn)
                    (.toByteArray
                      (doto (ClassWriter. ClassWriter/COMPUTE_FRAMES)
                        (->> (.accept cn))))))
                (catch Throwable t#
                  (.printStackTrace t#)))))]
      (println "Redefining class:" class)
      (doto ^Instrumentation INSTRUMENTATION
        (.addTransformer transformer true)
        (.retransformClasses (into-array [class]))
        (.removeTransformer transformer))
      (println "Redefined class:" class)))

  (defn find-method
    [^ClassNode node name desc]
    (some (fn [^MethodNode node]
            (and (= (.-name node) name)
                 (= (.-desc node) desc)
                 node))
          (.-methods node)))

  (defn find-insn
    [^MethodNode node predicate]
    (some #(and (predicate %)
                %)
          (.-instructions node)))

  (transform Compiler
    (fn [cn]
      (let [method (find-method cn
                                "maybeResolveIn"
                                "(Lclojure/lang/Namespace;Lclojure/lang/Symbol;)Ljava/lang/Object;")
            invokestatic (find-insn method
                                    #(and (instance? MethodInsnNode %)
                                          (let [^MethodInsnNode node %]
                                            (and (= (.getOpcode node) Opcodes/INVOKESTATIC)
                                                 (= (.-owner node) "clojure/lang/RT")
                                                 (= (.-name node) "classForName")))))]
        (set! (.-name ^MethodInsnNode invokestatic)
              "classForNameNonLoading"))))

  (transform (Class/forName "clojure.core$get_proxy_class")
    (fn [cn]
      (let [method (find-method cn "invokeStatic" "(Lclojure/lang/ISeq;)Ljava/lang/Object;")
            invokestatic (find-insn method
                                    #(and (instance? MethodInsnNode %)
                                          (let [^MethodInsnNode node %]
                                            (and (= (.getOpcode node) Opcodes/INVOKESTATIC)
                                                 (= (.-owner node) "clojure/lang/RT")
                                                 (= (.-name node) "loadClassForName")))))]
        (set! (.-name ^MethodInsnNode invokestatic)
              "classForNameNonLoading")))))
