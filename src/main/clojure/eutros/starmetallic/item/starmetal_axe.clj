(ns eutros.starmetallic.item.starmetal-axe
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.item.common :as cmn])
  (:import (net.minecraft.item AxeItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           (net.minecraft.world World)
           (net.minecraft.entity Entity)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)))

(def starmetal-axe
  (when-not *compile-files*
    (proxy [AxeItem AlignmentChargeRevealer ConstellationItem]
           [;; tier
            ^IItemTier cmn/tool-tier

            ;; attackDamage
            ^float (identity 5.)

            ;; attackSpeed
            ^float (identity 5.)

            ;; properties
            ^Item$Properties cmn/default-properties]
      (addInformation [stack _worldIn tooltip _flagIn]
        (cmn/add-information this stack tooltip))
      (inventoryTick [^ItemStack stack ^World world ^Entity entity ^int _slot ^boolean _isSelected]
        (cmn/do-regen 200 100 stack world entity))
      (shouldCauseReequipAnimation [oldStack newStack slotChanged]
        (cmn/should-reequip oldStack newStack slotChanged))
      (getAttunedConstellation [^ItemStack stack]
        (cmn/get-constellation stack cmn/TAG_ATTUNED IWeakConstellation))
      (setAttunedConstellation [stack cst]
        (cmn/set-constellation stack cst cmn/TAG_ATTUNED IWeakConstellation))
      (getTraitConstellation [stack]
        (cmn/get-constellation stack cmn/TAG_TRAIT IMinorConstellation))
      (setTraitConstellation [stack cst]
        (cmn/set-constellation stack cst cmn/TAG_TRAIT IMinorConstellation)))))
