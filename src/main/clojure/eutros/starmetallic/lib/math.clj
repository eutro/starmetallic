(ns eutros.starmetallic.lib.math
  (:import net.minecraft.util.math.MathHelper))

(defn deg->rad [deg]
  (* (/ deg 180)
     Math/PI))

(defn sin [theta]
  (MathHelper/sin theta))

(defn cos [theta]
  (MathHelper/cos theta))

(defn sin-deg [angle]
  (sin (deg->rad angle)))

(defn cos-deg [angle]
  (cos (deg->rad angle)))
