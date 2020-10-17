(ns eutros.starmetallic.core
  (:require [eutros.starmetallic.compilerhack.clinitfilter]
            [eutros.starmetallic.reference :as rf]
            [eutros.starmetallic.item.core :as items])
  (:import (java.io InputStreamReader)
           (java.util.function Supplier)
           (net.minecraftforge.fml.common Mod)
           (net.minecraftforge.fml.javafmlmod FMLJavaModLoadingContext)
           (net.minecraftforge.registries DeferredRegister
                                          ForgeRegistries)
           org.apache.logging.log4j.LogManager))

(set! *warn-on-reflection* true)

(gen-class
  :name ^{Mod "starmetallic"}
  eutros.starmetallic.Starmetallic
  :main false
  :post-init "post-init")

(when-not
  *compile-files*

  (def BLOCKS
    (DeferredRegister/create ForgeRegistries/BLOCKS
                             ^String rf/MODID))

  (def ENTITIES
    (DeferredRegister/create ForgeRegistries/ENTITIES
                             ^String rf/MODID))

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

  (load "packets")
  (load "recipe/attune_tool"))

(defn -post-init [_]
  (let [mod-bus (-> (FMLJavaModLoadingContext/get)
                    (.getModEventBus))]
    (items/listen mod-bus)
    (.register ^DeferredRegister ENTITIES mod-bus)
    (.register ^DeferredRegister BLOCKS mod-bus)))
