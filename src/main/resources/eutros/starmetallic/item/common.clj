(ns eutros.starmetallic.item.common
  (:import (net.minecraft.item ItemStack IItemTier Item$Properties ItemGroup)
           net.minecraft.item.crafting.Ingredient
           net.minecraft.world.World
           net.minecraft.entity.Entity
           net.minecraft.entity.player.PlayerEntity
           hellfirepvp.astralsorcery.common.auxiliary.charge.AlignmentChargeHandler
           net.minecraftforge.fml.LogicalSide
           (hellfirepvp.astralsorcery.common.constellation IWeakConstellation IConstellation))
  (:use eutros.starmetallic.Starmetallic
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.lib.sided
        eutros.starmetallic.lib.specific-proxy))

(defn should-reequip
  [^ItemStack oldStack
   ^ItemStack newStack
   slotChanged]
  (or slotChanged
      (not= (! oldStack
               (func_77973_b                                ;; getItem
                 ))
            (! newStack
               (func_77973_b                                ;; getItem
                 )))))

(def tool-tier
  (sproxy [IItemTier] []
    ((!m 'func_200926_a                                     ;; getMaxUses
       )
     [] 100)
    ((!m 'func_200928_b                                     ;; getEfficiency
       )
     [] 7.)
    ((!m 'func_200929_c                                     ;; getAttackDamage
       )
     [] 6.)
    ((!m 'func_200925_d                                     ;; getHarvestLevel
       )
     [] 4)
    ((!m 'func_200927_e                                     ;; getEnchantability
       )
     [] 40)
    ((!m 'func_200924_f                                     ;; getRepairMaterial
       )
     [] Ingredient/EMPTY)))

(def item-group
  (if-client
    (sproxy [ItemGroup] [^String MODID]
      ((!m 'func_151244_d                                   ;; getIcon
         )
       []
       (ItemStack. @(ns-resolve 'eutros.starmetallic.item.starmetal-sword
                                'starmetal-sword))))
    (sproxy [ItemGroup] [^String MODID])))

(def default-properties
  (!! (Item$Properties.)
    (func_200918_c                                          ;; maxDamage
      (! tool-tier
         (func_200926_a                                     ;; getMaxUses
           )))
    (func_200916_a                                          ;; group
      item-group)))

(defn create-regen
  [^Integer charge-per-damage ticks-between-regen]
  (fn
    [^ItemStack stack
     ^World world
     ^Entity entity
     ^Integer slot
     ^Boolean isSelected]
    (when (and (not (! world field_72995_K                  ;; isRemote
                       ))
               (! stack (func_77951_h                       ;; isDamaged
                          ))
               (instance? PlayerEntity entity)
               (-> (! world
                      (func_82737_E                         ;; getGameTime
                        ))
                   (mod ticks-between-regen)
                   zero?)
               (.drainCharge AlignmentChargeHandler/INSTANCE
                             ^PlayerEntity entity
                             LogicalSide/SERVER
                             charge-per-damage
                             false))
      (! stack (func_196085_b                               ;; setDamage
                 (- (! stack (func_77952_i                  ;; getDamage
                               ))
                    1))))))

(defn get-or-create-tag
  [^ItemStack stack]
  (! stack (func_196082_o                                   ;; getOrCreateTag
             )))

(defn get-tag
  [^ItemStack stack]
  (! stack (func_77978_p                                    ;; getTag
             )))

(def TAG_ATTUNED (str MODID ":attuned"))
(def TAG_TRAIT (str MODID ":attuned"))

(defn get-constellation
  [^ItemStack stack
   ^String key
   ^Class clazz]
  (when-let [cst (some-> (get-tag stack)
                         (IConstellation/readFromNBT key))]
    (when (instance? clazz cst) cst)))

(defn set-constellation
  [^ItemStack stack
   ^IConstellation cst
   ^String key
   ^Class clazz]
  (if (instance? clazz cst)
    (as-> (get-or-create-tag stack) $
          (.writeToNBT cst $ key))
    (some-> (get-tag stack)
            (! (func_82580_o                                ;; remove
                 key)))))
