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
       [:button
        {:class (when (= "" @slug) "disabled")
         :on-click #(do
                      (>evt [::rf/select-channel @slug])
                      (reset! slug ""))}
        (char 0x03A9)]
       [:aside "Change the channel"]
       [:input {:type "text"
                :placeholder "url, slug, or id"
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
  [:div>section
   [channel-changer]
   #_[:span
    [:button {:on-click #(>evt [::rf/add-node-sizes 2 1.9])}
     (char 0x03A6)]
    [:aside "Add varying sizes to each node"]]
   #_[color-picker]
   [:span
    [:button {:on-click #(>evt [::rf/order-up])}
     (char 0x03A4)]
    [:aside "Create the graph"]]
   [:span
    [:button {:on-click #(>evt [::console/delayed-log :info 500 (random-uuid)])}
     (char 0x03A3)]
    [:aside "Log a message"]]])

(defn main []
  [:<>
   [:header
    [:h1 "Amoe.ba"]
    [:hr]]
   [:main
    [graph]
    [console/element]
    [control-panel]]])
