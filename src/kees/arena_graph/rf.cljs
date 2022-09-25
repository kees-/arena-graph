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
  {:channel-slug ""
   :thumb {}
   :active-color :gold
   :graph-data empty-graph
   :console []
   :working false})

(def ^:private location "http://api.are.na/v2/")
(def ^:private auth "FILL IN YOUR OWN!")

(reg-fx :tap (fn [data] (tap> data)))
(reg-fx :error (fn [resp] (.error js/console "ERROR:" (clj->js resp))))
(reg-fx :browse (fn [url] (.open js/window url)))
(reg-fx :blur (fn [] (.blur (.-activeElement js/document))))

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
                       :timeout 20000
                       :format (json-request-format)
                       :response-format (json-response-format {:keywords? true})
                       :on-failure [::error]}
         request (-> (merge default-opts supplied {:uri uri})
                     (assoc-in [:headers :authorization] auth))]
     {:fx [[:http-xhrio request]]})))

(reg-event-fx
 ::GET-contents-page-loop
 (fn [{:keys [db]} [_ pages remaining accumulation completion-evt & resp]]
   (let [channel (get-in db [:thumb :id])
         current (dec remaining)
         accumulation (into accumulation (get (first resp) :contents))]
     (if (< remaining 1)
       {:fx [[:dispatch (conj completion-evt accumulation)]]}
       {:fx [(when (< 1 pages)
               [:dispatch [::console/log :info "Requesting page" (- pages current) "of" pages]])
             [:dispatch-later
              {:ms 1500
               :dispatch [::GET
                          {:path ["channels" channel "contents"]
                           :params {:page (- pages current)
                                    :per 100}
                           :on-success [::GET-contents-page-loop
                                        pages
                                        current
                                        accumulation
                                        completion-evt]
                           :on-failure [::error ":( Something went wrong in the loop"]}]}]]}))))

;; ========== EFFECTS ==========================================================
(reg-event-fx
 ::boot
 (fn [_ _]
   {:db default-db
    :fx [[:dispatch-later {:ms 2000 :dispatch [::console/delayed-log :guide 900 "Hi!"]}]
         [:dispatch-later {:ms 4250 :dispatch [::console/delayed-log :guide 1750 "Add a channel to get started."]}]]}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 ::tap
 (fn [_ [_ data]]
   {:fx [[:tap data]]}))

(reg-event-fx
 ::error
 (fn [_ [_ & error]]
   {:fx [[:error error]]}))

(reg-event-db
 ::assoc
 (fn [db [_ k v]]
   (assoc db k v)))

#_(reg-event-db
 ::assoc-prop
 (fn [db [_ state-key prop-key v]]
   (assoc db state-key (prop-key v))))

(reg-event-fx
 ::select-channel-success
 (fn [{:keys [db]} [_ response]]
   {:db (assoc db :thumb response)
    :fx [[:dispatch-later
          {:ms 500
           :dispatch [::console/delayed-log :guide 750 "Great! Looks like a valid channel."]}]]}))

(reg-event-fx
 ::select-channel
 (fn [_ [_ query]]
   (if-let [slug (re-find #"[-a-z0-9]+$" query)]
     {:fx [[:dispatch [::console/log :info "Changing channel to:" slug]]
           [:dispatch [::GET {:path ["channels" slug "thumb"]
                              :on-success [::select-channel-success]}]]]}
     {:fx [[:dispatch [::console/log :error "Are you sure that channel name is valid?"]]]})))

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

#_(reg-event-db
 ::add-node-sizes
 [(path :graph-data)]
 (fn [graph [_ base variance]]
   (update graph :nodes #(mapv merge % (logic/size-variance base variance)))))

#_(reg-event-db
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
 (fn [db [_ channels]]
   (let [id (get-in db [:thumb :id])
         links (mapv #(hash-map :source (:id %)
                                :target id
                                :color (logic/hex :pink))
                     channels)]
     (update-in db [:graph-data :links] into links))))

;; Call with a seq of channel maps
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
   (let [id (get-in db [:thumb :id])
         id-unknown [[:dispatch [::console/log :error "I'm not finding the id of the channel."]]]
         in-progress [[:dispatch [::console/log :error "I'm already working on a graph! Be patient!"]]]
         continue [[:dispatch [::console/delayed-log :guide 500 "Okay, I'm getting to work"]]
                   [:dispatch [::assoc :graph-data empty-graph]]
                   [:blur nil]
                   [:dispatch [::assoc :working true]]
                   [:dispatch [::order-up-o0 id]]]]
     {:fx (cond
            (not id) id-unknown
            (:working db) in-progress
            :else continue)})))

(reg-event-fx
 ::o0-conversation
 (fn [_ [_ pages]]
   (let [medium [[:dispatch [::console/delayed-log :guide 500 "Wow, that's some channel! This will take a little time."]]]
         large [[:dispatch [::console/delayed-log :guide 500 "That's a decent sized channel. I'll start looking now."]]]]
     {:fx (cond
            (< 5 pages) medium
            (< 2 pages) large
            :else [])})))

(reg-event-fx
 ::order-up-o0
 (fn [{:keys [db]} [_ id]]
   (let [thumb (:thumb db)
         length (:length thumb)
         pages (Math/ceil (* length 0.01))]
     {:fx [[:dispatch [::o0-populate thumb]]
           [:dispatch-later
            {:ms 500
             :dispatch [::o0-conversation pages]}]
           [:dispatch-later
            {:ms 2000
             :dispatch [::GET-contents-page-loop
                        pages pages [] [::order-up-o1 id]]}]]})))

(reg-event-fx
 ::order-up-o1
 (fn [_ [_ id contents]]
   (let [channels (filterv #(= "Channel" (:base_class %)) contents)]
     (if (not-empty channels)
       {:fx [[:dispatch [::o1-populate channels]]
             [:dispatch [::o1-connect channels]]
             [:dispatch [::complete]] ;; TEMP relocate to end
             #_[:dispatch [::GET {}]]]}
       {:fx [[:dispatch [::console/log :error "There are no channels in the chosen channel?!"]]]}))))

(reg-event-fx
 ::complete
 (fn [_ _]
   {:fx [[:dispatch [::assoc :working false]]
         [:dispatch-later
          {:ms 1500
           :dispatch [::console/delayed-log :guide 3500 "Alright it's done! Enjoy. On the desktop you can hover over nodes to see what channels they represent."]}]
         [:dispatch-later
          {:ms 7500
           :dispatch [::console/delayed-log :guide 3000 "And clicking a node visits its channel if you didn't notice."]}]]}))

;; ========== SUBSCRIPTIONS ====================================================
(reg-sub
 ::get
 (fn [db [_ k]]
   (k db)))
