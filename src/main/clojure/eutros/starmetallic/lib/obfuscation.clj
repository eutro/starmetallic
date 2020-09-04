(ns eutros.starmetallic.lib.obfuscation
  (:import net.minecraftforge.fml.common.ObfuscationReflectionHelper
           cpw.mods.modlauncher.api.INameMappingService$Domain))

(defn mapped-symbol
  [sym ^INameMappingService$Domain domain]
  (symbol (ObfuscationReflectionHelper/remapName
            domain
            (str sym))))

(defn !c
  "Map a class.

  Useless with Forge as classes are the same in production and dev."
  [srg-class]
  (mapped-symbol srg-class
                 INameMappingService$Domain/CLASS))

(defn !f
  "Map a field."
  [srg-field]
  (mapped-symbol srg-field
                 INameMappingService$Domain/FIELD))

(defn !m
  "Map a method."
  [srg-method]
  (mapped-symbol srg-method
                 INameMappingService$Domain/METHOD))

(defmacro !
  "Like the special form '. but mapped."
  [obj form]
  (list '. obj
        (if (list? form)
          (cons (-> (first form)
                    (!m))
                (next form))
          (!f form))))

(defmacro !!
  "Like the macro '.. but using '! instead of '."
  ([x form] `(! ~x ~form))
  ([x form & more] `(!! (! ~x ~form) ~@more)))
