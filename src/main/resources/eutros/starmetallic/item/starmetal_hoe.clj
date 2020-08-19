(ns eutros.starmetallic.item.starmetal-hoe
  (:import (net.minecraft.item HoeItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           net.minecraft.world.World
           net.minecraft.entity.Entity
           hellfirepvp.astralsorcery.common.item.base.AlignmentChargeRevealer
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem IWeakConstellation IMinorConstellation))
  (:use eutros.starmetallic.lib.specific-proxy
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.item.common))

(def starmetal_hoe
  (let [do-regen (create-regen 200                          ;; starlight every
                               100                          ;; ticks
                               )]
    (sproxy
      [HoeItem AlignmentChargeRevealer ConstellationItem]
      [;; tier
       ^IItemTier tool-tier

       ;; attackSpeed
       ^float (identity 5.)

       ;; properties
       ^Item$Properties default-properties]

      ((!m 'func_77663_a                                    ;; inventoryTick
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
        (should-reequip oldStack newStack slotChanged))

      ('getAttunedConstellation [^ItemStack stack] (get-constellation stack TAG_ATTUNED IWeakConstellation))
      ('setAttunedConstellation [^ItemStack stack
                                 ^IWeakConstellation cst] (set-constellation stack cst TAG_ATTUNED IWeakConstellation))
      ('getTraitConstellation [^ItemStack stack] (get-constellation stack TAG_TRAIT IMinorConstellation))
      ('setTraitConstellation [^ItemStack stack
                               ^IMinorConstellation cst] (set-constellation stack cst TAG_TRAIT IMinorConstellation)))))

starmetal_hoe
