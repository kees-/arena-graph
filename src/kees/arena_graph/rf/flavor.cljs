(ns kees.arena-graph.rf.flavor
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [kees.arena-graph.rf.console :as console]))

;; ========== REDUNDANT ========================================================
(reg-event-db
 ::set-initialized
 (fn [db _]
   (assoc db :initialized? true)))

(reg-event-db
 ::set-busy
 (fn [db [_ bool]]
   (-> db
       (assoc :working? bool)
       (assoc :active? bool))))
;; Hm...

;; ========== GENERIC FLAVOR TEXT ==============================================
(reg-event-fx
 ::intro
 (fn [_ [_ skip?]]
   (if skip?
     {:fx [[:dispatch [::set-initialized]]
           [:dispatch [::console/log :guide "Go!"]]]}
     (let [intro "I'll map out connections on the website are.na. Add a channel's URL to get started."]
       {:fx [[:dispatch-later
              [{:ms 1500
                :dispatch [::console/delayed-log :guide 900 "Hi!"]}
               {:ms 3250
                :dispatch [::console/delayed-log :guide 2250 intro]}
               {:ms 7000
                :dispatch [::set-initialized]}]]]}))))

;; A subtle nudge about what size of channel may work better.
(reg-event-fx
 ::size-shaming
 (fn [_ [_ pages]]
   (let [extra-large [[:dispatch [::console/delayed-log :guide 500 "You're really gonna do that? That's a giant channel. Well..."]]]
         large [[:dispatch [::console/delayed-log :guide 500 "Wow, that's some channel! This will take a little time."]]]
         medium [[:dispatch [::console/delayed-log :guide 500 "That's a decent sized channel. I'll start looking now."]]]]
     {:fx (cond
            (< 22 pages) extra-large
            (< 13 pages) large
            (< 6 pages) medium
            :else [])})))

(reg-event-fx
 ::completed-explanation
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:flavor :completed-explanation-seen] true)
    :fx [[:dispatch-later
          [{:ms 1500
            :dispatch [::console/delayed-log :guide 3500 "Alright it's done! Enjoy. On the desktop you can hover over nodes to see what channels they represent."]}
           {:ms 7500
            :dispatch [::console/delayed-log :guide 3000 "And clicking a node visits its channel if you didn't notice."]}]]]}))


;; ========== EVENT HANDLING ===================================================
(reg-event-fx
 ::request-error
 (fn [_ [_ code]]
   {:fx [[:dispatch [::console/log :error "Something's gone wrong working with this channel!"]]
         [:dispatch [::console/log :error "The most recent request returned a" code "code."]]
         [:dispatch [::console/delayed-log :guide 1250 "I'll try again and skip the one that's seeming to cause the problem. Unfortunately some nodes and connections might be missed."]]]}))

(reg-event-fx
 ::unauthorized
 (fn [_ [_ code]]
   {:fx [[:dispatch [::console/log :error "I got a " code "code."]]
         [:dispatch [::console/delayed-log :guide 1000 "That means you weren't authorized to make that request. Is the channel private?"]]
         [:dispatch-later
          [{:ms 2000
            :dispatch [::console/delayed-log :guide 1750 "Sorry... I can't take credentials right now. How about something else?"]}
           {:ms 3750
            :dispatch [::set-busy false]}]]]}))

(reg-event-fx
 ::no-channels
 (fn [_ _]
   {:fx [[:dispatch [::console/log :error "The channel you chose doesn't contain links to any other channels!"]]]}))

(reg-event-fx
 ::unknown-error
 (fn [_ [_ code]]
   {:fx [[:dispatch [::console/log :error "Something's really wrong! The request returned a" code "code."]]
         [:dispatch-later
          [{:ms 350
            :dispatch [::console/delayed-log :guide 1150 "I don't know how to deal with this... Ahh..."]}
           {:ms 1750
            :dispatch [::console/log :guide "Okay I collected myself. Ready to try something else"]}
           {:ms 2000
            :dispatch [::set-busy false]}]]]}))

(reg-event-fx
 ::generic-error
 (fn [_ [_ code]]
   (let [action (case code
                  401 [:dispatch [::unauthorized code]]
                  [:dispatch [::unknown-error code]])]
     {:fx [action]})))
