(ns eutros.starmetallic.packets
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.lib.functions :refer [biconsumer function supplier predicate]]
            [eutros.starmetallic.reference :as rf])
  (:import net.minecraftforge.fml.network.NetworkRegistry
           net.minecraftforge.fml.network.simple.SimpleChannel
           (java.util.function Supplier)
           (net.minecraft.util ResourceLocation)
           (net.minecraftforge.fml.network NetworkEvent$Context)))

(def PROTOCOL "1")

(def CHANNEL
  (when-not *compile-files*
    (NetworkRegistry/newSimpleChannel
      (ResourceLocation. rf/MODID "chan")
      (supplier [] PROTOCOL)
      (predicate [t] (= t PROTOCOL))
      (predicate [t] (= t PROTOCOL)))))

(deftype PacketBurst [])

(when-not *compile-files*
  (.registerMessage
    ^SimpleChannel CHANNEL
    1 PacketBurst

    ;; encoder
    (biconsumer [_ _] nil)

    ;; decoder
    (function [_] (PacketBurst.))

    ;; handler
    (biconsumer
      [_ ctx-supplier]
      (let [ctx ^NetworkEvent$Context (.get ^Supplier ctx-supplier)]
        (-> (.getSender ctx)
            ((ns-resolve 'eutros.starmetallic.item.starmetal-sword
                         'summon-burst)))))))
