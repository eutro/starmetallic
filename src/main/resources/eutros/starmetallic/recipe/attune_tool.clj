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
           (hellfirepvp.astralsorcery.common.lib RegistriesAS ColorsAS)
           (net.minecraft.tileentity TileEntity)
           (net.minecraft.item ItemStack)
           (net.minecraft.entity Entity)
           (hellfirepvp.astralsorcery.client.effect.handler EffectHelper)
           (hellfirepvp.astralsorcery.client.effect.source.orbital FXOrbitalCrystalAttunement)
           (hellfirepvp.astralsorcery.client.effect.source FXSourceOrbital)
           (hellfirepvp.astralsorcery.client.effect.function RefreshFunction VFXColorFunction)
           (java.util.function BiPredicate)
           (hellfirepvp.astralsorcery.client.effect EntityComplexFX))
  (:use eutros.starmetallic.Starmetallic
        eutros.starmetallic.lib.obfuscation
        eutros.starmetallic.lib.subclass
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
      (when (-> (.getRegistryName item)
                (! (func_110624_b                           ;; getNamespace
                     ))
                (= MODID))
        (or (and (nil? (.getAttunedConstellation ^ConstellationItem item
                                                 stack))
                 (instance? IWeakConstellation cst))
            (and (nil? (.getTraitConstellation ^ConstellationItem item
                                               stack))
                 (instance? IMinorConstellation cst)))))))

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

