(ns eutros.starmetallic.registry.entity.starlight-burst
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.lib.math :refer [sin-deg cos-deg]]
            [eutros.starmetallic.registry.block.blocks :as blocks])
  (:import (net.minecraft.entity Entity
                                 EntityType$Builder
                                 EntityType$IFactory
                                 EntityClassification
                                 LivingEntity)
           (net.minecraft.util.math RayTraceResult BlockPos$PooledMutable)
           (net.minecraftforge.fml.network NetworkHooks)
           (net.minecraftforge.fml.client.registry RenderingRegistry)
           (hellfirepvp.astralsorcery.client.render.entity RenderEntityEmpty$Factory)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)
           (hellfirepvp.astralsorcery.common.util.data Vector3 Vector3$Quat)
           (hellfirepvp.astralsorcery.client.effect.vfx FXLightbeam)
           (hellfirepvp.astralsorcery.client.effect.function VFXColorFunction)
           (java.util Random)
           (hellfirepvp.astralsorcery.common.util MiscUtils)
           (hellfirepvp.astralsorcery.client.lib EffectTemplatesAS)
           (hellfirepvp.astralsorcery.common.constellation IConstellation ConstellationItem ConstellationRegistry)
           (net.minecraft.network.datasync EntityDataManager DataSerializers)
           (net.minecraft.util ResourceLocation)
           (net.minecraft.block Block)
           (hellfirepvp.astralsorcery.client.effect EntityVisualFX)))

(declare starlight-burst)
(def random (Random.))

(declare ATTUNED)
(declare TRAIT)

(defn get-burst-constellation [^Entity e tag]
  (-> e .-dataManager (.get tag) ResourceLocation. ConstellationRegistry/getConstellation))

(defn set-burst-constellation [^Entity e ^IConstellation const tag]
  (some-> const .getRegistryName str
          (->> (.set (.-dataManager e) tag))))

(gen-class
  :name eutros.starmetallic.registry.entity.starlight-burst.EntityBurst
  :extends net.minecraft.entity.projectile.ThrowableEntity
  :state state
  :constructors {[net.minecraft.entity.EntityType
                  net.minecraft.world.World]
                 [net.minecraft.entity.EntityType
                  net.minecraft.world.World]

                 [net.minecraft.entity.LivingEntity]
                 [net.minecraft.entity.EntityType
                  net.minecraft.entity.LivingEntity
                  net.minecraft.world.World]}
  :exposes-methods {setRotation superSetRotation
                    tick        superTick}
  :init init
  :post-init post-init
  :prefix eb-)

(import eutros.starmetallic.registry.entity.starlight-burst.EntityBurst)

(defn eb-init
  ([et w]
   [[et w]
    {:last-star   (Vector3.),
     :from-living false}])
  ([^LivingEntity le]
   [[starlight-burst le (.getEntityWorld le)]
    {:last-star   (Vector3. (.getPositionVector le)),
     :from-living true}]))

(defn eb-post-init
  ([_ _ _])
  ([^EntityBurst this ^LivingEntity entity]
   (let [yaw (mod (+ (.rotationYaw entity)
                     180)
                  360)
         pitch (mod (- (.rotationPitch entity))
                    360)]
     (.superSetRotation this
                        (float yaw)
                        (float pitch))
     (.setMotion this
                 (* (sin-deg yaw)
                    (cos-deg pitch))
                 (sin-deg pitch)
                 (* -1
                    (cos-deg yaw)
                    (cos-deg pitch))))
   (let [stack (.getHeldItemMainhand entity)
         item (.getItem stack)]
     (when (instance? ConstellationItem item)
       (set-burst-constellation this (.getAttunedConstellation ^ConstellationItem item stack) ATTUNED)
       (set-burst-constellation this (.getTraitConstellation ^ConstellationItem item stack) TRAIT)))))

