(ns eutros.starmetallic.entity.starlight-burst
  (:import (net.minecraft.entity Entity
                                 EntityType
                                 EntityType$Builder
                                 EntityType$IFactory
                                 EntityClassification
                                 LivingEntity)
           net.minecraft.entity.projectile.ThrowableEntity
           net.minecraft.world.World
           net.minecraft.util.math.RayTraceResult
           net.minecraftforge.fml.network.NetworkHooks)
  (:use eutros.starmetallic.lib.subclass
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.lib.sided
        eutros.starmetallic.Starmetallic))

(defclass EntityBurst
  :prefix 'burst-
  :super ThrowableEntity
  :constructors
  [{:super [EntityType World]}
   {:super [EntityType Double/TYPE Double/TYPE Double/TYPE World]}
   {:super [EntityType LivingEntity World]}]
  :methods
  [{:alias 'tick
    :name  (!m 'func_70071_h_ ;; tick
               )
    :types []}
   {:alias 'register-data
    :name  (!m 'func_70088_a ;; registerData
               )
    :types []}
   {:alias 'on-impact
    :name  (!m 'func_70184_a ;; onImpact
               )
    :types [RayTraceResult]}
   {:alias 'create-spawn-packet
    :name (!m 'func_213297_N ;; createSpawnPacket
              )
    :types []}])

(defn burst-tick
  [^EntityBurst this]
  (.s$tick this)
  (let [world ^World (! this field_70170_p ;; world
                        )
        pos   (.getPositionVector ^ThrowableEntity this)
        x     (. pos x)
        y     (. pos y)
        z     (. pos z)]
    (when
      (! world field_72995_K ;; isRemote
         )
      (.addParticle world
                    net.minecraft.particles.ParticleTypes/LARGE_SMOKE
                    x y z
                    0. 0. 0.))))

(defn burst-register-data [^EntityBurst this] nil)

(defn burst-create-spawn-packet
  [^EntityBurst this]
  (NetworkHooks/getEntitySpawningPacket this))

(defn burst-on-impact
  [^EntityBurst this ^RayTraceResult rtr]
  nil)

(def starlight-burst
  (!!
   (EntityType$Builder/create
    (reify
     EntityType$IFactory
     (create [_ type world]
             (EntityBurst. type world)))
    EntityClassification/MISC)
   (func_220321_a ;; size
    0.5 0.5)
   (setUpdateInterval 10)
   (setTrackingRange 64)
   (setShouldReceiveVelocityUpdates true)
   (func_206865_a ;; build
    "starlight_burst")))

(when-client
 (import
  net.minecraftforge.fml.client.registry.RenderingRegistry
  hellfirepvp.astralsorcery.client.render.entity.RenderEntityEmpty$Factory)

 (defn event-clientsetup
   [evt]
   (RenderingRegistry/registerEntityRenderingHandler starlight-burst
                                                     (RenderEntityEmpty$Factory.))))

starlight-burst
