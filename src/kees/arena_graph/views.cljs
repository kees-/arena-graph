#_{:clj-kondo/ignore [:unused-referred-var]}
(ns kees.arena-graph.views
  (:require [kees.arena-graph.rf :as rf :refer [<sub <get >evt >assoc >GET]]
            [kees.arena-graph.graphs :as graphs]
            [kees.arena-graph.logic :as logic]))

(defn graph
  []
  [graphs/graph (<get :graph-data)])

(defn populate-graph
  []
  (let [id (<get :channel-id)
        request #(when id
                   (>evt [::rf/GET {:path ["channels" id]
                                    :on-success ::rf/resp->nodes
                                    :params {:page 1 :per 50}}]))]
    [:span
     [:button {:on-click request
               :class (when-not id "disabled")} (char 0x03A9)]
     [:aside "Replace graph nodes with connected channels"]]))

(defn get-prop
  [{:keys [prop-key state-key desc char-val needs]}]
  (let [active? (<get needs)
        channel (<get :channel-slug)
        request #(when active?
                   (>GET {:path ["channels" channel "thumb"]
                          :on-success [::rf/assoc-prop state-key prop-key]}))]
    [:span
     [:button {:on-click request
               :class (when-not active? "disabled")}
      (char char-val)]
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
              :char-val 0x03A8
              :needs :channel-slug}]
   [populate-graph]
   [get-prop {:prop-key :length
              :state-key :connection-count
              :desc "Grab the connection count of the channel"
              :char-val 0x03A7
              :needs :channel-id}]
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

(defn main []
  [:<>
   [:header
    [:h1 "Amoeba"]
    [:hr]]
   [:main
    [graph]
    [control-panel]
    [display-panel]]])
