(ns kees.arena-graph.logic)

(defn gen-hex
  "Supply three 2-tuples of [base variation] where base + variation <= 256"
  [& variance-tuples]
  (let [hex (fn [[n v]] (.toString (+ n (rand-int v)) 16))]
    (->> variance-tuples
         (map hex)
         (into ["#"])
         (reduce str))))

(defmulti hex identity)
(defmethod hex :gold [_] (gen-hex [220 20] [220 35] [50 20]))
(defmethod hex :pink [_] (gen-hex [200 20] [180 35] [200 20]))
(defmethod hex :grey [_] (gen-hex [120 30] [190 30] [170 30]))

(defn hex-map
  "Returns infinite maps supplying a random color in specified hue range"
  [color]
  (repeatedly #(hash-map :color (hex color))))

(defn size-variance
  "Returns infinite maps with :size to float within v of n"
  [n v]
  (repeatedly #(hash-map :size (+ n (- v) (* 2 v (rand))))))
