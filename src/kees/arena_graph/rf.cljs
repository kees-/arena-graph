#_{:clj-kondo/ignore [:unused-namespace :unused-referred-var]}
(ns kees.arena-graph.rf
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx reg-cofx path]]
            [kees.arena-graph.api :as api]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def <sub-lazy re-frame/subscribe)
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)
(defn <get [k] (<sub [::get k]))
(defn >assoc [k v] (>evt [::assoc k v]))

(def default-db
  {:channel-slug "other-ppl-sewing-channels"
   :channel-id nil
   :latest-response nil
   :graph-data {:nodes [{:id 1 :title ":)" :color "pink"}]
                :links []}})

;; ========== EFFECTS ==========================================================
(reg-event-fx
 ::boot
 (fn [_ _]
   {:db default-db}))

(reg-event-db
 ::resp->nodes
 (fn [db [_ resp]]
   (let [node-keys [:id :slug :title :owner_slug]
         data (->> resp :contents (mapv #(select-keys % node-keys)))]
     (assoc-in db [:graph-data :nodes] data))))

(reg-event-db
 ::assoc
 (fn [db [_ k v]]
   (assoc db k v)))

;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::get
 (fn [db [_ k]]
   (k db)))
