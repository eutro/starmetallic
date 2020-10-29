(ns eutros.starmetallic.registry.item.starmetal-hoe
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.registry.item.common :as cmn]
            [eutros.starmetallic.lib.functions :refer [consumer]])
  (:import (net.minecraft.item HoeItem ItemUseContext)
           (hellfirepvp.astralsorcery.common.item.base AlignmentChargeRevealer)
           (hellfirepvp.astralsorcery.common.constellation ConstellationItem
                                                           IWeakConstellation
                                                           IMinorConstellation)
           (net.minecraft.util ActionResultType Direction SoundCategory)
           (net.minecraft.util.math BlockPos)
           (net.minecraftforge.event ForgeEventFactory)
           (hellfirepvp.astralsorcery.common.lib SoundsAS ColorsAS)
           (hellfirepvp.astralsorcery.client.effect.source.orbital FXOrbitalCollector)
           (hellfirepvp.astralsorcery.common.util.data Vector3)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)))

(declare hoe-effect)

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
        (cmn/set-constellation stack cst cmn/TAG_TRAIT IMinorConstellation))
      (shouldReveal [_stack] true)
      (onItemUse [context]
        (hoe-effect context)))))

(defn hoe-effect
  [^ItemUseContext context]
  (let [hook (ForgeEventFactory/onHoeUse context)]
    (if (zero? hook)
      (let [player (.getPlayer context)
            world (.getWorld context)
            item (.getItem context)]
        (if (not= Direction/DOWN (.getFace context))
          (let [it (.iterator (BlockPos/getAllInBoxMutable
                                (-> (.getPos context)
                                    (.add -1 0 -1))
                                (-> (.getPos context)
                                    (.add 1 0 1))))]
            (while (.hasNext it)
              (let [pos ^BlockPos (.next it)]
                (when (.isAirBlock world (.up pos))
                  (when-some [new-state
                              (get HoeItem/HOE_LOOKUP
                                   (-> world (.getBlockState pos) .getBlock))]
                    (when-not (.isRemote world)
                      (.setBlockState world pos new-state)
                      (when player
                        (.damageItem
                          item 1 player
                          (consumer [player]
                            (.sendBreakAnimation player (.getHand context))))))))))
            (.playSound world
                        player
                        (.getPos context)
                        SoundsAS/ILLUMINATION_WAND_LIGHT
                        SoundCategory/BLOCKS
                        1.0 1.4)
            (when (.isRemote world)
              (let [attuned (.getAttunedConstellation ^ConstellationItem (.getItem item) item)
                    trait (.getTraitConstellation ^ConstellationItem (.getItem item) item)
                    pos (-> context .getPos Vector3.
                            (.add (float 0.5)
                                  (float 1)
                                  (float 0.5)))]
                (-> (FXOrbitalCollector. pos ColorsAS/DYE_WHITE)
                    (.setBranches (inc (+ (if attuned 0 1)
                                          (if trait 0 1))))
                    (.setTicksPerRotation 30)
                    (EffectHelper/spawnSource))
                (when attuned
                  (-> (FXOrbitalCollector. pos (.getConstellationColor attuned))
                      (.setBranches (if trait 1 2))
                      (.setTicksPerRotation 30)
                      (.setTickOffset (if trait 10 15))
                      (EffectHelper/spawnSource)))
                (when trait
                  (-> (FXOrbitalCollector. pos (.getConstellationColor trait))
                      (.setBranches (if attuned 1 2))
                      (.setTicksPerRotation 30)
                      (.setTickOffset (if attuned 10 15))
                      (EffectHelper/spawnSource)))))
            ActionResultType/SUCCESS)
          ActionResultType/FAIL))
      (if (> hook 0)
        ActionResultType/SUCCESS
        ActionResultType/FAIL))))
