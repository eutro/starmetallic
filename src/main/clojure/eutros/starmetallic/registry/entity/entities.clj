(ns eutros.starmetallic.registry.entity.entities
  (:require [eutros.starmetallic.registry.util :refer [register*]]))

(when *compile-files*
  (require '(eutros.starmetallic.registry.entity [starlight-burst :as sb])))

(defn starlight-burst [] sb/starlight-burst)

(defn listen [bus]
  (register* bus
             "net.minecraft.entity.EntityType"
             '(eutros.starmetallic.registry.entity starlight-burst)
             (starlight-burst) "starlight_burst"))
