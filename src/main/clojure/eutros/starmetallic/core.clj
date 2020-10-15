(ns eutros.starmetallic.core
  (:require [eutros.starmetallic.compilerhack.clinitfilter])
  (:import java.io.InputStreamReader
           java.util.function.Supplier
           net.minecraftforge.fml.common.Mod
           net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
           (net.minecraftforge.registries DeferredRegister
                                          ForgeRegistries)
           org.apache.logging.log4j.LogManager))

(set! *warn-on-reflection* true)

(gen-class
  :name ^{Mod "starmetallic"}
  eutros.starmetallic.Starmetallic
  :main false
  :post-init "post-init")

(def MODID "starmetallic")

(when-not
  *compile-files*

  (def ITEMS
    (DeferredRegister/create ForgeRegistries/ITEMS
                             ^String MODID))

  (def BLOCKS
    (DeferredRegister/create ForgeRegistries/BLOCKS
                             ^String MODID))

  (def ENTITIES
    (DeferredRegister/create ForgeRegistries/ENTITIES
                             ^String MODID))

  (defn register [^DeferredRegister registry path]
    (.register registry
               (name path)
               (reify Supplier
                 (get [_]
                   (with-open [iostream (-> (Thread/currentThread)
                                            (.getContextClassLoader)
                                            (.getResourceAsStream (str "/eutros/starmetallic/" path ".clj")))]
                     (-> (InputStreamReader. iostream)
                         (Compiler/load (str "eutros/starmetallic/" path ".clj")
                                        (-> (name path)
                                            (str ".clj")))))))))

  (def LOGGER (LogManager/getLogger "Starmetallic"))

  (register BLOCKS 'block/light_source)

  (register ENTITIES 'entity/starlight_burst)

  (register ITEMS 'item/starmetal_sword)
  (register ITEMS 'item/starmetal_pickaxe)
  (register ITEMS 'item/starmetal_axe)
  (register ITEMS 'item/starmetal_hoe)

  (load "packets")
  (load "recipe/attune_tool"))

(defn -post-init [_]
  (let [mod-bus (-> (FMLJavaModLoadingContext/get)
                    (.getModEventBus))]
    (.register ^DeferredRegister ITEMS mod-bus)
    (.register ^DeferredRegister ENTITIES mod-bus)
    (.register ^DeferredRegister BLOCKS mod-bus)))
