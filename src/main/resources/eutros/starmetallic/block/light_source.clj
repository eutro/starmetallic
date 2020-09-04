(ns eutros.starmetallic.block.light-source
  (:import (net.minecraft.block Block
                                Blocks
                                Block$Properties
                                BlockState
                                BlockRenderType)
           net.minecraft.util.math.BlockPos
           java.util.Random
           net.minecraft.world.server.ServerWorld
           (net.minecraft.world IBlockReader World)
           (net.minecraft.util.math.shapes ISelectionContext
                                           VoxelShapes))
  (:use eutros.clojurelib.lib.class-gen
        eutros.starmetallic.lib.obfuscation))

(defclass
  LightAir (:extends Block)

  (:constructor []
    (super [;; properties
            ^Block$Properties
            (!! Block$Properties
              (func_200950_a                                ;; from
                (! Blocks field_150350_a                    ;; AIR
                   ))
              (func_200951_a                                ;; lightValue
                8))]))

  (:method #obf/obf ^{:obf/srg func_149645_b} getRenderType
    [^BlockState state] BlockRenderType/INVISIBLE)

  (:method #obf/obf ^{:obf/srg func_220053_a} getShape
    [^BlockState state
     ^IBlockReader worldIn
     ^BlockPos pos
     ^ISelectionContext context]
    (! VoxelShapes
       (func_197880_a                                ;; empty
         )))

  (:method #obf/obf ^{:obf/srg func_220082_b} onBlockAdded
    [^BlockState state
     ^World world
     ^BlockPos pos
     ^BlockState oldState
     ^boolean isMoving]
    (!! world
      (func_205220_G_                                ;; getPendingBlockTicks
        )
      (func_205362_a                                 ;; scheduleTick
        pos
        this
        10)))

  (:method #obf/obf ^{:obf/srg func_225534_a_} tick
    [^BlockState state
     ^ServerWorld world
     ^BlockPos pos
     ^Random rand]
    (! world
       (func_217377_a                                ;; removeBlock
         pos
         false))))

(def light-source (LightAir.))

light-source