(defclass ActiveAttuneTool
  :prefix "attune-"
  :constructors [{:super [AttunementRecipe]
                  :args  [AttunementRecipe IConstellation Integer/TYPE]
                  :pre   'pass-first
                  :post  'direct-post}
                 {:super [AttunementRecipe]
                  :args  [AttunementRecipe CompoundNBT]
                  :pre   'pass-first
                  :post  'read!}]
  :super AttunementRecipe$Active
  :methods
  (let [methods [{:name  'stopCrafting
                  :alias 'stop-crafting
                  :types [TileAttunementAltar]}
                 {:name  'finishRecipe
                  :alias 'finish-recipe
                  :types [TileAttunementAltar]}
                 {:name  'doTick
                  :alias 'tick!
                  :types [LogicalSide TileAttunementAltar]}
                 {:name  'isFinished
                  :alias 'finished?
                  :types [TileAttunementAltar]}
                 {:name        'matches
                  :alias       'matches?
                  :super-alias 'matches
                  :types       [TileAttunementAltar]}
                 {:name        'writeToNBT
                  :super-alias 'writeToNBT
                  :alias       'write!
                  :types       [CompoundNBT]}
                 {:name        'readFromNBT
                  :super-alias 'readFromNBT
                  :alias       'read!
                  :types       [CompoundNBT]}]]
    (if-client
      (conj methods
            {:name  'stopEffects
             :alias 'stop-effects
             :types [TileAttunementAltar]})
      methods))
  :exposes-methods [{:name  'getTick
                     :alias 'p$getTick
                     :types []}]
  :fields [{:type Integer/TYPE
            :name 'entityId}
           {:type IConstellation
            :name 'constellation}

           {:type Object
            :name 'itemAttuneSound}
           {:type Object
            :name 'innerOrbital}
           {:type Object
            :name 'flare}])

(defn attune-pass-first
  ([recipe _]
   [recipe])
  ([recipe _ _]
   [recipe]))

(defn attune-direct-post
  [^ActiveAttuneTool this _ cst id]
  (set! (.constellation this) cst)
  (set! (.entityId this) id))

(def TAG_CONSTELLATION "constellation")
(def TAG_ENTITY "entity")

(defn attune-write!
  [^ActiveAttuneTool this ^CompoundNBT cmp]
  (.s$writeToNBT this cmp)

  (! cmp (func_74778_a                                      ;; putString
           TAG_CONSTELLATION
           (-> this
               .constellation
               (.getRegistryName)
               str)))
  (! cmp (func_74768_a                                      ;; putInt
           TAG_ENTITY
           (.entityId this))))

(defn attune-read!
  ([^ActiveAttuneTool this _ ^CompoundNBT cmp]
   (attune-read! this cmp))
  ([^ActiveAttuneTool this ^CompoundNBT cmp]
   (.s$readFromNBT this cmp)

   (set! (.constellation this)
         (.getValue RegistriesAS/REGISTRY_CONSTELLATIONS
                    (-> (! cmp (func_74779_i                ;; getString
                                 TAG_CONSTELLATION))
                        (ResourceLocation.))))
   (set! (.entityId this)
         (! cmp (func_74762_e                               ;; getInt
                  TAG_ENTITY)))))

;; Called on server when this recipe should stop (stop effects, world interactions, ...)
(defn attune-stop-crafting
  [^ActiveAttuneTool this ^TileAttunementAltar altar]
  ;; NOOP
  )

(defn ^ItemEntity
  get-entity
  [^ActiveAttuneTool this ^TileEntity tile]
  (when-some [entity ^Entity (some-> tile
                                     (! (func_145831_w      ;; getWorld
                                          ))
                                     (! (func_73045_a       ;; getEntityById
                                          (.entityId this))))]
    (when (and (! entity (func_70089_S                      ;; isAlive
                           ))
               (instance? ItemEntity entity))
      entity)))

(defn attune-matches?
  [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (and (.s$matches this altar)
       (when-some [entity (get-entity this altar)]
         (applicable-constellation? entity
                                    (.getActiveConstellation altar)))
       (= (.constellation this)
          (.getActiveConstellation altar))))

;; Called on server when this recipe should create rewards
(defn attune-finish-recipe
  [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (when-some [entity (get-entity this altar)]
    (let [stack ^ItemStack
                (! entity (func_92059_d                     ;; getItem
                            ))
          item (! stack (func_77973_b                       ;; getItem
                          ))
          cst-item ^ConstellationItem item
          cst (.getActiveConstellation altar)]
      (when (and (nil? (.getAttunedConstellation cst-item stack))
                 (instance? IWeakConstellation cst))
        (.setAttunedConstellation cst-item stack cst))
      (when (and (nil? (.getTraitConstellation cst-item stack))
                 (instance? IMinorConstellation cst))
        (.setTraitConstellation cst-item stack cst)))))

;; Called every tick for both sides
(defn attune-tick!
  [^ActiveAttuneTool this ^LogicalSide side ^TileAttunementAltar altar]
  (when-some [entity (get-entity this altar)]
    (let [hover-pos (.add (Vector3. altar)
                          0.5 1.4 0.5)]
      (! entity (func_70107_b                               ;; setPosition
                  (.getX hover-pos)
                  (.getY hover-pos)
                  (.getZ hover-pos)))
      (set! (! entity field_70169_q                         ;; prevPosX
               )
            (.getX hover-pos))
      (set! (! entity field_70167_r                         ;; prevPosY
               )
            (.getY hover-pos))
      (set! (! entity field_70166_s                         ;; prevPosZ
               )
            (.getZ hover-pos))
      (! entity (func_213293_j                              ;; setMotion
                  0 0 0))

      (when-client
        (when (.isClient side)
          (let [ticks (.p$getTick this)
                altar-pos (.subtract (.clone hover-pos)
                                     0 1.4 0)]

            (when (nil? (.innerOrbital this))
              (set! (.innerOrbital this)
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

(def DURATION 1000)

;; Called every tick on server to test if this recipe is done. Create 'reward' and return true when finished.
(defn attune-finished?
  [^ActiveAttuneTool this ^TileAttunementAltar _]
  (>= (.p$getTick this) DURATION))

(when-client
  ;; Called on client to stop effects and such
  (defn attune-stop-effects
    [^ActiveAttuneTool this ^TileAttunementAltar altar]
    (when (attune-finished? this altar)
      ;; TODO sound
      )
    (when-some [orbital ^EntityComplexFX (.innerOrbital this)]
      (.requestRemoval orbital))

    (when-some [flare ^EntityComplexFX (.flare this)]
      (.requestRemoval flare))))

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

(.register AttunementCraftingRegistry/INSTANCE
           recipe)
