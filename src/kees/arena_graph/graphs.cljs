(ns kees.arena-graph.graphs
  (:require ["react-force-graph-2d" :as ForceGraph2D]
            [kees.arena-graph.rf :as rf :refer [>evt]]))

(defn graph
  [data]
  [:> ForceGraph2D
   {:graphData data
    :width 500
    :height 400
    :minZoom 0.75
    :maxZoom 5
    :d3VelocityDecay 0.6
    :nodeVal :size
    :nodeLabel :title
    :nodeColor :color
    :onNodeClick #(>evt [::rf/visit-node-channel %1])}])
