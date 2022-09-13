#_{:clj-kondo/ignore [:unused-referred-var]}
(ns kees.arena-graph.views
  (:require [kees.arena-graph.rf :as rf :refer [<sub <get >evt >assoc]]
            [kees.arena-graph.graphs :as graphs]
            [kees.arena-graph.api :as api]
            [kees.arena-graph.logic :as logic]))

(defn graph
  []
  [graphs/graph (<get :graph-data)])

(defn populate-graph
  []
  (let [id (<get :channel-id)
        request (fn []
                  (if id
                    (api/GET {:path ["channels" id]
                              :handler #(>evt [::rf/resp->nodes %])
                              :params {:page 1 :per 50}})
                    (js/console.error "Need an ID loaded!")))]
    [:span
     [:button {:on-click request
               :class (when-not id "disabled")} (char 0x03A9)]
     [:aside "Replace graph nodes with connected channels"]]))

(defn get-prop
  [{:keys [prop-key state-key desc char-val]}]
  (let [channel (<get :channel-slug)
        request (fn []
                  (api/GET {:path ["channels" channel "thumb"]
                            :handler #(>assoc state-key (prop-key %))}))]
    [:span
     [:button {:on-click request} (char char-val)]
     [:aside desc]]))

(defn color-picker 
  []
  (let [color (keyword (<get :active-color))]
    [:span
     [:button {:on-click #(>evt [::rf/add-node-colors color])}
      (char 0x03A5)]
     [:aside "Assign all the nodes random shades of"]
     (into
      [:select
       {:on-change #(>assoc :active-color (.. % -target -value))}]
      (for [k (keys (methods logic/hex))]
        [:option k]))]))

(defn control-panel
  []
  [:div
   [get-prop {:prop-key :id
              :state-key :channel-id
              :desc "Grab the ID of the channel"
              :char-val 0x03A8}]
   [populate-graph]
   [get-prop {:prop-key :length
              :state-key :connection-count
              :desc "Grab the connection count of the channel"
              :char-val 0x03A7}]
   [:span
    [:button {:on-click #(>evt [::rf/add-node-sizes])}
     (char 0x03A6)]
    [:aside "Add varying sizes to each node"]]
   [color-picker]])

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

(defn color-tones
  []
  (into
   [:div
    {:style {:display "grid"
             :grid-template-rows "repeat(8, 1fr)"
             :grid-template-columns "repeat(8, 1fr)"
             :gap 0}}]
   (for [n (range 64)
         :let [color (logic/hex :gold)]]
     [:div {:style {:width "3rem"
                    :height "3rem"
                    :background-color color
                    :font-size "0.4rem"}}
      n])))

(defn main []
  [:<>
   [:header
    [:h1 "Amoeba"]
    [:hr]]
   [:main
    [graph]
    [control-panel]
    [display-panel]
    [color-tones]]])
