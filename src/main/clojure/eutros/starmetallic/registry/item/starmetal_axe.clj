(ns eutros.starmetallic.registry.item.starmetal-axe
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.registry.item.common :as cmn])
  (:import (net.minecraft.item AxeItem)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)))

(def starmetal-axe
  (when-not *compile-files*
    (proxy [AxeItem AlignmentChargeRevealer ConstellationItem]
           [;; tier
            cmn/tool-tier

            ;; attackDamage
            (identity 5.)

            ;; attackSpeed
            (identity 5.)

            ;; properties
            cmn/default-properties]
      (addInformation [stack _worldIn tooltip _flagIn]
        (cmn/add-information this stack tooltip))
      (inventoryTick [stack world entity _slot _isSelected]
        (cmn/do-regen 200 100 stack world entity))
      (shouldCauseReequipAnimation [oldStack newStack slotChanged]
        (cmn/should-reequip oldStack newStack slotChanged))
      (getAttunedConstellation [stack]
        (cmn/get-constellation stack cmn/TAG_ATTUNED IWeakConstellation))
      (setAttunedConstellation [stack cst]
        (cmn/set-constellation stack cst cmn/TAG_ATTUNED IWeakConstellation))
      (getTraitConstellation [stack]
        (cmn/get-constellation stack cmn/TAG_TRAIT IMinorConstellation))
      (setTraitConstellation [stack cst]
        (cmn/set-constellation stack cst cmn/TAG_TRAIT IMinorConstellation))
      (shouldReveal [_stack] true))))
