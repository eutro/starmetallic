(ns eutros.starmetallic.item.core
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.lib.events :as evt]
            [eutros.starmetallic.reference :as rf])
  (:import (net.minecraftforge.event RegistryEvent$Register)
           (net.minecraftforge.registries IForgeRegistry IForgeRegistryEntry)
           (net.minecraft.util ResourceLocation)))

(when *compile-files*
  (require '(eutros.starmetallic.item [starmetal-axe :as axe]
                                      [starmetal-hoe :as hoe]
                                      [starmetal-pickaxe :as pick]
                                      [starmetal-sword :as sword])))

(defn axe [] axe/starmetal-axe)
(defn hoe [] hoe/starmetal-hoe)
(defn pick [] pick/starmetal-pickaxe)
(defn sword [] sword/starmetal-sword)

(defn register
  [registry item path]
  (.register ^IForgeRegistry registry
             (doto ^IForgeRegistryEntry item
               (.setRegistryName (ResourceLocation. rf/MODID path)))))

(defn listen [bus]
  (evt/listen-generic
    bus
    RegistryEvent$Register
    (Class/forName "net.minecraft.item.Item"
                   false
                   (.getContextClassLoader (Thread/currentThread)))
    (fn [^RegistryEvent$Register event]
      (require '(eutros.starmetallic.item starmetal-axe
                                          starmetal-hoe
                                          starmetal-pickaxe
                                          starmetal-sword))
      (doto (.getRegistry event)
        (register (axe) "starmetal_axe")
        (register (hoe) "starmetal_hoe")
        (register (pick) "starmetal_pickaxe")
        (register (sword) "starmetal_sword")))))
