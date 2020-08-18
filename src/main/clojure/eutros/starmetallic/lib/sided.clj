(ns eutros.starmetallic.lib.sided
  (:import net.minecraftforge.fml.loading.FMLEnvironment))

(defn is-client [] (.isClient FMLEnvironment/dist))

(defmacro when-client
  [& forms]
  (when (is-client)
    `(do ~@forms)))

(defmacro if-client
  ([then] `(if-client ~then nil))
  ([then else]
   (if (is-client) then else)))
