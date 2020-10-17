(ns eutros.starmetallic.compilerhack.clinitfilter
  (:import (org.objectweb.asm ClassReader ClassWriter Opcodes)
           (org.apache.commons.io IOUtils)))

(gen-class
  :name eutros.starmetallic.compilerhack.clinitfilter.NoClinitClassLoader
  :extends ClassLoader
  :constructors {[ClassLoader] [ClassLoader]}
  :prefix nccl-
  :exposes-methods {loadClass       superLoadClass
                    resolveClass    superResolveClass
                    findLoadedClass superFindLoadedClass
                    defineClass     superDefineClass})

(gen-class
  :name eutros.starmetallic.compilerhack.clinitfilter.NoClinitClassVisitor
  :extends org.objectweb.asm.ClassVisitor
  :constructors {[int org.objectweb.asm.ClassVisitor] [int org.objectweb.asm.ClassVisitor]}
  :exposes-methods {visitMethod superVisitMethod}
  :exposes {cv {:get getDelegate}}
  :prefix nccv-)

(import (eutros.starmetallic.compilerhack.clinitfilter NoClinitClassLoader
                                                       NoClinitClassVisitor))

(defn nccv-visitMethod
  [^NoClinitClassVisitor this
   access
   name
   descriptor
   signature
   exceptions]
  (when (not= "<clinit>" name)
    (.superVisitMethod this
                       access
                       name
                       descriptor
                       signature
                       exceptions)))

(defn strip-clinit?
  [class-name]
  (re-matches #"(.*\.proxy\$)?net\.minecraft(forge)?\..+" class-name))

(defn strip-clinit [class-bytes]
  (let [cr (ClassReader. ^bytes class-bytes)]
    (if (= "java/lang/Enum" (.getSuperName cr))
      class-bytes
      (. ^ClassWriter
         (. (doto (NoClinitClassVisitor. Opcodes/ASM7
                                         (ClassWriter. cr 0))
              (as-> $ (.accept cr $ 0)))
            (getDelegate))
         (toByteArray)))))

(defn get-class-bytes
  [^ClassLoader this
   ^String name]
  (-> name
      (.replace \. \/)
      (.concat ".class")
      (->> (.getResource this))
      (IOUtils/toByteArray)))

(defn load-without-clinit
  [^NoClinitClassLoader this
   ^String name]
  (println "Loading without clinit:" name)
  (let [class-bytes (strip-clinit (get-class-bytes this name))]
    (.superDefineClass this
                       name
                       class-bytes
                       0
                       (int (alength class-bytes)))))

(defn nccl-loadClass
  ([^NoClinitClassLoader this
    ^String name]
   (.superLoadClass this name))
  ([^NoClinitClassLoader this
    ^String name
    resolve]
   (println "Loading class:" name)
   (or (if (strip-clinit? name)
         (try (let [cls (or (.superFindLoadedClass this name)
                            (load-without-clinit this name))]
                (when resolve
                  (.superResolveClass this cls))
                cls)
              (catch ClassNotFoundException _ nil)
              (catch SecurityException _ nil)))
       (.superLoadClass this name resolve))))

#_[`nccl-loadClass `nccv-visitMethod]

(when *compile-files*
  (let [thread (Thread/currentThread)]
    (.setContextClassLoader thread
                            (new NoClinitClassLoader
                                 (.getContextClassLoader thread)))))
