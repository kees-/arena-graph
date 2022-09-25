(ns kees.arena-graph.rf.console
  (:require [re-frame.core :as re-frame :refer [reg-event-fx reg-fx reg-sub]]))

(reg-fx
 :scroll
 (fn [id]
   (let [el (js/document.getElementById id)]
     (.scrollIntoView el #js{:top (.-offsetHeight id)}))))

(defn- timestamp
  []
  (let [now (.toLocaleTimeString (js/Date.))]
    (str "[" (re-find #"[^\s]+" now) "]")))

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

;; Boilerplate for toggling visibility
(reg-event-fx
 ::show
 (fn [_ [_ id]]
   {:fx [[:show id]]}))
(reg-fx
 :show
 (fn [id]
   (-> id js/document.getElementById .-classList (.add "visible"))))
(reg-fx
 :hide
 (fn [id]
   (-> id js/document.getElementById .-classList (.remove "visible"))))
(reg-event-fx
 ::hide
 (fn [_ [_ id]]
   {:fx [[:hide id]]}))

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
 (fn [_ [_ type ms message]]
   {:fx [[:dispatch [::typing ms]]
         [:dispatch-later {:ms ms :dispatch [::log type message]}]]}))
;; Issue! Only one arg to message. & message would  wrap too many vectors.

;; New sub instead of <get in the face of circular ns deps
(reg-sub
 ::messages
 (fn [db _]
   (:console db)))

;; #console-anchor is an empty fake el to target when scrolling to bottom
(defn element
  []
  (let [messages @(re-frame/subscribe [::messages])]
    [:div
     (into
      [:ul#console [:li#console-anchor [:span#typing "..."]]]
      messages)]))
