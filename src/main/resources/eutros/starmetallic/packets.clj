(ns eutros.starmetallic.packets
  (:import net.minecraftforge.fml.network.NetworkRegistry
           net.minecraft.util.ResourceLocation
           (java.util.function Supplier Predicate BiConsumer Function))
  (:use eutros.starmetallic.Starmetallic))

(def PROTOCOL "1")

(def CHANNEL
  (NetworkRegistry/newSimpleChannel
   (net.minecraft.util.ResourceLocation. MODID "chan")
   (reify
    Supplier
    (get [_] PROTOCOL))
   (reify
    Predicate
    (test [_ t] (= t PROTOCOL)))
   (reify
    Predicate
    (test [_ t] (= t PROTOCOL)))))

(deftype PacketBurst [])

(def ^:dynamic burst-handler nil)

(.registerMessage CHANNEL
                  1
                  PacketBurst
                  (reify ;; encoder
                   BiConsumer
                   (accept [_ msg buffer] nil))
                  (reify ;; decoder
                   Function
                   (apply [_ buffer] (PacketBurst.)))
                  (reify ;; handler
                   BiConsumer
                   (accept [_ msg ctx-supplier]
                           (burst-handler ctx-supplier))))
