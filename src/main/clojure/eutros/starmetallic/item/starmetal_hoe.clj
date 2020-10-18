(ns eutros.starmetallic.item.starmetal-hoe
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.item.common :as cmn])
  (:import (net.minecraft.item HoeItem)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)))

(def starmetal-hoe
  (when-not *compile-files*
    (proxy [HoeItem AlignmentChargeRevealer ConstellationItem]
           [;; tier
            cmn/tool-tier

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
        (cmn/set-constellation stack cst cmn/TAG_TRAIT IMinorConstellation)))))
