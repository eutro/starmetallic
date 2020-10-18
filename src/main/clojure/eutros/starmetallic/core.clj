(ns eutros.starmetallic.core
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.registry.item.items :as items]
            [eutros.starmetallic.registry.block.blocks :as blocks]
            [eutros.starmetallic.registry.entity.entities :as entities]
            [eutros.starmetallic.packets]
            [eutros.starmetallic.recipe.attune-tool])
  (:import (net.minecraftforge.fml.common Mod)
           (net.minecraftforge.fml.javafmlmod FMLJavaModLoadingContext)))

(gen-class
  :name ^{Mod "starmetallic"}
  eutros.starmetallic.Starmetallic
  :main false
  :post-init "post-init")

(defn -post-init [_]
  (doto (-> (FMLJavaModLoadingContext/get)
            (.getModEventBus))
    (items/listen)
    (blocks/listen)
    (entities/listen)))

#_`-post-init
