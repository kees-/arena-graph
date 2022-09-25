(ns kees.arena-graph.rf.flavor
  (:require [re-frame.core :as re-frame :refer [reg-event-fx]]
            [kees.arena-graph.rf.console :as console]))

(reg-event-fx
 ::intro
 (fn [_ _]
   {:fx [[:dispatch-later {:ms 2000 :dispatch [::console/delayed-log :guide 900 "Hi!"]}]
         [:dispatch-later {:ms 4250 :dispatch [::console/delayed-log :guide 1750 "Add a channel to get started."]}]]}))

(reg-event-fx
 ::completed-explanation
 (fn [_ _]
   {:fx [[:dispatch-later
          {:ms 1500
           :dispatch [::console/delayed-log :guide 3500 "Alright it's done! Enjoy. On the desktop you can hover over nodes to see what channels they represent."]}]
         [:dispatch-later
          {:ms 7500
           :dispatch [::console/delayed-log :guide 3000 "And clicking a node visits its channel if you didn't notice."]}]]}))
