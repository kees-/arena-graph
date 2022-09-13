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
  [{:keys [prop-key state-key desc char-val]}]
  (let [channel (<get :channel-slug)
        request (fn []
                  (api/GET {:path (str "channels/" channel "/thumb")
                            :handler #(>assoc state-key (prop-key %))}))]
    [:span
     [:button {:on-click request} (char char-val)]
     [:aside desc]]))

(defn control-panel
  []
  [:div
   [populate-graph]
   [get-prop {:prop-key :id
              :state-key :channel-id
              :desc "Grab the ID of the channel"
              :char-val 0x03A8}]
   [get-prop {:prop-key :length
              :state-key :connection-count
              :desc "Grab the connection count of the channel"
              :char-val 0x03A7}]
   [:span
    [:button {:on-click #(>evt [::rf/add-node-sizes])} (char 0x03A6)]
    [:aside "Add varying sizes to each node"]]])

(defn value-display
  [k]
  [:aside (<get k)])

(defn display-panel
  []
  [:div
   [:span
    [:h3 "Channel slug:"]
    [value-display :channel-slug]]
   [:span
    [:h3 "Channel ID:"]
    [value-display :channel-id]]
   [:span
    [:h3 "Connection count:"]
    [value-display :connection-count]]])

(defn main []
  [:<>
   [:header
    [:h1 "Amoeba"]
    [:hr]]
   [:main
    [graph]
    [control-panel]
    [display-panel]]])
