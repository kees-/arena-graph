(ns kees.arena-graph.graphs
  (:require ["react-force-graph-2d" :as ForceGraph2D]))

(defn graph
  [data]
  [:> ForceGraph2D
   {:graphData data
    :width 500
    :height 400
    :minZoom 0.75
    :maxZoom 5
    :nodeLabel :title
    :nodeColor :color}])
