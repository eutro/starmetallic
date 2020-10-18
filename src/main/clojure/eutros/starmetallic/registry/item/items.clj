(ns eutros.starmetallic.registry.item.items
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.registry.util :refer [register*]]))

(when *compile-files*
  (require '(eutros.starmetallic.registry.item [starmetal-axe :as axe]
                                               [starmetal-hoe :as hoe]
                                               [starmetal-pickaxe :as pick]
                                               [starmetal-sword :as sword])))

(defn axe [] axe/starmetal-axe)
(defn hoe [] hoe/starmetal-hoe)
(defn pick [] pick/starmetal-pickaxe)
(defn sword [] sword/starmetal-sword)

(defn listen [bus]
  (register* bus
             "net.minecraft.item.Item"
             '(eutros.starmetallic.registry.item starmetal-axe
                                                 starmetal-hoe
                                                 starmetal-pickaxe
                                                 starmetal-sword)
             (axe) "starmetal_axe"
             (hoe) "starmetal_hoe"
             (pick) "starmetal_pickaxe"
             (sword) "starmetal_sword"))
