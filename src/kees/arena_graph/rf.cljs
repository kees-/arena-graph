(ns kees.arena-graph.rf
  (:require [ajax.core :refer [json-request-format json-response-format]]
            [clojure.string :as s]
            [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx path]]
            [day8.re-frame.http-fx]
            [kees.arena-graph.logic :as logic]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)
(defn <get [k] (<sub [::get k]))
(defn >assoc [k v] (>evt [::assoc k v]))
(defn >GET [opts] (>evt [::GET opts]))

(def default-db
  {:channel-slug "other-ppl-sewing-channels"
   :channel-id nil
   :connection-count nil
   :active-color :gold
   :graph-data {:nodes []
                :links []}})

(def ^:private location "http://api.are.na/v2/")
(def ^:private auth "FILL IN YOUR OWN!")

(reg-fx :tap (fn [data] (tap> data)))
(reg-fx :error (fn [resp] (.error js/console "ERROR:" resp)))
(reg-fx :browse (fn [url] (.open js/window url)))

;; Provide a map with, at minimum, :path and :on-success kvs
;  :path is a vector of the URI path components to be appended to the API URL
;  :on-success is a namespaced kw for an event handler that accepts the response
(reg-event-fx
 ::GET
 (fn [_ [_ {:keys [path] :as opts}]]
   (let [uri (->> path
                  (mapv (comp #(s/replace % "^/|/$" "") str))
                  (interpose "/")
                  (apply str location))
         vwrap #(if (vector? %) % [%])
         req-keys [:params :on-success :on-failure]
         supplied (-> opts
                      (select-keys req-keys)
                      (update :on-success vwrap)
                      (update :on-failure vwrap))
         default-opts {:method :get
                       :timeout 10000
                       :format (json-request-format)
                       :response-format (json-response-format {:keywords? true})
                       :on-failure [::error]}
         request (-> (merge default-opts supplied {:uri uri})
                     (assoc-in [:headers :authorization] auth))]
     {:fx [[:http-xhrio request]]})))

;; ========== EFFECTS ==========================================================
(reg-event-fx
 ::boot
 (fn [_ _]
   {:db default-db}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 ::tap
 (fn [_ [_ data]]
   {:fx [[:tap data]]}))

(reg-event-fx
 ::error
 (fn [_ [_ error]]
   {:fx [[:error error]]}))

(reg-event-db
 ::resp->nodes
 (fn [db [_ resp]]
   (let [node-keys [:id :slug :title :owner_slug :base_class :class]
         data (->> resp :contents (mapv #(select-keys % node-keys)))]
     (assoc-in db [:graph-data :nodes] data))))

(reg-event-db
 ::assoc
 (fn [db [_ k v]]
   (assoc db k v)))

(reg-event-db
 ::assoc-prop
 (fn [db [_ state-key prop-key v]]
   (assoc db state-key (prop-key v))))

;; Format a web URL for either channels or blocks and visit them in the browser
(reg-event-fx
 ::graph-node->visit
 (fn [_ [_ m]]
   (let [{:keys [base_class owner_slug slug id]} (js->clj m :keywordize-keys true)
         path (if (= "Channel" base_class)
                [owner_slug slug]
                ["block" id])
         url (apply str "https://are.na/" (interpose "/" path))]
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
