(ns kees.arena-graph.rf.console
  (:require [re-frame.core :as re-frame :refer [reg-event-fx reg-fx reg-sub]]))

;; ========== DOM HICCUP =======================================================
(defn- timestamp
  []
  (let [now (.toLocaleTimeString (js/Date.))]
    (re-find #"[^\s]+" now)))

(defn- li
  [type message]
  [(type {:info :li.info
          :guide :li.guide
          :error :li.error})
   {:key (rand)}
   [:aside (timestamp)]
   (->> message
        (interpose \space)
        (into [:span]))])

;; #console-anchor is an empty fake el to target when scrolling to bottom
(defn element
  []
  (let [messages @(re-frame/subscribe [::messages])]
    [:div#console
     (into
      [:ul [:li#console-anchor [:span#typing "..."]]]
      messages)]))

(reg-fx
 :scroll
 (fn [id]
   (let [el (js/document.getElementById id)]
     (.scrollIntoView el #js{:top (.-offsetHeight id)}))))

;; Boilerplate for toggling visibility
(reg-fx
 :show
 (fn [id]
   (-> id js/document.getElementById .-classList (.add "visible"))))
(reg-fx
 :hide
 (fn [id]
   (-> id js/document.getElementById .-classList (.remove "visible"))))
(reg-event-fx
 ::show
 (fn [_ [_ id]]
   {:fx [[:show id]]}))
(reg-event-fx
 ::hide
 (fn [_ [_ id]]
   {:fx [[:hide id false]]}))

;; Display a little typing icon in the console for ms
(reg-event-fx
 ::typing
 (fn [_ [_ ms]]
   {:fx [[:dispatch [::show "typing"]]
         [:scroll "console-anchor"]
         [:dispatch-later {:ms ms :dispatch [::hide "typing"]}]]}))

;; Log a message to the console DOM element (not browser console)
(reg-event-fx
 ::log
 (fn [{:keys [db]} [_ type & message]]
   {:db (update db :console (partial cons (li type message)))
    :fx [[:scroll "console-anchor"]]}))

;; Display a typing message for ms and log something immediately after
(reg-event-fx
 ::delayed-log
 (fn [_ [_ type ms & message]]
   {:fx [[:dispatch [::typing ms]]
         [:dispatch-later {:ms ms :dispatch (into [::log type] message)}]]}))

;; New sub instead of <get in the face of circular ns deps
(reg-sub
 ::messages
 (fn [db _]
   (:console db)))
