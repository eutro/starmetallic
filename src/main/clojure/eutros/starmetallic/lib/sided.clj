(ns eutros.starmetallic.lib.sided
  (:import net.minecraftforge.fml.loading.FMLEnvironment))

(defmacro when-client
  [& forms]
  (when (.isClient FMLEnvironment/dist)
    `(do ~@forms)))
