(ns eutros.starmetallic.registry.block.light-source
  (:require [eutros.starmetallic.compilerhack.clinitfilter])
  (:import (net.minecraft.block Block
                                Blocks
                                Block$Properties
                                BlockRenderType)
           (net.minecraft.util.math BlockPos)
           (net.minecraft.world.server ServerWorld)
           (net.minecraft.world World)
           (net.minecraft.util.math.shapes VoxelShapes)))

(def light-source
  (when-not *compile-files*
    (proxy [Block]
           [(-> (Block$Properties/from Blocks/AIR)
                (.lightValue 8))]
      (getRenderType [_state]
        BlockRenderType/INVISIBLE)

      (getShape [_state _worldIn _pos _context]
        (VoxelShapes/empty))

      (onBlockAdded [_state
                     ^World world
                     ^BlockPos pos
                     _oldState
                     _isMoving]
        (-> world .getPendingBlockTicks
            (.scheduleTick pos this 10)))

      (tick [_state
             ^ServerWorld world
             ^BlockPos pos
             _rand]
        (.removeBlock world pos false)))))
