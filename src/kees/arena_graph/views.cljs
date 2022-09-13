#_{:clj-kondo/ignore [:unused-referred-var]}
(ns kees.arena-graph.views
  (:require [kees.arena-graph.rf :as rf :refer [<sub <get >evt >assoc]]
            [kees.arena-graph.graphs :as graphs]
            [kees.arena-graph.api :as api]))

(defn graph
  []
  [graphs/graph (<get :graph-data)])

(defn populate-graph
  []
  (let [channel (<get :channel-slug)
        request (fn []
                 (api/GET {:path (str "channels/" channel)
                           :handler #(>evt [::rf/resp->nodes %])
                           :params {:page 1 :per 50}}))]
    [:span
     [:button {:on-click request} (char 0x03A9)]
     [:aside "Replace graph nodes with connected channels"]]))

(defn get-prop
  [{:keys [prop-key desc char-val]}]
  (let [channel (<get :channel-slug)
        request (fn []
                  (api/GET {:path (str "channels/" channel "/thumb")
                            :handler #(>assoc :channel-id (prop-key %))}))]
    [:span
     [:button {:on-click request} (char char-val)]
     [:aside desc]]))

(defn main []
  [:<>
   [:header
    [:h1 "Amoeba"]
    [:hr]]
   [:main
    [graph]
    [:div
     [populate-graph]
     [get-prop {:prop-key :id
                :desc "Grab the ID of the channel"
                :char-val 0x03A8}]
     [get-prop {:prop-key :length
                :desc "Grab the connection count of the channel"
                :char-val 0x03A7}]
     [:span
      [:button {:on-click #(>evt [::rf/add-node-sizes])}
       (char 0x03A6)]
      [:aside "Add varying sizes to each node"]]]]])
