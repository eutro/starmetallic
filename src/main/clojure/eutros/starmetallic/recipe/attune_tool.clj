(ns eutros.starmetallic.recipe.attune-tool
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.reference :as rf]
            [eutros.starmetallic.lib.functions :refer [bipredicate]])
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
           (hellfirepvp.astralsorcery.client.effect EntityComplexFX)))

(defn applicable-constellation?
  [^ItemEntity entity ^IConstellation cst]
  (when-let [stack (and (.isAlive entity)
                        (let [stack (.getItem entity)]
                          (and (not (.isEmpty stack))
                               stack)))]
    (-> ^ItemStack stack
        .getItem
        .getRegistryName
        .getNamespace
        (= rf/MODID))))

(defn ^ItemEntity find-tool
  [^TileAttunementAltar altar]
  (when-some [cst (.getActiveConstellation altar)]
    (let [box (-> altar .getPos .up AxisAlignedBB. (.grow 1))
          items (-> altar .getWorld (.getEntitiesWithinAABB ItemEntity box))]
      (when-not (empty? items)
        (let [closest (apply min-key
                             #(->> (.getPositionVector ^ItemEntity %)
                                   (.distanceSquared (.add (Vector3. altar)
                                                           0.5 1.5 0.5)))
                             items)]
          (when (applicable-constellation? closest cst)
            closest))))))

(def TAG_CONSTELLATION "constellation")
(def TAG_ENTITY "entity")

(def DURATION 200)

(declare get-entity)

(gen-class
  :name eutros.starmetallic.recipe.attune_tool.ActiveAttuneTool
  :extends hellfirepvp.astralsorcery.common.crafting.nojson.attunement.AttunementRecipe$Active
  :constructors {[hellfirepvp.astralsorcery.common.crafting.nojson.attunement.AttunementRecipe
                  hellfirepvp.astralsorcery.common.constellation.IConstellation
                  int]
                 [hellfirepvp.astralsorcery.common.crafting.nojson.attunement.AttunementRecipe]

                 [hellfirepvp.astralsorcery.common.crafting.nojson.attunement.AttunementRecipe
                  net.minecraft.nbt.CompoundNBT]
                 [hellfirepvp.astralsorcery.common.crafting.nojson.attunement.AttunementRecipe]}
  :state state
  :prefix aat-
  :init init
  :post-init post-init
  :exposes-methods {getTick     superGetTick
                    matches     superMatches
                    writeToNBT  superWriteToNBT
                    readFromNBT superReadFromNBT})

(import eutros.starmetallic.recipe.attune_tool.ActiveAttuneTool)

(defn aat-init
  ([recipe constellation entity-id]
   [[recipe] (atom {:entity-id     entity-id
                    :constellation constellation})])
  ([recipe nbt]
   [[recipe] (atom {})]))

(declare aat-readFromNBT)

(defn aat-post-init
  ([this recipe constellation entity])
  ([this recipe nbt]
   (aat-readFromNBT this nbt)))

(defn aat-stopCrafting [^ActiveAttuneTool this ^TileAttunementAltar altar])

;; Called on server when this recipe should create rewards
(defn aat-finishRecipe [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (when-some [entity ^ItemEntity (get-entity this altar)]
    (let [stack (.getItem entity)
          item ^ConstellationItem (.getItem stack)
          cst (.getActiveConstellation altar)]
      (when (instance? IWeakConstellation cst)
        (.setAttunedConstellation item stack cst))
      (when (instance? IMinorConstellation cst)
        (.setTraitConstellation item stack cst)))))

(defn aat-doTick [^ActiveAttuneTool this
                  ^LogicalSide side
                  ^TileAttunementAltar altar]
  (when-some [entity ^ItemEntity (get-entity this altar)]
    (let [hover-pos (.add (Vector3. altar)
                          0.5 1.4 0.5)]
      (.setPosition entity
                    (.getX hover-pos)
                    (.getY hover-pos)
                    (.getZ hover-pos))
      (set! (.-prevPosX entity) (.getX hover-pos))
      (set! (.-prevPosY entity) (.getY hover-pos))
      (set! (.-prevPosZ entity) (.getZ hover-pos))
      (.setMotion entity 0 0 0)

      (when (.isClient side)
        (let [ticks (.superGetTick this)
              altar-pos (.subtract (.clone hover-pos)
                                   0 1.4 0)]

          (when (nil? (:inner-orbital @(.state this)))
            (swap! (.state this)
                   assoc :inner-orbital
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
                                   (bipredicate [t _]
                                     (and (.canPlayConstellationActiveEffects ^TileAttunementAltar t)
                                          (= (.getActiveRecipe altar) this))))))))

          )))))

;; Called every tick on server to test if this recipe is done. Create 'reward' and return true when finished.
(defn aat-isFinished [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (>= (.superGetTick this) DURATION))

(defn aat-matches [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (and (.superMatches this ^TileAttunementAltar altar)
       (let [active-constellation
             (.getActiveConstellation altar)]
         (some-> (get-entity this altar) (applicable-constellation? active-constellation))
         (-> this .state deref :constellation
             (= active-constellation)))))

(defn aat-writeToNBT [^ActiveAttuneTool this ^CompoundNBT nbt]
  (.superWriteToNBT this nbt)
  (let [{:keys [constellation entity-id]}
        @(.state this)]
    (.putString nbt
                TAG_CONSTELLATION
                (-> ^IConstellation
                    constellation
                    .getRegistryName
                    str))
    (.putInt nbt TAG_ENTITY entity-id)))

(defn aat-readFromNBT [^ActiveAttuneTool this ^CompoundNBT nbt]
  (.superReadFromNBT this nbt)
  (swap! (.state this) assoc
         :constellation
         (.getValue RegistriesAS/REGISTRY_CONSTELLATIONS
                    (-> (.getString nbt
                                    TAG_CONSTELLATION)
                        (ResourceLocation.)))
         :entity-id
         (.getInt nbt TAG_ENTITY)))

;; client-only
;; Called on client to stop effects and such
(defn aat-stopEffects [^ActiveAttuneTool this ^TileAttunementAltar altar]
  (when (.isFinished this altar)
    ;; TODO sound
    )
  (let [{:keys [orbital flare]}
        @(.state this)]
    (some-> orbital .requestRemoval)
    (some-> flare .requestRemoval)))

(defn ^ItemEntity
  get-entity
  [^ActiveAttuneTool this ^TileEntity tile]
  (let [entity (some-> tile .getWorld (.getEntityByID (-> this .state deref :entity-id)))]
    (when (and entity
               (.isAlive ^Entity entity)
               (instance? ItemEntity entity))
      entity)))

(def recipe
  (proxy [AttunementRecipe] [(ResourceLocation. rf/MODID "attune_tools")]
    (canStartCrafting [^TileAttunementAltar altar]
      (boolean
        (and (some-> altar .getWorld DayTimeHelper/isNight)
             (find-tool altar))))
    (createRecipe [^TileAttunementAltar altar]
      (ActiveAttuneTool. recipe
                         (.getActiveConstellation altar)
                         (-> (find-tool altar)
                             .getEntityId)))
    (deserialize [^TileAttunementAltar _ ^CompoundNBT cmp ^AttunementRecipe$Active _]
      (ActiveAttuneTool. recipe cmp))))

#_`[aat-init aat-post-init
    aat-stopCrafting
    aat-finishRecipe
    aat-doTick
    aat-isFinished
    aat-matches
    aat-writeToNBT
    aat-readFromNBT
    aat-stopEffects]

(.register AttunementCraftingRegistry/INSTANCE recipe)
