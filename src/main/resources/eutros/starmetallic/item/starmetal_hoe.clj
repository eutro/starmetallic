(ns eutros.starmetallic.item.starmetal-hoe
  (:import (net.minecraft.item HoeItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           net.minecraft.world.World
           net.minecraft.entity.Entity
           hellfirepvp.astralsorcery.common.item.base.AlignmentChargeRevealer
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem IWeakConstellation IMinorConstellation)
           (java.util List))
  (:use eutros.clojurelib.lib.class-gen
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.item.common))

(def do-regen
  (create-regen 200                                         ;; starlight every
                100                                         ;; ticks
                ))

#rip/rip ^{}
(defclass
  SMHoe (:extends HoeItem) (:implements AlignmentChargeRevealer ConstellationItem)

  (:constructor []
    (super [;; tier
            ^IItemTier tool-tier

            ;; attackSpeed
            ^float (identity 5.)

            ;; properties
            ^Item$Properties default-properties]))

  #rip/client ^void
  (:method #obf/obf ^{:obf/srg func_77624_a} addInformation
    [^ItemStack stack
     ^World worldIn
     ^List tooltip
     ^net.minecraft.client.util.ITooltipFlag flagIn]
    (add-information this stack tooltip))

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

(def starmetal-hoe (SMHoe.))

starmetal-hoe
