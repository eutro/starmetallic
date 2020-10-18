(ns eutros.starmetallic.registry.block.blocks
  (:require [eutros.starmetallic.registry.util :refer [register*]]))

(when *compile-files*
  (require '(eutros.starmetallic.registry.block [light-source :as ls])))

(defn light-source [] ls/light-source)

(defn listen [bus]
  (register* bus
             "net.minecraft.block.Block"
             '(eutros.starmetallic.registry.block light-source)
             (light-source) "light_source"))
