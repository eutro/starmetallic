(ns eutros.starmetallic.registry.util
  (:require [eutros.starmetallic.lib.events :as evt]
            [eutros.starmetallic.reference :as rf])
  (:import (net.minecraftforge.registries IForgeRegistry IForgeRegistryEntry)
           (net.minecraft.util ResourceLocation)
           (net.minecraftforge.event RegistryEvent$Register)))

(defn register
  [registry thing path]
  (.register ^IForgeRegistry registry
             (doto ^IForgeRegistryEntry thing
               (.setRegistryName (ResourceLocation. rf/MODID path)))))

(defmacro register*
  [bus class-name to-require & things]
  `(evt/listen-generic
     ~bus
     RegistryEvent$Register
     (Class/forName ~class-name
                    false
                    (.getContextClassLoader (Thread/currentThread)))
     (fn [^RegistryEvent$Register e#]
       (require ~to-require)
       (doto (.getRegistry e#)
         ~@(map (partial cons `register)
                (partition 2 things))))))
