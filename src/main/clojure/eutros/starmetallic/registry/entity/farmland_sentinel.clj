(ns eutros.starmetallic.registry.entity.farmland-sentinel
  (:require [eutros.starmetallic.registry.entity.entity-common :as cmn])
  (:import (net.minecraft.entity EntityType$Builder EntityType$IFactory EntityClassification)
           (net.minecraftforge.fml.client.registry RenderingRegistry)
           (hellfirepvp.astralsorcery.client.render.entity RenderEntityEmpty$Factory)
           (net.minecraft.network.datasync DataSerializers EntityDataManager)
           (net.minecraftforge.fml.network NetworkHooks)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)
           (hellfirepvp.astralsorcery.client.lib EffectTemplatesAS)
           (hellfirepvp.astralsorcery.common.util.data Vector3)
           (hellfirepvp.astralsorcery.client.effect.function VFXColorFunction VFXScaleFunction)
           (hellfirepvp.astralsorcery.common.util MiscUtils)
           (java.util Random)))

(gen-class
  :name eutros.starmetallic.registry.entity.FarmlandSentinel
  :extends net.minecraft.entity.Entity
  :constructors {[net.minecraft.entity.EntityType net.minecraft.world.World]
                 [net.minecraft.entity.EntityType net.minecraft.world.World]

                 [net.minecraft.world.World]
                 [net.minecraft.entity.EntityType net.minecraft.world.World]}
  :exposes-methods {tick superTick}
  :init init
  :prefix fs-)

(import (eutros.starmetallic.registry.entity FarmlandSentinel))

(declare farmland-sentinel)

(defn fs-init
  ([type world]
   [[type world]])
  ([world]
   [[farmland-sentinel world]]))

(declare ATTUNED TRAIT)
(def random (Random.))

(defn fs-registerData [^FarmlandSentinel this]
  (doto (.dataManager this)
    (.register ATTUNED "starmetallic:__none")
    (.register TRAIT "starmetallic:__none")))

(defn fs-readAdditional [_this _tag])

(defn fs-writeAdditional [_this _tag])

(defn fs-createSpawnPacket [this]
  (NetworkHooks/getEntitySpawningPacket this))

(defn fs-tick [^FarmlandSentinel this]
  (.superTick this)
  (when (-> this .-world .isRemote)
    (let [pos (Vector3. (.getPositionVector this))
          colors [VFXColorFunction/WHITE
                  (or (some-> (cmn/get-constellation this ATTUNED) .getConstellationColor  VFXColorFunction/constant)
                      VFXColorFunction/WHITE)
                  (or (some-> (cmn/get-constellation this TRAIT) .getConstellationColor VFXColorFunction/constant)
                      VFXColorFunction/WHITE)]]
      (dotimes [i 3]
        (-> (EffectHelper/of EffectTemplatesAS/GENERIC_PARTICLE)
            (.spawn (doto pos (MiscUtils/applyRandomOffset random)))
            (.color (nth colors i))
            (.setScaleMultiplier (-> random .nextGaussian Math/abs inc (/ 10)))
            (.scale VFXScaleFunction/SHRINK))))))

#_`[fs-init
    fs-registerData
    fs-readAdditional
    fs-writeAdditional
    fs-createSpawnPacket
    fs-tick]

(def farmland-sentinel
  (when-not *compile-files*
    (-> (EntityType$Builder/create
          (reify EntityType$IFactory
            (create [_ type world]
              (FarmlandSentinel. type world)))
          EntityClassification/MISC)
        (.size 0 0)
        (.setUpdateInterval 10)
        (.setTrackingRange 64)
        (.setShouldReceiveVelocityUpdates true)
        (.build "farmland_sentinel"))))

(defn create-string-key []
  (EntityDataManager/createKey FarmlandSentinel DataSerializers/STRING))

(def ATTUNED (create-string-key))
(def TRAIT (create-string-key))

(when-not *compile-files*
  (RenderingRegistry/registerEntityRenderingHandler
    farmland-sentinel
    (RenderEntityEmpty$Factory.)))
