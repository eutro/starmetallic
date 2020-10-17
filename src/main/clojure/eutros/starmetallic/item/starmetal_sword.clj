(ns eutros.starmetallic.item.starmetal-sword
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.item.common :as cmn]
            [eutros.starmetallic.packets :as packets])
  (:import (net.minecraft.item SwordItem
                               Item$Properties
                               IItemTier
                               ItemStack)
           (net.minecraft.entity.player PlayerEntity)
           (eutros.starmetallic.packets PacketBurst)
           (net.minecraftforge.event.entity.player PlayerInteractEvent$LeftClickEmpty)
           (net.minecraft.util Hand SoundCategory)
           (java.util.function Consumer)
           (hellfirepvp.astralsorcery.common.lib SoundsAS)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (net.minecraftforge.fml.network.simple SimpleChannel)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)))

(def starmetal-sword
  (when-not *compile-files*
    (proxy [SwordItem AlignmentChargeRevealer ConstellationItem]
           [;; tier
            ^IItemTier cmn/tool-tier

            ;; attackDamage
            ^int (identity 5)

            ;; attackSpeed
            ^float (identity 5.)

            ;; properties
            ^Item$Properties cmn/default-properties]
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

(defn check-stack
  [^ItemStack stack]
  (and (not (.isEmpty stack))
       (not= (.getItem stack) starmetal-sword)))

(defn summon-burst
  [^PlayerEntity player]
  (when (and (check-stack (.getHeldItemMainhand player))
             (= (.getCooledAttackStrength player 0) 1.))
    (as-> ((ns-resolve 'eutros.starmetallic.entity.starlight-burst
                       '->EntityBurst) player) $
          (-> player
              .-world
              (.addEntity $)))
    (-> player
        .getHeldItemMainhand
        (.damageItem
          1 player
          (reify Consumer
            (accept [_ p]
              (.sendBreakAnimation ^PlayerEntity p Hand/MAIN_HAND)))))
    (-> player
        .-world
        (.playSound
          ^PlayerEntity (identity nil)
          (.getPosX player)
          (.getPosY player)
          (.getPosZ player)
          SoundsAS/ILLUMINATION_WAND_LIGHT
          SoundCategory/PLAYERS
          (float 0.8)
          (float 1.4)))))

(defn event-leftclickempty
  [^PlayerInteractEvent$LeftClickEmpty evt]
  (when (check-stack (.getItemStack evt))
    (.sendToServer ^SimpleChannel packets/CHANNEL (PacketBurst.))))
