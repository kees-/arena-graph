#_{:clj-kondo/ignore [:unused-referred-var]}
(ns kees.arena-graph.views
  (:require [kees.arena-graph.rf :as rf :refer [<sub <get >evt >assoc >GET]]
            [reagent.core :as r]
            [kees.arena-graph.graphs :as graphs]
            [kees.arena-graph.logic :as logic]
            [kees.arena-graph.rf.console :as console]))

(defn graph
  []
  [graphs/graph (<get :graph-data)])

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

(defn channel-changer
  []
  (let [slug (r/atom "")]
    (fn []
      [:span
       [:button {:on-click #(do
                              (>assoc :channel-slug (if (= "" @slug) nil @slug))
                              (>evt [::console/log :info "New channel:" @slug])
                              (>assoc :channel-id nil)
                              (>assoc :connection-count nil)
                              (reset! slug ""))}
        (char 0x03A9)]
       [:aside "Change the channel"]
       [:input {:type "text"
                :placeholder "slug"
                :on-change #(reset! slug (.. % -target -value))
                :value @slug}]])))

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
  [:section
   [channel-changer]
   [get-prop {:prop-key :id
              :state-key :channel-id
              :desc "Grab the ID of the channel"
              :char-val 0x03A8
              :needs :channel-slug}]
   #_[get-prop {:prop-key :length
              :state-key :connection-count
              :desc "Grab the connection count of the channel"
              :char-val 0x03A7
              :needs :channel-id}]
   [:span
    [:button {:on-click #(>evt [::rf/add-node-sizes 2 1.9])}
     (char 0x03A6)]
    [:aside "Add varying sizes to each node"]]
   [color-picker]
   [:span
    [:button {:on-click #(>evt [::rf/order-up])}
     (char 0x03A4)]
    [:aside [:i "ORDER UP!!!"]]]
   [:span
    [:button {:on-click #(>evt [::console/delayed-log :info 500 (random-uuid)])}
     (char 0x03A3)]
    [:aside "Log a message"]]])

(defn value-display
  [k]
  [:aside (<get k)])

(defn display-panel
  []
  [:section
   [:span [:h3 "Channel ID:"] [value-display :channel-id]]
   [:span [:h3 "Connection count:"] [value-display :connection-count]]])

(defn panels
  []
  [:div
   [control-panel]
   #_[display-panel]])

(defn main []
  [:<>
   [:header
    [:h1 "Amoe.ba"]
    [:hr]]
   [:main
    [graph]
    [console/element]
    [panels]]])
