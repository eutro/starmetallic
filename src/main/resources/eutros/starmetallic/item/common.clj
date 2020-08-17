(ns eutros.starmetallic.item.common
  (:import net.minecraft.item.ItemStack
           net.minecraft.world.World
           net.minecraft.entity.Entity
           net.minecraft.entity.player.PlayerEntity
           hellfirepvp.astralsorcery.common.auxiliary.charge.AlignmentChargeHandler
           net.minecraftforge.fml.LogicalSide)
  (:use eutros.starmetallic.lib.obfuscation))

(defn should-reequip
  [^ItemStack oldStack
   ^ItemStack newStack
   slotChanged]
  (or slotChanged
      (not=
        (! oldStack
           (func_77973_b ;; getItem
             ))
        (! newStack
           (func_77973_b ;; getItem
             )))))

(defn create-regen
  [^Integer charge-per-damage ticks-between-regen]
  (fn
    [^ItemStack stack
     ^World world
     ^Entity entity
     ^Integer slot
     ^Boolean isSelected]
    (when
      (and
       (not
        (! world field_72995_K ;; isRemote
           ))
       (! stack
          (func_77951_h ;; isDamaged
            ))
       (instance? PlayerEntity entity)
       (->
        (! world
           (func_82737_E ;; getGameTime
             ))
        (mod ticks-between-regen)
        zero?)
       (.drainCharge AlignmentChargeHandler/INSTANCE
                     ^PlayerEntity entity
                     LogicalSide/SERVER
                     charge-per-damage
                     false))
      (! stack
         (func_196085_b ;; setDamage
          (-
           (! stack
              (func_77952_i ;; getDamage
                ))
           1))))))
