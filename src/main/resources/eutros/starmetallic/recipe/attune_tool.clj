(ns eutros.starmetallic.recipe.attune-tool
  (:import (hellfirepvp.astralsorcery.common.crafting.nojson AttunementCraftingRegistry)
           (hellfirepvp.astralsorcery.common.crafting.nojson.attunement AttunementRecipe AttunementRecipe$Active)
           (net.minecraft.util ResourceLocation)
           (hellfirepvp.astralsorcery.common.tile TileAttunementAltar)
           (hellfirepvp.astralsorcery.common.constellation.world DayTimeHelper)
           (net.minecraft.util.math AxisAlignedBB)
           (hellfirepvp.astralsorcery.common.util.data Vector3 Vector3$RotAxis)
           (net.minecraft.entity.item ItemEntity)
           (hellfirepvp.astralsorcery.common.constellation IConstellation IWeakConstellation ConstellationItem IMinorConstellation)
           (net.minecraftforge.fml LogicalSide)
           (net.minecraft.nbt CompoundNBT)
           (hellfirepvp.astralsorcery.common.lib RegistriesAS)
           (net.minecraft.tileentity TileEntity)
           (net.minecraft.item ItemStack)
           (net.minecraft.entity Entity)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)
           (hellfirepvp.astralsorcery.client.effect.source.orbital FXOrbitalCrystalAttunement)
           (hellfirepvp.astralsorcery.client.effect.source FXSourceOrbital)
           (hellfirepvp.astralsorcery.client.effect.function RefreshFunction)
           (java.util.function BiPredicate)
           (hellfirepvp.astralsorcery.client.effect EntityComplexFX))
  (:use eutros.starmetallic.Starmetallic
        eutros.starmetallic.lib.obfuscation
        eutros.clojurelib.lib.class-gen
        eutros.starmetallic.lib.sided))

(defn applicable-constellation?
  [^ItemEntity entity ^IConstellation cst]
  (when-let [stack (and (! entity (func_70089_S             ;; isAlive
                                    ))
                        (let [stack (! entity (func_92059_d ;; getItem
                                                ))]
                          (and (not (! stack (func_190926_b ;; isEmpty
                                               )))
                               stack)))]
    (let [item (! ^ItemStack stack (func_77973_b            ;; getItem
                                     ))]
      (-> (.getRegistryName item)
          (! (func_110624_b                                 ;; getNamespace
               ))
          (= MODID)))))

(defn ^ItemEntity find-tool
  [^TileAttunementAltar altar]
  (when-some [cst (.getActiveConstellation altar)]
    (let [box (-> (AxisAlignedBB. (!! altar (func_174877_v  ;; getPos
                                              )
                                            (func_177984_a  ;; up
                                              )))
                  (! (func_186662_g                         ;; grow
                       1)))
          vec (.add (Vector3. altar)
                    0.5 1.5 0.5)
          items (seq (!! altar
                       (func_145831_w                       ;; getWorld
                         )
                       (func_217357_a                       ;; getEntitiesWithingAABB
                         ItemEntity box)))]
      (when-not (empty? items)
        (let [closest (apply min-key
                             #(->> (! ^ItemEntity % (func_174791_d ;; getPositionVector
                                                      ))
                                   (.distanceSquared vec))
                             items)]
          (when (applicable-constellation? closest cst)
            closest))))))

(def TAG_CONSTELLATION "constellation")
(def TAG_ENTITY "entity")

(def DURATION 200)

(declare get-entity)

