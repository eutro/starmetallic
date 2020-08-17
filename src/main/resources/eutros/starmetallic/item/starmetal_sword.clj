(ns eutros.starmetallic.item.starmetal-sword
  (:import (net.minecraft.item SwordItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           net.minecraft.world.World
           net.minecraft.entity.Entity
           net.minecraft.entity.player.PlayerEntity
           net.minecraftforge.fml.network.NetworkEvent$Context
           java.util.function.Supplier
           eutros.starmetallic.packets.PacketBurst
           (net.minecraftforge.event.entity.player PlayerInteractEvent$LeftClickEmpty
                                                   AttackEntityEvent)
           (net.minecraft.util Hand SoundCategory)
           java.util.function.Consumer
           hellfirepvp.astralsorcery.common.lib.SoundsAS)
  (:use [eutros.starmetallic.Starmetallic :only [tool-tier]]
        eutros.starmetallic.lib.specific-proxy
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.item.common
        eutros.starmetallic.packets))

(def starmetal_sword
  (let [do-regen (create-regen 200 ;; starlight every
                               100 ;; ticks
                               )]
    (sproxy
     [SwordItem]
     [;; tier
       ^IItemTier tool-tier

       ;; attackDamage
       ^int (identity 5)

       ;; attackSpeed
       ^float (identity 5.)

       ;; properties
       ^Item$Properties
       (-> (Item$Properties.)
           (.maxDamage (.getMaxUses ^IItemTier tool-tier)))]

     ((!m 'func_77663_a ;; inventoryTick
          )
       [^ItemStack stack
        ^World world
        ^Entity entity
        ^int slot
        ^boolean isSelected]
       (do-regen stack world entity slot isSelected))

     ('shouldCauseReequipAnimation
      [^ItemStack oldStack
       ^ItemStack newStack
       ^boolean slotChanged]
      (should-reequip oldStack newStack slotChanged)))))

(defn check-stack
  [^ItemStack stack]
  (and
   (not
    (! stack
       (func_190926_b ;; isEmpty
         )))
   (-> stack
       (!
        (func_77973_b ;; getItem
          ))
       (= starmetal_sword))))

(def ^:dynamic ->EntityBurst nil)

(defn summon-burst
  [^PlayerEntity player]
  (when
    (and
      (check-stack
        (! player
           (func_184614_ca ;; getHeldItemMainhand
             )))
      (=
        (! player
           (func_184825_o ;; getCooledAttackStrength
            0))
        1.))
    (as-> (->EntityBurst player) $
          (!! player
              field_70170_p ;; world
              (func_217411_a ;; addEntity
               $)))
    (!! player
        (func_184614_ca ;; getHeldItemMainhand
          )
        (func_222118_a ;; damageItem
         1
         player
         (reify
          Consumer
          (accept [_ p]
                  (! p
                     (func_213334_d ;; sendBreakAnimation
                      Hand/MAIN_HAND))))))
    (!! player
        field_70170_p ;; world
        (func_184148_a ;; playSound
         ^PlayerEntity (identity nil)
         (! player func_226277_ct_ ;; getPosX
            )
         (! player func_226278_cu_ ;; getPosY
            )
         (! player func_226281_cx_ ;; getPosZ
            )
         SoundsAS/ILLUMINATION_WAND_LIGHT
         SoundCategory/PLAYERS
         0.4
         1.4))))

(defn event-leftclickempty
  [^PlayerInteractEvent$LeftClickEmpty evt]
  (when (check-stack (.getItemStack evt))
    (.sendToServer CHANNEL (PacketBurst.))))

(defn event-attackentity
  [^AttackEntityEvent evt]
  (when-not
    (!! evt
        (getPlayer)
        field_70170_p ;; world
        field_72995_K ;; isRemote
        )
    (summon-burst (.getPlayer evt))))

(alter-var-root
 #'burst-handler
 (constantly
  (fn [^Supplier ctx-supplier]
    (let [ctx ^NetworkEvent$Context (.get ctx-supplier)]
      (-> (.getSender ctx)
          summon-burst)))))

starmetal_sword
