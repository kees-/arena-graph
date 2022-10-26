(ns kees.arena-graph.rf.flavor
  (:require [re-frame.core :as re-frame :refer [reg-event-fx]]
            [kees.arena-graph.rf.console :as console]))

(reg-event-fx
 ::intro
 (fn [_ _]
   {:fx [[:dispatch-later {:ms 1500 :dispatch [::console/delayed-log :guide 900 "Hi!"]}]
         [:dispatch-later {:ms 3250 :dispatch [::console/delayed-log :guide 2250 "I'll map out connections on the website are.na. Add a channel's URL to get started."]}]]}))

;; A subtle nudge about what size of channel may work better.
(reg-event-fx
 ::size-shaming
 (fn [_ [_ pages]]
   (let [extra-large [[:dispatch [::console/delayed-log :guide 500 "You're really gonna do that? That's a giant channel. Well..."]]]
         large [[:dispatch [::console/delayed-log :guide 500 "Wow, that's some channel! This will take a little time."]]]
         medium [[:dispatch [::console/delayed-log :guide 500 "That's a decent sized channel. I'll start looking now."]]]]
     {:fx (cond
            (< 18 pages) extra-large
            (< 8 pages) large
            (< 4 pages) medium
            :else [])})))

(reg-event-fx
 ::completed-explanation
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:flavor :completed-explanation-seen] true)
    :fx [[:dispatch-later
          {:ms 1500
           :dispatch [::console/delayed-log :guide 3500 "Alright it's done! Enjoy. On the desktop you can hover over nodes to see what channels they represent."]}]
         [:dispatch-later
          {:ms 7500
           :dispatch [::console/delayed-log :guide 3000 "And clicking a node visits its channel if you didn't notice."]}]]}))

(reg-event-fx
 ::request-error
 (fn [_ [_ code]]
   {:fx [[:dispatch [::console/log :error "Something's gone wrong working with this channel!"]]
         [:dispatch [::console/log :error "The response returned a" code "code."]]
         [:dispatch [::console/delayed-log :guide 1250 "I'll try again and skip the request that's seeming to cause the problem. Unfortunately some nodes and connections might be missed."]]]}))
