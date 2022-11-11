#_{:clj-kondo/ignore [:unused-referred-var]}
(ns kees.arena-graph.views
  (:require [reagent.core :as r]
            [kees.arena-graph.graphs :as graphs]
            [kees.arena-graph.rf.console :as console]
            [kees.arena-graph.rf :as rf :refer [<sub <get >evt >assoc >GET]]
            [kees.arena-graph.logic :as logic]))

(defn- graph
  "The container for the generated force graph (initially blank)"
  []
  [graphs/element (<get :graph-data)])

(defn- input
  "Panel of text input and buttons"
  []
  (let [value (r/atom "")
        confirm (fn []
                  (>evt [::rf/select-channel @value])
                  (reset! value ""))
        handle-change (fn [e]
                        (reset! value (.. e -target -value)))
        handle-keypress (fn [e]
                          (when (= (.-code e) "Enter")
                            (confirm)))]
    (fn []
      [:div#controls>span
       [:input
        {:on-change handle-change
         :on-key-press handle-keypress
         :value @value}]
       (when (not= "" @value)
         [:button
          {:on-click confirm}
          (char 0x2192)])
       (when (and (not (<get :working))
                  (= "" @value)
                  (<get :thumb :id))
         [:button
          {:on-click #(>evt [::rf/order-up])}
          "create"])])))

(defn- item
  [label value]
  [:div.item
   [:div label]
   [:div.value
    {:style {:color (logic/hex :aqua)}}
    value]])

(defn- channel-info
  []
  (let [{:keys [title]
         {:keys [username]} :user} (<get :hovered-node)]
    [:div#channel-info
     [item "Channel:" title]
     [item "Owner:" username]]))

(defn- sidebar
  []
  (let [active (<get :active)
        gif (str "url(../_asset/gif/"
                 (logic/which-gif)
                 ")")]
    [:div#sidebar
     [:div#loader
      (when active
        {:style {:background-image gif}})]]))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- palette
  "Generates an 8x8 color palette to test the acceptable color ranges"
  []
  (let [palette-color (<get :palette-color)
        element [:div
                 {:style {:display "grid"
                          :width "fit-content"
                          :margin "1rem 0"
                          :padding "0.5rem"
                          :border "4px solid var(--ui)"
                          :border-radius "1rem"
                          :grid-template-columns "repeat(8, 1fr)"
                          :grid-template-rows "repeat(8, 1fr)"
                          :justify-content "center"
                          :justify-items "center"
                          :gap "0.4rem"}}]
        squares (for [_ (range 64)
                      :let [color (logic/hex palette-color)]]
                  [:div.swatch
                   {:style {:background color
                            :width "3rem"
                            :height "3rem"
                            :border-radius "0.5rem"}}])
        color-picker (fn [color]
                       (>assoc :palette-color color))
        panel (into
               [:div
                {:style {:display "flex"
                         :flex-flow "column nowrap"
                         :justify-content "space-around"}}]
               (for [color (keys (methods logic/hex))
                     :let [color-str (name color)]]
                 [:button
                  {:key color
                   :on-click (partial color-picker color)
                   :style {:cursor "pointer"}}
                  color-str]))]
    [:div
     {:style {:display "flex"
              :flex-flow "row nowrap"
              :justify-content "center"
              :gap "1rem"}}
     (into element squares)
     panel]))

(defn main []
  [:main
   [:header
    [:h1 "amoeba-2"]]
   [:div#container
    #_[info]
    [sidebar]
    [:div#canvas
     [graph]]]
   [:div#under
    [console/element]
    [channel-info]]
   [input]
   #_[palette]])
