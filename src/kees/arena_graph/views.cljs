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

(defn- canvas-cover
  []
  (let [covered? (<get :canvas-covered?)]
    [:div#canvas-cover
     {:style {:padding "0.25rem"}
      :class (when covered? "canvas-covered")}
     [:div
      [:img {:src "_asset/logo.png"
             :draggable false}]]]))

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
    [:div#controls-cover
     {:class (when initialized? "controls-cover-revealed")}]))

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
      [item "Channel:" title #_"usuuuuuuuuuuuuuuuuupeprepreprps instanslyey long channnnnnnnnnnnnnelll name this is soooo long 12 2 3 4 5 i hate this user" color]
      [item "Owner:" username color]
      [item "Connections:" length color]]]))

(defn- %-str
  [current total]
  (if (zero? total)
    "0%"
    (-> (/ current total 0.01)
        js/Math.round
        (str "%"))))

(defn progress-bar-filler
  [basis flex-direction]
  [:div.progress-bar-filler
   {:style {:flex-basis basis
            :flex-direction flex-direction}}])

(defn- progress-bar-1
  []
  (let [{:keys [channel-current channel-total]} (<get :progress)
        vertical? (<get :vertical-layout?)
        progress-%-str (%-str channel-current channel-total)]
    [:div.progress-bar
     {:style {:flex-direction (if vertical? "row-reverse" "column")
              (if vertical? :width :height) "90%"}}
     [progress-bar-filler
      progress-%-str
      (if vertical? "row" "column")]]))

(defn- progress-bar-2
  []
  (let [{:keys [current total]} (<get :progress)
        vertical? (<get :vertical-layout?)
        progress-%-str (%-str current total)]
    [:div.progress-bar
     {:style {:flex-direction (if vertical? "row" "column-reverse")
              (if vertical? :width :height) "90%"}}
     [progress-bar-filler
      progress-%-str
      (if vertical? "row-reverse" "column-reverse")]]))

(defn- progress-bar
  []
  (let [sep (fn [n] [:div.sep {:style {:flex-grow n}}])
        vertical? (<get :vertical-layout?)]
    [:div#progress-outer-container
     [:div#progress-background-lines
      {:style {:flex-direction (if vertical? "column" "row")}}
      [sep 27] [sep 46] [sep 27]]
     [:div#progress-bar-container
      {:style {:flex-direction (if vertical? "column" "row")}}
      [progress-bar-1]
      [progress-bar-2]]]))

(defn- loader
  []
  (let [active? (<get :active?)
        vertical? (<get :vertical-layout?)
        gif (str "url(_asset/gif/" (logic/which-gif) ")")]
    [:div#loader-container
     {:style {:max-width (if vertical? 100 125)}}
     [:div#loader-cover-left
      (when active?
        {:class "loader-cover-left-revealed"})]
     [:div#loader-cover-right
      (when active?
        {:class "loader-cover-right-revealed"})]
     [:div#loader
      {:style {:width (if vertical? 100 125)
               :height (if vertical? 100 125)
               :background-image (when active? gif)}}]]))

(defn- sidebar
  []
  (let [vertical? (<get :vertical-layout?)]
    [:div#sidebar
     {:style {:flex-direction (if vertical? "row" "column")
              :flex-grow (if vertical? "1" "0")}}
     [progress-bar]
     [loader]]))

(defn- under
  []
  (let [vertical? (<get :vertical-layout?)
        screen-width (<get :screen-width)]
    [:div#under
     {:style {:max-width (if vertical?
                           (js/Math.floor (* 0.92 screen-width))
                           629)}}
     [channel-info]
     [console/element]]))

(defn- nav
  []
  [:nav
   [:span>a.nav-button {:href "https://github.com/kees-/arena-graph"
                        :target "_blank"}
    [:div#github]]
   [:span>a.nav-button {:href "https://www.are.na/block/19423227"
                        :target "_blank"}
    [:div#arena]]
   [:span>button.nav-button
    {:on-click #(>evt [::rf/toggle-canvas-cover])}
    [:div#close]]])

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
    [:h1 (<sub [::rf/version])]
    [nav]]
   [:div#container
    [sidebar]
    [:div#canvas-container
     [canvas-cover]
     [:div#canvas
      [graph]]]]
   [under]
   [:div#input-container
    [controls-cover]
    [input]]
   #_[palette]])
