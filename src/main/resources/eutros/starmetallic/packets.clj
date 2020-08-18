(ns eutros.starmetallic.packets
  (:import net.minecraftforge.fml.network.NetworkRegistry
           net.minecraftforge.fml.network.simple.SimpleChannel
           (java.util.function Supplier Predicate BiConsumer Function)
           (net.minecraft.util ResourceLocation)
           (net.minecraftforge.fml.network NetworkEvent$Context))
  (:use eutros.starmetallic.Starmetallic))

(def PROTOCOL "1")

(def CHANNEL
  (NetworkRegistry/newSimpleChannel
    (ResourceLocation. MODID "chan")
    (reify Supplier
      (get [_] PROTOCOL))
    (reify Predicate
      (test [_ t] (= t PROTOCOL)))
    (reify Predicate
      (test [_ t] (= t PROTOCOL)))))

(deftype PacketBurst [])

(.registerMessage ^SimpleChannel CHANNEL
                  1 PacketBurst
                  (reify BiConsumer                         ;; encoder
                    (accept [_ _ _] nil))
                  (reify Function                           ;; decoder
                    (apply [_ _] (PacketBurst.)))
                  (reify BiConsumer                         ;; handler
                    (accept [_ _ ctx-supplier]
                      (let [ctx ^NetworkEvent$Context (.get ^Supplier ctx-supplier)]
                        (-> (.getSender ctx)
                            ((ns-resolve 'eutros.starmetallic.item.starmetal-sword
                                         'summon-burst)))))))
