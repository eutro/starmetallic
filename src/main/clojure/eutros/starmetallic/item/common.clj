(ns eutros.starmetallic.item.common
  (:require [eutros.starmetallic.compilerhack.clinitfilter])
  (:import (net.minecraft.item ItemStack IItemTier Item$Properties ItemGroup)
           net.minecraft.item.crafting.Ingredient
           net.minecraft.world.World
           net.minecraft.entity.player.PlayerEntity
           hellfirepvp.astralsorcery.common.auxiliary.charge.AlignmentChargeHandler
           net.minecraftforge.fml.LogicalSide
           (hellfirepvp.astralsorcery.common.constellation IConstellation ConstellationItem)
           (hellfirepvp.astralsorcery.common.data.research ResearchHelper GatedKnowledge)
           (net.minecraft.util.text TranslationTextComponent TextFormatting)
           (java.util List))
  (:use eutros.starmetallic.core))

(defn should-reequip
  [^ItemStack oldStack
   ^ItemStack newStack
   slotChanged]
  (or slotChanged
      (not= (.getItem oldStack)
            (.getItem newStack))))

(def tool-tier
  (reify IItemTier
    (getMaxUses [_] 100)
    (getEfficiency [_] 7.)
    (getAttackDamage [_] 6.)
    (getHarvestLevel [_] 4)
    (getEnchantability [_] 40)
    (getRepairMaterial [_] Ingredient/EMPTY)))

(def item-group
  (when-not *compile-files*
    (proxy [ItemGroup] [MODID]
      (createIcon []
        (ItemStack. @(ns-resolve 'eutros.starmetallic.item.starmetal-sword
                                 'starmetal-sword))))))

(def default-properties
  (when-not *compile-files*
    (-> (Item$Properties.)
        (.maxDamage (.getMaxUses tool-tier))
        (.group item-group))))

(defn do-regen
  [charge-per-damage
   ticks-between-regen
   ^ItemStack stack
   ^World world
   entity]
  (when (and (not (.isRemote world))
             (.isDamaged stack)
             (instance? PlayerEntity entity)
             (-> (.getGameTime world)
                 (mod ticks-between-regen)
                 zero?)
             (.drainCharge AlignmentChargeHandler/INSTANCE
                           ^PlayerEntity entity
                           LogicalSide/SERVER
                           charge-per-damage
                           false))
    (->> (.getDamage stack)
         (+ -1)
         (.setDamage stack))))

(def TAG_ATTUNED (str MODID ":attuned"))
(def TAG_TRAIT (str MODID ":trait"))

(defn get-constellation
  [^ItemStack stack
   ^String key
   ^Class clazz]
  (when-let [cst (some-> (.getTag stack)
                         (IConstellation/readFromNBT key))]
    (when (instance? clazz cst) cst)))

(defn set-constellation
  [^ItemStack stack
   ^IConstellation cst
   ^String key
   ^Class clazz]
  (if (instance? clazz cst)
    (do (.writeToNBT cst (.getOrCreateTag stack) key)
        true)
    (do (some-> (.getTag stack)
                (.remove key))
        false)))

(defn add-information [^ConstellationItem item
                       ^ItemStack stack
                       ^List tooltip]
  (let [progress (ResearchHelper/getClientProgress)
        tier (.getTierReached progress)]
    (when-some [attuned (.getAttunedConstellation item stack)]
      (.add tooltip (-> (if (and (.canSee GatedKnowledge/CRYSTAL_TUNE tier)
                                 (.hasConstellationDiscovered progress attuned))
                          (TranslationTextComponent.
                            "crystal.info.astralsorcery.attuned"
                            (into-array Object
                                        [(-> (.getConstellationName attuned)
                                             (.applyTextStyle TextFormatting/BLUE))]))
                          (TranslationTextComponent.
                            "astralsorcery.progress.missing.knowledge"
                            (make-array Object 0)))
                        (.applyTextStyle TextFormatting/GRAY))))
    (when-some [trait (.getTraitConstellation item stack)]
      (.add tooltip (-> (if (and (.canSee GatedKnowledge/CRYSTAL_TUNE tier)
                                 (.hasConstellationDiscovered progress trait))
                          (TranslationTextComponent.
                            "crystal.info.astralsorcery.trait"
                            (into-array Object
                                        [(-> (.getConstellationName trait)
                                             (.applyTextStyle TextFormatting/BLUE))]))
                          (TranslationTextComponent.
                            "astralsorcery.progress.missing.knowledge"
                            (make-array Object 0)))
                        (.applyTextStyle TextFormatting/GRAY))))))
