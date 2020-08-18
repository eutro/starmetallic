(ns eutros.starmetallic.lib.class-gen
  (:import (java.lang.reflect Method Modifier Member)
           (clojure.lang DynamicClassLoader)
           clojure.asm.Type)
  (:use [clojure.string :only [join]]))

(defn get-super-and-interfaces [bases]
  "Copied"
  (if (.isInterface ^Class (first bases))
    [Object bases]
    [(first bases) (next bases)]))

(defn to-type [^Class c] (Type/getType c))

(defn to-types [cs]
  (if (pos? (count cs))
    (into-array (map to-type cs))
    (make-array Type 0)))

(defn iname
  [^Class c]
  (.. Type
    (getType c)
    (getInternalName)))

(defn check-overridable
  [^Method method]
  (let [mods (.getModifiers method)]
    (if-not (and (not (or (Modifier/isPublic mods)
                          (Modifier/isProtected mods)))
                 (Modifier/isStatic mods)
                 (Modifier/isFinal mods))
      method)))

(defn check-visible
  [^Member field]
  (let [mods (.getModifiers field)]
    (if (or (Modifier/isPublic mods)
            (Modifier/isProtected mods))
      field)))

(defn get-target-field
  [super name]
  (loop [^Class cls super]
    (if cls
      (or (try (check-visible (.getDeclaredField cls name))
               (catch NoSuchMethodException _ nil))
          (recur (.getSuperclass cls)))
      (throw (NoSuchFieldException. name)))))

(defn get-target-method
  [method-filter super interfaces name param-classes]
  (let [pclasses (into-array Class param-classes)]
    (or (loop [^Class cls super]
          (if cls
            (or (try (method-filter (.getDeclaredMethod cls name pclasses))
                     (catch NoSuchMethodException e nil))
                (some #(try (method-filter (.getMethod ^Class % name pclasses))
                            (catch NoSuchMethodException e nil))
                      (.getInterfaces cls))
                (recur (.getSuperclass cls)))
            nil))
        (some #(try (method-filter (.getMethod ^Class % name pclasses))
                    (catch NoSuchMethodException e nil))
              interfaces)
        (throw (NoSuchMethodException. (str name
                                            "(" (join ", "
                                                      (map #(.getName ^Class %)
                                                           param-classes)) ")"))))))

(defn define-class
  [name bytes super interfaces]
  #_;
          (with-open [fos (java.io.FileOutputStream.
                            (java.io.File.
                              (str \. java.io.File/separatorChar
                                   "generated" java.io.File/separatorChar
                                   name
                                   ".class")))]
            (.write fos bytes)
            (.flush fos))
  (.defineClass ^DynamicClassLoader (deref Compiler/LOADER)
                name
                bytes
                [super interfaces]))
