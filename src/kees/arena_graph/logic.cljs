(ns kees.arena-graph.logic
  "Miscellaneous helper functions"
  (:require [clojure.string :as s]))

(def ^:private ui-var
  (-> js/document.body
      js/window.getComputedStyle
      (.getPropertyValue "--ui")
      s/trim))

(defn- gen-hex
  "Supply three 2-tuples of [base variation] where base + variation <= 256"
  [& variance-tuples]
  (let [hex (fn [[n v]] (.toString (+ n (rand-int v)) 16))
        pad (fn [n] (str (when (= 1 (count n)) "0") n))]
    (->> variance-tuples
         (map (comp pad hex))
         (into ["#"])
         (reduce str))))

(defmulti hex
  "Returns a hex color string"
  identity)
(defmethod hex :default [_] "#FFFFFF")

;; In use
(defmethod hex :static-var-ui [_] ui-var)
(defmethod hex :gold-light [_] (gen-hex [0xFC 0] [0xD4 20] [0x48 40]))
(defmethod hex :aqua [_] (gen-hex [0x8C 30] [0xD2 30] [0xBE 30]))

;; Misc
(defmethod hex :gold [_] (gen-hex [0xDE 20] [0xCE 34] [0x32 20]))
(defmethod hex :acid [_] (gen-hex [0x8B 70] [0xF2 14] [0 20]))
(defmethod hex :green-murky [_] (gen-hex [0xAB 30] [0xC3 20] [0 10]))
(defmethod hex :lavender [_] (gen-hex [0xC8 20] [0xB4 35] [0xC8 25]))

(defn hex-map
  "Returns a hash-map supplying a random color in specified hue range"
  [color-key]
  {:color (hex color-key)})

(defn size-variant
  "Return a hash-map of :size to a float within [b,b+v)"
  [base variant]
  {:size (+ base (* variant (rand)))})

;; Vector of 2-tuples [a b] where a is chance out of 1 that gif b is chosen.
;; Sum of numbers must be < 1
(def ^:private gif-chances
  (sort
   [[0.02 "takase.gif"]
    [0.05 "sparkle.gif"]
    [0.1 "scroll.gif"]
    [0.1 "ripples.gif"]
    [0.25 "planet.gif"]]))

;; 1 - the sum of gif-chances is the probability of the default gif playing.
(def ^:private default-gif
  "tan.gif")

(defn which-gif
  []
  (let [n (rand)]
    (loop [chances gif-chances
           acc 0]
      (let [[[probability filename] & remaining] chances
            acc (+ acc probability)]
        (cond
          (empty? chances) default-gif
          (< n acc) filename
          :else (recur remaining acc))))))

(defn into-by-key
  "Combines two vectors of maps, skipping repeated values of key k"
  [v1 v2 k]
  (let [key-compare (fn [acc el]
                      (if (some #{(k el)} (map k acc))
                        acc
                        (conj acc el)))]
    (reduce key-compare v1 v2)))
