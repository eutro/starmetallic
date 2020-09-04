(ns eutros.starmetallic.entity.starlight-burst
  (:import (net.minecraft.entity Entity
                                 EntityType
                                 EntityType$Builder
                                 EntityType$IFactory
                                 EntityClassification
                                 LivingEntity)
           net.minecraft.entity.projectile.ThrowableEntity
           net.minecraft.world.World
           (net.minecraft.util.math RayTraceResult BlockPos$PooledMutable)
           net.minecraftforge.fml.network.NetworkHooks
           (net.minecraftforge.fml.client.registry RenderingRegistry)
           (hellfirepvp.astralsorcery.client.render.entity RenderEntityEmpty$Factory)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)
           (hellfirepvp.astralsorcery.common.util.data Vector3)
           (hellfirepvp.astralsorcery.client.effect.vfx FXLightbeam)
           (hellfirepvp.astralsorcery.client.effect.function VFXColorFunction VFXAlphaFunction)
           (java.util Random)
           (hellfirepvp.astralsorcery.common.util MiscUtils)
           (hellfirepvp.astralsorcery.client.lib EffectTemplatesAS)
           (net.minecraft.network IPacket))
  (:use eutros.clojurelib.lib.class-gen
        eutros.starmetallic.block.light-source
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.lib.sided
        eutros.starmetallic.lib.math
        eutros.starmetallic.Starmetallic))

(defn get-world
  [^Entity entity]
  (! entity field_70170_p                                   ;; world
     ))

(declare starlight-burst)
(def random (Random.))

(defclass
  EntityBurst (:extends ThrowableEntity)

  (:field ^Vector3 lastStar)

  (:constructor [^EntityType type ^World world])

  (:constructor [^LivingEntity entity]
    (super [^EntityType starlight-burst
            ^LivingEntity entity
            ^World (get-world entity)])
    (let [yaw (mod (+ (! entity field_70177_z               ;; rotationYaw
                         )
                      180)
                   360)
          pitch (mod (- (! entity field_70125_A             ;; rotationPitch
                           ))
                     360)
          vel 0.5]
      (call-protected #obf/obf ^{:obf/srg func_70101_b} setRotation
                      ^float (float yaw)
                      ^float (float pitch))
      (! this (func_213293_j                                ;; setMotion
                (* (sin-deg yaw)
                   (cos-deg pitch)
                   vel)
                (* (sin-deg pitch)
                   vel)
                (* -1
                   (cos-deg yaw)
                   (cos-deg pitch)
                   vel)))))

  ^{Override {}}
  (:method #obf/obf ^{:obf/srg func_70071_h_} tick
    []
    (call-super #obf/obf ^{:obf/srg func_70071_h_} tick)
    (let [world (get-world this)]
      (if (! world field_72995_K                            ;; isRemote
             )
        (when-client
          (when (<= 0.01 (rand))
            (when (nil? (.lastStar this))
              (set! (.lastStar this)
                    (Vector3. (! this (func_174791_d        ;; getPositionVector
                                        )))))

            (let [burst-pos (Vector3. (! this (func_174791_d ;; getPositionVector
                                                )))]
              (-> (FXLightbeam. (.clone (.lastStar this)))
                  (.setup (.clone (doto (.lastStar this)
                                    (.setX (.getX burst-pos))
                                    (.setY (.getY burst-pos))
                                    (.setZ (.getZ burst-pos))
                                    (MiscUtils/applyRandomOffset random 0.5)))
                          0.1 0.1)
                  (.color VFXColorFunction/WHITE)
                  (.setAlphaMultiplier 0.8)
                  (.alpha VFXAlphaFunction/FADE_OUT)
                  (.setMaxAge (+ (* (.nextGaussian random) 2)
                                 10))
                  (EffectHelper/refresh EffectTemplatesAS/LIGHTBEAM))

              (-> (EffectHelper/of EffectTemplatesAS/GENERIC_PARTICLE)
                  (.spawn (.lastStar this))
                  (.color VFXColorFunction/WHITE)
                  (.setScaleMultiplier 0.25)
                  (.setMaxAge (+ (* (.nextGaussian random) 2)
                                 10))))))

        (let [pos (BlockPos$PooledMutable/retain this)]
          (if (or (not (!! world
                           (func_180495_p                   ;; getBlockState
                             pos)
                           (isAir world pos)))
                  (> (! this field_70173_aa                 ;; ticksExisted
                        )
                     200))
            (! this (func_70106_y                           ;; remove
                      ))

            (when (< 0.1 (Math/random))
              (! world (func_175656_a                       ;; setBlockState
                         (BlockPos$PooledMutable/retain this)
                         (! light-source (func_176223_P     ;; getDefaultState
                                           ))))))))))

  ^{Override {}}
  (:method #obf/obf ^{:obf/srg func_70088_a} registerData [])

  ^{Override {}}
  (:method #obf/obf ^{:obf/srg func_70184_a} onImpact [^RayTraceResult rtr])

  ^{Override {}}
  ^IPacket
  (:method #obf/obf ^{:obf/srg func_213297_N} createSpawnPacket []
    (NetworkHooks/getEntitySpawningPacket this))

  ^{Override {}}
  ^float
  (:method #obf/obf ^{:obf/srg func_70185_h} getGravityVelocity []
    (if (> (! this field_70173_aa                           ;; ticksExisted
              )
           60)
      0.01
      0)))

(def starlight-burst
  (!! (EntityType$Builder/create
        (reify EntityType$IFactory
          (create [_ type world]
            (EntityBurst. type world)))
        EntityClassification/MISC)
      (func_220321_a                                        ;; size
        0 0)
      (setUpdateInterval 10)
      (setTrackingRange 64)
      (setShouldReceiveVelocityUpdates true)
      (func_206865_a                                        ;; build
        "starlight_burst")))

(when-client
  (defn event-clientsetup [_]
    (RenderingRegistry/registerEntityRenderingHandler starlight-burst
                                                      (RenderEntityEmpty$Factory.))))

(defn ->EntityBurst
  [^LivingEntity entity]
  (EntityBurst. entity))

starlight-burst
