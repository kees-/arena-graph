(ns kees.arena-graph.graphs
  (:require ["react-force-graph-2d" :as ForceGraph2D]
            [kees.arena-graph.rf :as rf :refer [<sub >evt]]))

#_{:clj-kondo/ignore [:unused-binding]}
(defn element
  [data]
  (let [{:keys [width height]} (<sub [::rf/graph-dimensions])]
    (fn [data]
      [:> ForceGraph2D
       {:graphData data
        :width width
        :height height
        :minZoom 0.2
        :maxZoom 7.5
        :d3VelocityDecay 0.5
        :nodeVal :size
        #_#_:nodeLabel :title
        :nodeColor :color
        :onNodeClick #(>evt [::rf/graph-node->visit %1])
        :onNodeHover #(>evt [::rf/hover-node %1])
        :onNodeDrag #(>evt [::rf/hover-node %1])
        :onNodeDragEnd #(>evt [::rf/hover-node nil])
        :linkWidth 1}])))
