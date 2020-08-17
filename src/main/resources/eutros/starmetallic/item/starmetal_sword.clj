(ns eutros.starmetallic.item.starmetal-sword
  (:import (net.minecraft.item SwordItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           net.minecraft.world.World
           net.minecraft.entity.Entity)
  (:use [eutros.starmetallic.Starmetallic :only [tool-tier]]
        eutros.starmetallic.lib.specific-proxy
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.item.common))

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

starmetal_sword
