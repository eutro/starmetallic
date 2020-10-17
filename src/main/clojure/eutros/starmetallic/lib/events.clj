(ns eutros.starmetallic.lib.events
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.lib.functions :refer [consumer]])
  (:import (net.minecraftforge.common MinecraftForge)
           (net.minecraftforge.eventbus.api EventPriority IEventBus)))

(defn listen
  ([event-type func]
   (listen MinecraftForge/EVENT_BUS event-type func))
  ([bus event-type func]
   (listen bus event-type EventPriority/NORMAL func))
  ([bus event-type priority func]
   (listen bus event-type priority false func))
  ([bus event-type priority receive-cancelled func]
   (.addListener ^IEventBus bus
                 priority
                 receive-cancelled
                 event-type
                 (consumer [event]
                   (func event)))))

(defn listen-generic
  ([event-type generic-type func]
   (listen-generic MinecraftForge/EVENT_BUS event-type generic-type func))
  ([bus event-type generic-type func]
   (listen-generic bus event-type generic-type EventPriority/NORMAL func))
  ([bus event-type generic-type priority func]
   (listen-generic bus event-type generic-type priority false func))
  ([bus event-type generic-type priority receive-cancelled func]
   (.addGenericListener ^IEventBus bus
                        generic-type
                        priority
                        receive-cancelled
                        event-type
                        (consumer [event]
                          (func event)))))
