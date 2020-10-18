(ns eutros.starmetallic.packets
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.lib.functions :refer [supplier predicate]]
            [eutros.starmetallic.reference :as rf])
  (:import net.minecraftforge.fml.network.NetworkRegistry
           (net.minecraft.util ResourceLocation)))

(def PROTOCOL "1")

(def CHANNEL
  (when-not *compile-files*
    (NetworkRegistry/newSimpleChannel
      (ResourceLocation. rf/MODID "chan")
      (supplier [] PROTOCOL)
      (predicate [t] (= t PROTOCOL))
      (predicate [t] (= t PROTOCOL)))))
