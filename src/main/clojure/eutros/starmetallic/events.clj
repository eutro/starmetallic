(ns eutros.starmetallic.events
  (:require [eutros.starmetallic.compilerhack.clinitfilter])
  (:import net.minecraftforge.fml.common.Mod$EventBusSubscriber
           net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
           net.minecraftforge.eventbus.api.SubscribeEvent
           net.minecraftforge.api.distmarker.Dist))

(gen-class
  :name ^{Mod$EventBusSubscriber {:value [Dist/CLIENT]
                                  :modid "starmetallic"
                                  :bus   Mod$EventBusSubscriber$Bus/MOD}}
  eutros.starmetallic.events.StarlightBurstEvents
  :main false
  :methods
  [^:static [^{SubscribeEvent {}}
             clientsetup
             [net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent]
             void]]
  :impl-ns eutros.starmetallic.entity.starlight-burst
  :load-impl-ns false
  :prefix event-)

(gen-class
  :name ^{Mod$EventBusSubscriber {:modid "starmetallic"}}
  eutros.starmetallic.events.StarmetalSwordEvents
  :main false
  :methods
  [^:static [^{SubscribeEvent {}}
             leftclickempty
             [net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickEmpty]
             void]]
  :impl-ns eutros.starmetallic.item.starmetal-sword
  :load-impl-ns false
  :prefix event-)
