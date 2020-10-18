(ns eutros.starmetallic.registry.entity.entities
  (:require [eutros.starmetallic.registry.util :refer [register*]]))

(when *compile-files*
  (require '(eutros.starmetallic.registry.entity [starlight-burst :as sb]))
  (import (eutros.starmetallic.registry.entity.starlight-burst EntityBurst)))

(defn starlight-burst [] sb/starlight-burst)
(defn ->starlight-burst [le] (EntityBurst. le))

(defn listen [bus]
  (register* bus
             "net.minecraft.entity.EntityType"
             '(eutros.starmetallic.registry.entity starlight-burst)
             (starlight-burst) "starlight_burst"))
