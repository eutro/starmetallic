(ns eutros.starmetallic.registry.entity.entity-common
  (:import (net.minecraft.entity Entity)
           (hellfirepvp.astralsorcery.common.constellation IConstellation ConstellationRegistry)
           (net.minecraft.util ResourceLocation)))

(defn get-constellation
  ^IConstellation [^Entity e tag]
  (-> e .-dataManager (.get tag) ResourceLocation. ConstellationRegistry/getConstellation))

(defn set-constellation [^Entity e ^IConstellation const tag]
  (some-> const .getRegistryName str
          (->> (.set (.-dataManager e) tag))))
