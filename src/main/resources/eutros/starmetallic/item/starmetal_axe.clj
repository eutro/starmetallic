(ns eutros.starmetallic.item.starmetal-axe
  (:import (net.minecraft.item AxeItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           net.minecraft.world.World
           net.minecraft.entity.Entity
           hellfirepvp.astralsorcery.common.item.base.AlignmentChargeRevealer
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem IWeakConstellation IMinorConstellation))
  (:use eutros.clojurelib.lib.class-gen
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.item.common))

(def do-regen
  (create-regen 200                                         ;; starlight every
                100                                         ;; ticks
                ))

(defclass
  SMAxe (:extends AxeItem) (:implements AlignmentChargeRevealer ConstellationItem)

  (:constructor []
    (super [;; tier
            ^IItemTier tool-tier

            ;; attackDamage
            ^float (identity 5.)

            ;; attackSpeed
            ^float (identity 5.)

            ;; properties
            ^Item$Properties default-properties]))

  (:method #obf/obf ^{:obf/srg func_77663_a} inventoryTick
    [^ItemStack stack ^World world ^Entity entity ^int slot ^boolean isSelected]
    (do-regen stack world entity slot isSelected))

  ^boolean
  (:method shouldCauseReequipAnimation
    [^ItemStack oldStack
     ^ItemStack newStack
     ^boolean slotChanged]
    (should-reequip oldStack newStack slotChanged))

  ^IWeakConstellation
  (:method getAttunedConstellation
    [^ItemStack stack]
    (get-constellation stack TAG_ATTUNED IWeakConstellation))

  ^boolean
  (:method setAttunedConstellation
    [^ItemStack stack
     ^IWeakConstellation cst]
    (set-constellation stack cst TAG_ATTUNED IWeakConstellation))

  ^IMinorConstellation
  (:method getTraitConstellation
    [^ItemStack stack]
    (get-constellation stack TAG_TRAIT IMinorConstellation))

  ^boolean
  (:method setTraitConstellation
    [^ItemStack stack
     ^IMinorConstellation cst]
    (set-constellation stack cst TAG_TRAIT IMinorConstellation)))

(def starmetal-axe (SMAxe.))

starmetal-axe
