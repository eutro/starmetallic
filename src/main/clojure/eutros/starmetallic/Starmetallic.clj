(ns eutros.starmetallic.Starmetallic
  (:import java.io.InputStreamReader
           java.util.function.Supplier
           net.minecraftforge.fml.common.Mod
           net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
           net.minecraft.item.IItemTier
           net.minecraft.item.crafting.Ingredient
           (net.minecraftforge.registries DeferredRegister
                                          ForgeRegistries)
           org.apache.logging.log4j.LogManager))

(set! *warn-on-reflection* true)

(gen-class
 :name      ^{Mod "starmetallic"}
            eutros.starmetallic.Starmetallic
 :main      false
 :post-init "post-init")

(def MODID "starmetallic")

(when-not
  *compile-files*

  (def ITEMS
    (DeferredRegister/create ForgeRegistries/ITEMS
                             ^String MODID))

  (def ENTITIES
    (DeferredRegister/create ForgeRegistries/ENTITIES
                             ^String MODID))

  (defn register [^DeferredRegister registry path]
    (.register registry
               (name path)
               (reify
                Supplier
                (get [_]
                     (with-open [iostream (.. (Thread/currentThread)
                                              (getContextClassLoader)
                                              (getResourceAsStream (str "/eutros/starmetallic/" path ".clj")))]
                       (-> (InputStreamReader. iostream)
                           (Compiler/load (str "eutros/starmetallic/" path ".clj")
                                          (-> (name path)
                                              (str ".clj")))))))))

  (def LOGGER (LogManager/getLogger "Starmetallic"))

  (def tool-tier
    (reify
     IItemTier
     (getMaxUses [_] 100)
     (getEfficiency [_] 7.)
     (getAttackDamage [_] 6.)
     (getHarvestLevel [_] 4)
     (getEnchantability [_] 40)
     (getRepairMaterial [_] Ingredient/EMPTY)))

  (register ITEMS 'item/starmetal_sword)
  (register ITEMS 'item/starmetal_axe)
  (register ITEMS 'item/starmetal_pickaxe)

  (register ENTITIES 'entity/starlight_burst))

(defn -post-init [this]
  (let [mod-bus (-> (FMLJavaModLoadingContext/get)
                    (.getModEventBus))]
    (.register ^DeferredRegister ITEMS mod-bus)
    (.register ^DeferredRegister ENTITIES mod-bus)))
