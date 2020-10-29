(ns eutros.starmetallic.registry.entity.entities
  (:require [eutros.starmetallic.registry.entity.entity-common :as cmn])
  (:require [eutros.starmetallic.registry.util :refer [register*]]))

(when *compile-files*
  (require '(eutros.starmetallic.registry.entity [starlight-burst :as sb]
                                                 [farmland-sentinel :as fs]))
  (import (eutros.starmetallic.registry.entity EntityBurst FarmlandSentinel)))

(defn starlight-burst [] sb/starlight-burst)
(defn ->starlight-burst [le] (EntityBurst. le))
(defn farmland-sentinel [] fs/farmland-sentinel)
(defn ->farmland-sentinel [world attuned trait]
  (doto (FarmlandSentinel. world)
    (cmn/set-constellation attuned fs/ATTUNED)
    (cmn/set-constellation trait fs/TRAIT)))

(defn listen [bus]
  (register* bus
             "net.minecraft.entity.EntityType"
             '(eutros.starmetallic.registry.entity starlight-burst
                                                   farmland-sentinel)
             (starlight-burst) "starlight_burst"
             (farmland-sentinel) "farmland_sentinel"))
