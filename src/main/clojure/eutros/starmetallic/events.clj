(ns eutros.starmetallic.events
  (:import net.minecraftforge.fml.common.Mod$EventBusSubscriber
           net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
           net.minecraftforge.eventbus.api.SubscribeEvent
           net.minecraftforge.api.distmarker.Dist))

(gen-class
 :name         ^{Mod$EventBusSubscriber {:value [Dist/CLIENT]
                                         :modid "starmetallic"
                                         :bus Mod$EventBusSubscriber$Bus/MOD}}
               eutros.starmetallic.events.StarlightBurstEvents
 :main         false
 :methods
               [^:static [^{SubscribeEvent {}}
                          clientsetup
                          [net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent]
                          void]]
 :impl-ns      eutros.starmetallic.entity.starlight-burst
 :load-impl-ns false
 :prefix       event-)