(defclass
  ActiveAttuneTool (:extends AttunementRecipe$Active)

  (:field ^int entity-id)
  (:field ^IConstellation constellation)

  (:field item-attune-sound)
  (:field inner-orbital)
  (:field flare)

  (:constructor [^AttunementRecipe recipe
                 ^IConstellation constellation
                 ^int entity-id]
    (super [^AttunementRecipe recipe])
    (set! (.constellation this) constellation)
    (set! (.entity-id this) entity-id))

  (:constructor [^AttunementRecipe recipe
                 ^CompoundNBT nbt]
    (super [^AttunementRecipe recipe])
    (.readFromNBT this nbt))

  (:method stopCrafting [^TileAttunementAltar altar])

  ;; Called on server when this recipe should create rewards
  (:method finishRecipe [^TileAttunementAltar altar]
    (when-some [entity ^ItemEntity (get-entity this altar)]
      (let [stack ^ItemStack
                  (! entity (func_92059_d                   ;; getItem
                              ))
            item (! stack (func_77973_b                     ;; getItem
                            ))
            cst-item ^ConstellationItem item
            cst (.getActiveConstellation altar)]
        (when (instance? IWeakConstellation cst)
          (.setAttunedConstellation cst-item stack cst))
        (when (instance? IMinorConstellation cst)
          (.setTraitConstellation cst-item stack cst)))))

  (:method doTick [^LogicalSide side
                   ^TileAttunementAltar altar]
    (when-some [entity ^ItemEntity (get-entity this altar)]
      (let [hover-pos (.add (Vector3. altar)
                            0.5 1.4 0.5)]
        (! entity (func_70107_b                             ;; setPosition
                    (.getX hover-pos)
                    (.getY hover-pos)
                    (.getZ hover-pos)))
        (set! (! entity field_70169_q                       ;; prevPosX
                 )
              (.getX hover-pos))
        (set! (! entity field_70167_r                       ;; prevPosY
                 )
              (.getY hover-pos))
        (set! (! entity field_70166_s                       ;; prevPosZ
                 )
              (.getZ hover-pos))
        (! entity (func_213293_j                            ;; setMotion
                    0 0 0))

        (when-client
          (when (.isClient side)
            (let [ticks (call-protected ^int getTick)
                  altar-pos (.subtract (.clone hover-pos)
                                       0 1.4 0)]

              (when (nil? (.inner-orbital this))
                (set! (.inner-orbital this)
                      (-> ^FXSourceOrbital
                          (EffectHelper/spawnSource (FXOrbitalCrystalAttunement. altar-pos
                                                                                 hover-pos
                                                                                 (.getActiveConstellation altar)))
                          (.setOrbitRadius 3)
                          (.setBranches 6)
                          (.setOrbitAxis Vector3$RotAxis/Y_AXIS)
                          (.setTicksPerRotation 300)
                          (.refresh (RefreshFunction/tileExistsAnd
                                      altar
                                      (reify BiPredicate
                                        (test [_ t _] (and (.canPlayConstellationActiveEffects ^TileAttunementAltar t)
                                                           (= (.getActiveRecipe altar) this)))))))))

              ))))))

  ;; Called every tick on server to test if this recipe is done. Create 'reward' and return true when finished.
  (:method ^boolean isFinished [^TileAttunementAltar altar]
    (>= (call-protected ^int getTick) DURATION))

  (:method ^boolean matches [^TileAttunementAltar altar]
    (and (call-super ^boolean matches ^TileAttunementAltar altar)
         (when-some [entity (get-entity this altar)]
           (applicable-constellation? entity
                                      (.getActiveConstellation altar)))
         (= (.constellation this)
            (.getActiveConstellation altar))))

  (:method writeToNBT [^CompoundNBT nbt]
    (call-super ^void writeToNBT ^CompoundNBT nbt)
    (! nbt (func_74778_a                                    ;; putString
             TAG_CONSTELLATION
             (-> this
                 .constellation
                 (.getRegistryName)
                 str)))
    (! nbt (func_74768_a                                    ;; putInt
             TAG_ENTITY
             (.entity-id this))))

  (:method readFromNBT [^CompoundNBT nbt]
    (call-super ^void readFromNBT ^CompoundNBT nbt)
    (set! (.constellation this)
          (.getValue RegistriesAS/REGISTRY_CONSTELLATIONS
                     (-> (! nbt (func_74779_i               ;; getString
                                  TAG_CONSTELLATION))
                         (ResourceLocation.))))
    (set! (.entity-id this)
          (! nbt (func_74762_e                              ;; getInt
                   TAG_ENTITY))))

  ;; client-only
  ;; Called on client to stop effects and such
  (:method stopEffects [^TileAttunementAltar altar]
    (when (.isFinished this altar)
      ;; TODO sound
      )
    (when-some [orbital ^EntityComplexFX (.inner-orbital this)]
      (.requestRemoval orbital))

    (when-some [flare ^EntityComplexFX (.flare this)]
      (.requestRemoval flare))))

(defn ^ItemEntity
  get-entity
  [^ActiveAttuneTool this ^TileEntity tile]
  (when-some [entity ^Entity (some-> tile
                                     (! (func_145831_w      ;; getWorld
                                          ))
                                     (! (func_73045_a       ;; getEntityById
                                          (.entity-id this))))]
    (when (and (! entity (func_70089_S                      ;; isAlive
                           ))
               (instance? ItemEntity entity))
      entity)))

(def recipe
  (proxy [AttunementRecipe] [(ResourceLocation. MODID "attune_tools")]
    (canStartCrafting [^TileAttunementAltar altar]
      (and (some-> (! altar (func_145831_w                  ;; getWorld
                              ))
                   (DayTimeHelper/isNight))
           (some? (find-tool altar))))
    (createRecipe [^TileAttunementAltar altar]
      (ActiveAttuneTool. recipe
                         (.getActiveConstellation altar)
                         (-> (find-tool altar)
                             (! (func_145782_y              ;; getEntityId
                                  )))))
    (deserialize [^TileAttunementAltar _ ^CompoundNBT cmp ^AttunementRecipe$Active _]
      (ActiveAttuneTool. recipe cmp))))

(.register AttunementCraftingRegistry/INSTANCE recipe)
