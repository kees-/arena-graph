#_{:clj-kondo/ignore [:unused-namespace :unused-referred-var]}
(ns kees.arena-graph.rf
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx reg-cofx path]]
            [kees.arena-graph.api :as api]
            [kees.arena-graph.logic :as logic]))

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
   :connection-count nil
   :active-color :gold
   :graph-data {:nodes []
                :links []}})

(reg-fx
 :browse
 (fn [url]
   (.open js/window url)))

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

(reg-event-fx
 ::visit-node-channel
 (fn [_ [_ m]]
   (let [url (str "https://are.na/" (.-owner_slug m) "/" (.-slug m))]
     {:fx [[:browse url]]})))

(reg-event-db
 ::add-node-sizes
 [(path :graph-data)]
 (fn [graph-data _]
   (update graph-data :nodes #(mapv merge % (logic/size-variance 5 3)))))

(reg-event-db
 ::add-node-colors
 [(path :graph-data)]
 (fn [graph-data [_ color]]
   (update graph-data :nodes #(mapv merge % (logic/hex-map color)))))


;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::get
 (fn [db [_ k]]
   (k db)))
