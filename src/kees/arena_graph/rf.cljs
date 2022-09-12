#_{:clj-kondo/ignore [:unused-namespace :unused-referred-var]}
(ns kees.arena-graph.rf
  (:require
   [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx reg-cofx path]]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def <sub-lazy re-frame/subscribe)
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)

(def default-db
  {})

;; ========== EFFECTS ==========================================================
(reg-event-fx
 ::boot
 (fn [_ _]
   {:db default-db}))

;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::test
 (fn [db _]
   db))