(defn eb-tick [^EntityBurst this]
  (.superTick this)
  (let [world (.getEntityWorld this)]
    (if (.isRemote world)
      (when (<= (.nextDouble random) 0.8)
        (let [burst-pos (Vector3. (.getPositionVector this))
              ^Vector3
              last-star (:last-star (.state this))]
          (-> (FXLightbeam. (.clone last-star))
              (.setup (.clone (doto last-star
                                (.setX (.getX burst-pos))
                                (.setY (.getY burst-pos))
                                (.setZ (.getZ burst-pos))
                                (MiscUtils/applyRandomOffset random 0.5)))
                      0.1 0.1)
              (.color VFXColorFunction/WHITE)
              (.setAlphaMultiplier 0.8)
              ^EntityVisualFX
              (.setMaxAge (+ (* (.nextGaussian random) 2)
                             10))
              (EffectHelper/refresh EffectTemplatesAS/LIGHTBEAM))

          (-> (EffectHelper/of EffectTemplatesAS/GENERIC_PARTICLE)
              (.spawn last-star)
              (.color (or (when (.nextBoolean random)
                            (some-> (get-burst-constellation this (if (.nextBoolean random)
                                                                    ATTUNED
                                                                    TRAIT))
                                    (.getConstellationColor)
                                    (VFXColorFunction/constant)))
                          VFXColorFunction/WHITE))
              (.setScaleMultiplier 0.25)
              (.setMaxAge (+ (* (.nextGaussian random) 2)
                             10)))))

      (let [pos (BlockPos$PooledMutable/retain this)]
        (if (or (not (.isAirBlock world pos))
                (> (.ticksExisted this)
                   40))
          (do (.remove this)
              (when (:from-living (.state this))
                (let [motion (-> this .getMotion Vector3. .perpendicular .normalize (.multiply 0.05))
                      quat (-> this .getMotion Vector3. .normalize
                               (Vector3$Quat/buildQuatFrom3DVector (/ Math/PI 4)))]
                  (dotimes [_ 8]
                    (.rotateWithMagnitude quat motion)
                    (doto (EntityBurst. starlight-burst world)
                      (.setPosition (.getPosX this)
                                    (.getPosY this)
                                    (.getPosZ this))
                      (-> .state ^Vector3 (:last-star)
                          (.setX (.getPosX this))
                          (.setY (.getPosY this))
                          (.setZ (.getPosZ this)))
                      (.setMotion (.getX motion)
                                  (.getY motion)
                                  (.getZ motion))
                      (set-burst-constellation (get-burst-constellation this ATTUNED) ATTUNED)
                      (set-burst-constellation (get-burst-constellation this TRAIT) TRAIT)
                      (->> (.addEntity world)))))))

          (.setBlockState world
                          (BlockPos$PooledMutable/retain this)
                          (.getDefaultState ^Block (blocks/light-source))))))))

(defn eb-registerData [^EntityBurst this]
  (doto (.dataManager this)
    (.register ATTUNED "starmetallic:__none")
    (.register TRAIT "starmetallic:__none")))

(defn eb-onImpact [^EntityBurst _this ^RayTraceResult _rtr])

(defn eb-createSpawnPacket [^EntityBurst this]
  (NetworkHooks/getEntitySpawningPacket this))

(defn eb-getGravityVelocity [_] 0)

#_`[eb-init
    eb-post-init
    eb-tick
    eb-registerData
    eb-onImpact
    eb-createSpawnPacket
    eb-getGravityVelocity]

(defn create-string-key []
  (EntityDataManager/createKey EntityBurst DataSerializers/STRING))

(def ATTUNED (create-string-key))
(def TRAIT (create-string-key))

(def starlight-burst
  (when-not *compile-files*
    (-> (EntityType$Builder/create
          (reify EntityType$IFactory
            (create [_ type world]
              (EntityBurst. type world)))
          EntityClassification/MISC)
        (.size 0 0)
        (.setUpdateInterval 10)
        (.setTrackingRange 64)
        (.setShouldReceiveVelocityUpdates true)
        (.build "starlight_burst"))))

(when-not *compile-files*
  (RenderingRegistry/registerEntityRenderingHandler
    starlight-burst
    (RenderEntityEmpty$Factory.)))
