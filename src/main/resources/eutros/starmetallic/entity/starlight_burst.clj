(ns eutros.starmetallic.entity.starlight-burst
  (:import (net.minecraft.entity Entity
                                 EntityType
                                 EntityType$Builder
                                 EntityType$IFactory
                                 EntityClassification
                                 LivingEntity)
           net.minecraft.entity.projectile.ThrowableEntity
           net.minecraft.world.World
           net.minecraft.particles.ParticleTypes
           (net.minecraft.util.math RayTraceResult BlockPos$PooledMutable)
           net.minecraftforge.fml.network.NetworkHooks
           (net.minecraftforge.fml.client.registry RenderingRegistry)
           (hellfirepvp.astralsorcery.client.render.entity RenderEntityEmpty$Factory))
  (:use eutros.starmetallic.lib.subclass
        eutros.starmetallic.item.starmetal-sword
        eutros.starmetallic.block.light-source
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.lib.sided
        eutros.starmetallic.lib.math
        eutros.starmetallic.Starmetallic))

(defclass EntityBurst
  :prefix 'burst-
  :super ThrowableEntity
  :constructors
  [{:super [EntityType World]}
   {:super [EntityType Double/TYPE Double/TYPE Double/TYPE World]}
   {:super [EntityType LivingEntity World]}
   {:super [EntityType LivingEntity World]
    :args  [LivingEntity]
    :pre   'constructor
    :post  'from-player-look}]
  :exposes-methods
  [{:alias 'p$setRotation
    :name  (!m 'func_70101_b                                ;; setRotation
               )
    :types [Float/TYPE Float/TYPE]}]
  :methods
  [{:alias 'tick
    :name  (!m 'func_70071_h_                               ;; tick
               )
    :types []}
   {:alias 'register-data
    :name  (!m 'func_70088_a                                ;; registerData
               )
    :types []}
   {:alias 'on-impact
    :name  (!m 'func_70184_a                                ;; onImpact
               )
    :types [RayTraceResult]}
   {:alias 'create-spawn-packet
    :name  (!m 'func_213297_N                               ;; createSpawnPacket
               )
    :types []}
   {:alias 'get-gravity
    :name  (!m 'func_70185_h                                ;; getGravityVelocity
               )
    :types []}])

(defn get-world
  [^Entity entity]
  (! entity field_70170_p                                   ;; world
     ))

(defn burst-tick
  [^EntityBurst this]
  (.s$tick this)
  (let [world (get-world this)
        x (! this (func_226277_ct_                          ;; getPosX
                    ))
        y (! this (func_226278_cu_                          ;; getPosY
                    ))
        z (! this (func_226281_cx_                          ;; getPosZ
                    ))]
    (if (! world field_72995_K                              ;; isRemote
           )
      (.addParticle world
                    ParticleTypes/LARGE_SMOKE
                    x y z
                    0. 0. 0.)
      (let [pos (BlockPos$PooledMutable/retain this)]
        (if (or (not (!! world
                       (func_180495_p                       ;; getBlockState
                         pos)
                       (isAir world pos)))
                (> (! this field_70173_aa                   ;; ticksExisted
                      )
                   200))
          (! this (func_70106_y                             ;; remove
                    ))

          (when (< 0.1 (Math/random))
            (! world (func_175656_a                         ;; setBlockState
                       (BlockPos$PooledMutable/retain this)
                       (! light-source (func_176223_P       ;; getDefaultState
                                         ))))))))))

(defn burst-register-data [^EntityBurst _] nil)

(defn burst-create-spawn-packet
  [^EntityBurst this]
  (NetworkHooks/getEntitySpawningPacket this))

(defn burst-on-impact
  [^EntityBurst _ ^RayTraceResult _]
  nil)

(defn burst-get-gravity
  [^EntityBurst this]
  (if (> (! this field_70173_aa                             ;; ticksExisted
            )
         60)
    0.01
    0))

(def starlight-burst
  (!! (EntityType$Builder/create
        (reify EntityType$IFactory
          (create [_ type world]
            (EntityBurst. type world)))
        EntityClassification/MISC)
    (func_220321_a                                          ;; size
      0 0)
    (setUpdateInterval 10)
    (setTrackingRange 64)
    (setShouldReceiveVelocityUpdates true)
    (func_206865_a                                          ;; build
      "starlight_burst")))

(defn burst-constructor
  [^LivingEntity entity]
  [starlight-burst
   entity
   (get-world entity)])

(defn burst-from-player-look
  [^EntityBurst this ^LivingEntity entity]
  (let [yaw (mod (+ (! entity field_70177_z                 ;; rotationYaw
                       )
                    180)
                 360)
        pitch (mod (- (! entity field_70125_A               ;; rotationPitch
                         ))
                   360)
        vel 0.5]
    (.p$setRotation this yaw pitch)
    (! this (func_213293_j                                  ;; setMotion
              (* (sin-deg yaw)
                 (cos-deg pitch)
                 vel)
              (* (sin-deg pitch)
                 vel)
              (* -1
                 (cos-deg yaw)
                 (cos-deg pitch)
                 vel)))))

(when-client
  (defn event-clientsetup [_]
    (RenderingRegistry/registerEntityRenderingHandler starlight-burst
                                                      (RenderEntityEmpty$Factory.))))

(alter-var-root
  #'->EntityBurst
  (constantly
    (fn [^LivingEntity entity]
      (EntityBurst. entity))))

starlight-burst
