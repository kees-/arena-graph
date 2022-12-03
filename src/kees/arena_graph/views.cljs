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
          {:on-click confirm
           :style {:font-size "2rem"}}
          (char 0x2192)])
       (when (and (not (<get :working?))
                  (= "" @value)
                  (<get :thumb :id))
         [:button
          {:on-click #(>evt [::rf/order-up])}
          "create"])])))

(defn- controls-cover
  []
  (let [initialized? (<get :initialized?)]
    (if initialized?
      [:div#controls-cover.controls-cover-revealed]
      [:div#controls-cover])))

(defn- item
  [label value color]
  [:div.item
   {:style {:color color}}
   [:div label]
   [:div.value value]])

(defn- channel-info
  []
  (let [node (<get :hovered-node)
        {:keys [title color length]
         {:keys [username]} :user} node]
    [:div#channel-info-container
     [:div#channel-info
      {:style {:border (str "0.45rem solid" color)}}
      [item "Channel:" title color]
      [item "Owner:" username color]
      [item "Connections:" length color]]]))

(defn- %-str
  [current total]
  (if (zero? total)
    "0%"
    (-> (/ current total 0.01)
        js/Math.round
        (str "%"))))

(defn- progress-bar-outer
  []
  (let [{:keys [channel-current channel-total]} (<get :progress)
        progress-%-str (%-str channel-current channel-total)]
    [:div.progress-bar
     {:style {:flex-direction "column"}}
     [:div.progress-bar-filler
      {:style {:flex-basis progress-%-str}}]]))

(defn- progress-bar-inner
  []
  (let [{:keys [current total]} (<get :progress)
        progress-%-str (%-str current total)]
    [:div.progress-bar
     {:style {:flex-direction "column-reverse"}}
     [:div.progress-bar-filler
      {:style {:flex-basis progress-%-str}}]]))

(defn- progress-bar
  []
  (let [sep (fn [n] [:div.sep {:style {:flex-grow n}}])]
    [:div#progress-outer-container
     [:div#progress-background-lines
      [sep 27] [sep 46] [sep 27]]
     [:div#progress-bar-container
      [progress-bar-outer]
      [progress-bar-inner]]]))

(defn- loader
  []
  (let [active? (<get :active?)
        gif (str "url(_asset/gif/" (logic/which-gif) ")")]
    [:div#loader-container
     [:div#loader-cover-left
      (when active?
        {:class "loader-cover-left-revealed"})]
     [:div#loader-cover-right
      (when active?
        {:class "loader-cover-right-revealed"})]
     [:div#loader
      (when active?
        {:style {:background-image gif}})]]))

#_{:clj-kondo/ignore [:unused-private-var]}
#_
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
    [:div#sidebar
     [progress-bar]
     [loader]]
    [:div#canvas
     [graph]]]
   [:div#under
    [channel-info]
    [console/element]]
   [:div#input-container
    [controls-cover]
    [input]]
   #_[palette]])
