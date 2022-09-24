(ns kees.arena-graph.rf
  (:require [ajax.core :refer [json-request-format json-response-format]]
            [clojure.string :as s]
            [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx path]]
            [day8.re-frame.http-fx]
            [kees.arena-graph.logic :as logic]
            [kees.arena-graph.rf.console :as console]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)
(defn <get [k] (<sub [::get k]))
(defn >assoc [k v] (>evt [::assoc k v]))
(defn >GET [opts] (>evt [::GET opts]))

(def empty-graph
  {:nodes []
   :links []})

(def default-db
  {:channel-slug "other-ppl-sewing-channels"
   :channel-id nil
   :connection-count nil
   :active-color :gold
   :graph-data empty-graph
   :console []})

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
   {:db default-db
    :fx [[:dispatch-later {:ms 2000 :dispatch [::console/delayed-log :info 900 "Hi!"]}]
         [:dispatch-later {:ms 4250 :dispatch [::console/delayed-log :info 1750 "Add a channel to get started."]}]]}))

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
 (fn [graph [_ base variance]]
   (update graph :nodes #(mapv merge % (logic/size-variance base variance)))))

(reg-event-db
 ::add-node-colors
 [(path :graph-data)]
 (fn [graph-data [_ color]]
   (update graph-data :nodes #(mapv merge % (logic/hex-maps color)))))

;; Call with a thumb request
(reg-event-db
 ::o0-populate
 [(path :graph-data)]
 (fn [data [_ thumb]]
   (let [node-keys [:id :slug :title :owner_slug :base_class]
         node (-> thumb
                  (select-keys node-keys)
                  (merge (logic/size-variant 4 0.5)
                         (logic/hex-map :gold)
                         {:order 0}))]
     (update data :nodes conj node))))

(reg-event-db
 ::o1-connect
 (fn [{:keys [channel-id] :as db} [_ channels]]
   (let [links (mapv #(hash-map :source (:id %)
                                :target channel-id
                                :color (logic/hex :pink))
                     channels)]
     (update-in db [:graph-data :links] into links))))

;; Call with a contents request
(reg-event-db
 ::o1-populate
 [(path :graph-data)]
 (fn [data [_ channels]]
   (let [node-keys [:id :slug :title :owner_slug :base_class]
         nodes (mapv
                #(-> (select-keys % node-keys)
                     (merge (logic/size-variant 1.25 0.5)
                            (logic/hex-map :pink)
                            {:order 1}))
                channels)]
     (update data :nodes into nodes))))

(reg-event-fx
 ::order-up
 (fn [{:keys [db]} _]
   (let [id (:channel-id db)]
     (if id
       {:fx [[:dispatch [::assoc :graph-data empty-graph]]
             [:dispatch [::GET {:path ["channels" id "thumb"]
                                :params {:page 1 :per 50}
                                :on-success [::order-up-o0 id]}]]]}
       {:fx [[:dispatch [::console/log :error "Grab the ID before submitting an order!"]]]}))))

(reg-event-fx
 ::order-up-o0
 (fn [_ [_ id thumb]]
   {:fx [[:dispatch [::o0-populate thumb]]
         [:dispatch [::GET {:path ["channels" id "contents"]
                            :params {:page 1 :per 50}
                            :on-success [::order-up-o1 id]}]]]}))

(reg-event-fx
 ::order-up-o1
 (fn [_ [_ id {:keys [contents]}]]
   (let [channels (filterv #(= "Channel" (:base_class %)) contents)]
     (if (not-empty channels)
       {:fx [[:dispatch [::o1-populate channels]]
             [:dispatch [::o1-connect channels]]
             #_[:dispatch [::GET {}]]]}
       {:fx [[:dispatch [::console/log :error "There are no channels in the chosen channel?!"]]]}))))

;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::get
 (fn [db [_ k]]
   (k db)))
