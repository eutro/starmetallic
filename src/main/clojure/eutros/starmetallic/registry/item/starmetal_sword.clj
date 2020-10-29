(ns eutros.starmetallic.registry.item.starmetal-sword
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.registry.item.common :as cmn]
            [eutros.starmetallic.packets :as packets]
            [eutros.starmetallic.lib.events :as events]
            [eutros.starmetallic.lib.functions :refer [biconsumer function consumer]]
            [eutros.starmetallic.registry.entity.entities :as entities])
  (:import (net.minecraft.item SwordItem ItemStack)
           (net.minecraft.entity.player PlayerEntity)
           (net.minecraftforge.event.entity.player PlayerInteractEvent$LeftClickEmpty)
           (net.minecraft.util Hand SoundCategory)
           (java.util.function Supplier)
           (hellfirepvp.astralsorcery.common.lib SoundsAS)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (net.minecraftforge.fml.network.simple SimpleChannel)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)
           (net.minecraftforge.fml.network NetworkEvent$Context)))

(def starmetal-sword
  (when-not *compile-files*
    (proxy [SwordItem AlignmentChargeRevealer ConstellationItem]
           [;; tier
            cmn/tool-tier

            ;; attackDamage
            (identity 5)

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

(defn check-stack
  [^ItemStack stack]
  (and (not (.isEmpty stack))
       (= (.getItem stack) starmetal-sword)))

(defn summon-burst
  [^PlayerEntity player]
  (when (and (check-stack (.getHeldItemMainhand player))
             (= (.getCooledAttackStrength player 0) 1.))
    (as-> (entities/->starlight-burst player) $
          (-> player .-world (.addEntity $)))
    (-> player
        .getHeldItemMainhand
        (.damageItem
          1 player
          (consumer [p]
            (.sendBreakAnimation ^PlayerEntity p Hand/MAIN_HAND))))
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

(deftype PacketBurst [])
(when-not *compile-files*
  (.registerMessage
    ^SimpleChannel packets/CHANNEL
    1 PacketBurst
    ;; encoder
    (biconsumer [_ _] nil)
    ;; decoder
    (function [_] (PacketBurst.))
    ;; handler
    (biconsumer
      [_ ctx-supplier]
      (-> ^Supplier
          ctx-supplier
          ^NetworkEvent$Context
          .get .getSender (summon-burst))))
  (events/listen
    PlayerInteractEvent$LeftClickEmpty
    (fn [^PlayerInteractEvent$LeftClickEmpty evt]
      (when (check-stack (.getItemStack evt))
        (.sendToServer ^SimpleChannel packets/CHANNEL (PacketBurst.))))))

(deliver cmn/sword* starmetal-sword)
