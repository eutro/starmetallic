(ns eutros.starmetallic.Starmetallic
  (:import net.minecraftforge.fml.common.Mod
           net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
           java.io.InputStreamReader
           [org.apache.logging.log4j
            Logger
            LogManager]
           [net.minecraftforge.registries
            DeferredRegister
            ForgeRegistries]
           net.minecraft.item.crafting.Ingredient
           java.util.function.Supplier
           [net.minecraft.item
            Item
            SwordItem
            Item$Properties
            IItemTier]))

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

  (defn register-item [name]
    (.register ^DeferredRegister ITEMS
               (str "starmetal_" name)
               (reify
                Supplier
                (get [_]
                     (with-open [iostream (.. (Thread/currentThread)
                                              (getContextClassLoader)
                                              (getResourceAsStream (str "/eutros/starmetallic/item/" name ".clj")))]
                       (-> (InputStreamReader. iostream)
                           (load-reader)))))))

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

  (register-item "sword")
  (register-item "axe")
  (register-item "pickaxe"))

(defn -post-init [this]
  (let [mod-bus (-> (FMLJavaModLoadingContext/get)
                    (.getModEventBus))]
    (.register ^DeferredRegister ITEMS mod-bus)))
