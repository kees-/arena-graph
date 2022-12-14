(ns kees.arena-graph.rf
  (:require [ajax.core :refer [json-request-format json-response-format]]
            [clojure.string :as s]
            [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-sub reg-fx reg-cofx inject-cofx path]]
            [day8.re-frame.http-fx]
            [kees.arena-graph.logic :as logic]
            [kees.arena-graph.rf.console :as console]
            [kees.arena-graph.rf.flavor :as flavor]))

;; ========== SETUP ============================================================
(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)
(def >evt-now re-frame/dispatch-sync)
(defn <get [& k] (<sub [::get k]))
(defn >assoc [k v] (>evt [::assoc k v]))
(defn >GET [opts] (>evt [::GET opts]))

;; Source!: https://clojureverse.org/t/9104/9
(def ^:private url-query-params
  (let [query-arr (-> js/window
                      .-location
                      .-href
                      js/URL.
                      .-searchParams
                      js/URLSearchParams.
                      js/Array.from)
        f (fn [m [k v]]
            (update m (keyword k) (fn [cur]
                                    (cond
                                      (nil? cur) v
                                      (vector? cur) (conj cur v)
                                      :else [cur v]))))]
    (reduce f {} query-arr)))

(reg-cofx
 :screen-width
 (fn [cofx]
   (let [width (.-innerWidth js/window)]
     (-> cofx
         (assoc :screen-width width)
         (assoc :vertical-layout? (<= width 666))))))

(def ^:private falsy-strs
  ["n" "no" "none" "false" "skip" "hide"])
(def ^:private skip-intro?
  (some #{(:intro url-query-params)} falsy-strs))

(def global-per 50)

(def empty-graph
  {:nodes []
   :links []})

(def default-progress
  {:current 0
   :total 0
   :channel-total 0
   :channel-current 0})

(def default-db
  {:graph-data empty-graph
   :thumb {}
   :hovered-node nil
   :console []
   :version 2
   :skip-intro? skip-intro?
   :canvas-covered? false
   :initialized? false ; whether the app has finished its startup and intro
   :working? false ; whether to display "create" button / allow creation
   :active? false ; whether to display gif panel
   :progress default-progress
   #_#_ :palette-color :aqua ; for testing
   :style {:o1-color :gold-light
           :o2-color :aqua}
   :flavor {:completed-explanation-seen false}})

(def ^:private location "https://api.are.na/v2/")
(def ^:private auth "")

(def node-keys
  [:id :slug :title :owner_slug :base_class :user :length])

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
                       :timeout 15000
                       :format (json-request-format)
                       :response-format (json-response-format {:keywords? true})
                       :on-failure [::console/log :error "Oh no! Something went wrong."]}
         request (-> (merge default-opts supplied {:uri uri})
                     (assoc-in [:headers :authorization] auth))]
     {:fx [[:http-xhrio request]]})))

(reg-event-fx
 ::o1-GET-loop
 (fn [{:keys [db]} [_ pages remaining accumulation completion-evt & resp]]
   (let [channel (get-in db [:thumb :id])
         current (dec remaining)
         accumulation (into accumulation (get (first resp) :contents))]
     (if (< remaining 1)
       {:fx [[:dispatch (conj completion-evt accumulation)]]}
       {:db (-> db
                (assoc-in [:progress :channel-current] (- pages current))
                (assoc-in [:progress :channel-total] pages))
        :fx [(when (< 1 pages)
               [:dispatch [::console/log :info "Requesting page" (- pages current) "of" pages]])
             [:dispatch-later
              {:ms 500
               :dispatch [::GET
                          {:path ["channels" channel "contents"]
                           :params {:page (- pages current)
                                    :per global-per}
                           :on-success [::o1-GET-loop
                                        pages
                                        current
                                        accumulation
                                        completion-evt]
                           :on-failure [::console/log :error ":( Something went wrong"]}]}]]}))))

(reg-event-fx
 ::o2-GET-node-loop
 (fn [_ [_
         {:keys [id pages remaining accumulation completion-evt]
          :or {accumulation []}}
         & resp]]
   (let [current (dec remaining)
         accumulation (into accumulation (:contents (first resp)))]
     (if (< remaining 1)
       {:fx [[:dispatch (conj completion-evt accumulation)]]}
       (let [next-args {:id id
                        :pages pages
                        :remaining current
                        :accumulation accumulation
                        :completion-evt completion-evt}]
         {:fx [(when (< 1 pages)
                 [:dispatch [::console/log :info "Requesting page" (- pages current) "of" pages]])
               [:dispatch-later
                {:ms 750
                 :dispatch [::GET
                            {:path ["channels" id "contents"]
                             :params {:page (- pages current)
                                      :per global-per}
                             :on-success [::o2-GET-node-loop next-args]
                             :on-failure [::GET-error next-args]}]}]]})))))

(reg-event-fx
 ::GET-error
 (fn [_ [_ next-args resp]]
   (let [{:keys [response]} resp]
     {:fx [[::flavor/request-error (:code response)]
           [:dispatch-later
            {:ms 2000
             :dispatch [::o2-GET-node-loop (update next-args :remaining dec)]}]]})))

;; ========== EFFECTS ==========================================================
(reg-event-fx
 ::boot
 [(inject-cofx :screen-width)]
 (fn [cofx _]
   (let [skip? (:skip-intro? default-db)
         layout (select-keys cofx [:screen-width :vertical-layout?])]
     {:db (merge default-db layout)
      :fx [[:dispatch [::flavor/intro skip?]]]})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 ::tap
 (fn [_ [_ data]]
   {:fx [[:tap data]]}))

(reg-event-db
 ::assoc
 (fn [db [_ k v]]
   (assoc db k v)))

(reg-event-db
 ::set-busy
 (fn [db [_ bool]]
   (-> db
       (assoc :working? bool)
       (assoc :active? bool))))

(reg-event-db
 ::toggle-canvas-cover
 (fn [db _]
   (update db :canvas-covered? not)))

(reg-event-fx
 ::select-channel-success
 (fn [{:keys [db]} [_ response]]
   {:db (assoc db :thumb response)
    :fx [[:dispatch-later
          [{:ms 500
            :dispatch [::console/delayed-log :guide 750 "Great! Looks like a valid channel."]}
           {:ms 1750
            :dispatch [::set-busy false]}]]]}))

(reg-event-fx
 ::select-channel-failure
 (fn [_ [_ response]]
   (let [code (-> response :response :code)]
     {:fx [[:dispatch [::flavor/generic-error code]]]})))

(reg-event-fx
 ::select-channel
 (fn [{:keys [db]} [_ query]]
   (if-let [slug (re-find #"[-_a-z0-9]+$" query)]
     {:db (assoc db :working? true)
      :fx [[:blur nil]
           [:dispatch [::console/log :info "Changing channel to:" slug]]
           [:dispatch [::GET {:path ["channels" slug "thumb"]
                              :on-success [::select-channel-success]
                              :on-failure [::select-channel-failure]}]]]}
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

(reg-event-fx
 ::hover-node
 (fn [{:keys [db]} [_ node]]
   (let [node-map (js->clj node :keywordize-keys true)]
     {:db (assoc db :hovered-node node-map)
      :fx (if node-map
            [[::console/show ["channel-info-container" "visible"]]]
            [[::console/hide ["channel-info-container" "visible"]]])})))

;; Call with a thumb request
(reg-event-db
 ::o0-populate
 [(path :graph-data)]
 (fn [data [_ thumb]]
   (let [node (-> thumb
                  (select-keys node-keys)
                  (merge (logic/size-variant 3.5 1)
                         (logic/hex-map :static-var-ui)
                         {:order 0}))]
     (update data :nodes conj node))))

;; Call with a seq of channel maps
(reg-event-db
 ::o1-populate
 [(path :graph-data)]
 (fn [data [_ channels]]
   (let [nodes (mapv
                #(-> (select-keys % node-keys)
                     (merge (logic/size-variant 0.75 1)
                            (logic/hex-map :gold-light)
                            {:order 1}))
                channels)]
     (update data :nodes into nodes))))

(reg-event-db
 ::connect
 [(path :graph-data)]
 (fn [data [_ arg-map]]
   (let [{:keys [id channels color]} arg-map
         links (mapv
                #(hash-map :source (:id %)
                           :target id
                           :color (logic/hex color))
                channels)]
     (update data :links into links))))

(reg-event-db
 ::o2-populate
 [(path :graph-data)]
 (fn [data [_ arg-map]]
   (let [{:keys [channels color]} arg-map
         nodes (mapv
                #(-> (select-keys % node-keys)
                     (merge (logic/size-variant 0.5 0.5)
                            (logic/hex-map color)
                            {:order 2}))
                channels)]
     (update data :nodes logic/into-by-key nodes :id))))

;; Block or continue initialization of graph crawl
(reg-event-fx
 ::order-up
 (fn [{:keys [db]} _]
   (let [id (get-in db [:thumb :id])
         id-unknown [[:dispatch [::console/log :error "I'm not finding the id of the channel."]]]
         in-progress [[:dispatch [::console/log :error "I'm already working on a graph! Be patient!"]]]
         continue [[:dispatch [::console/delayed-log :guide 500 "Okay, I'm getting to work"]]
                   [:dispatch [::assoc :graph-data empty-graph]]
                   [:blur nil]
                   [:dispatch [::set-busy true]]
                   [:dispatch [::assoc :progress default-progress]]
                   [:dispatch [::o0-order-up id]]]]
     {:fx (cond
            (not id) id-unknown
            (:working? db) in-progress
            :else continue)})))

(reg-event-fx
 ::o0-order-up
 (fn [{:keys [db]} [_ id]]
   (let [thumb (:thumb db)
         length (:length thumb)
         pages (Math/ceil (/ length global-per))]
     {:fx [[:dispatch [::o0-populate thumb]]
           [:dispatch-later
            [{:ms 500
              :dispatch [::flavor/size-shaming pages]}
             {:ms 2000
              :dispatch [::o1-GET-loop
                         pages pages [] [::o1-order-up id]]}]]]})))

(reg-event-fx
 ::o1-order-up
 (fn [{:keys [db]} [_ id contents]]
   (let [color (get-in db [:style :o1-color])
         channels (filterv #(= (:base_class %) "Channel") contents)]
     (if (not-empty channels)
       {:fx [[:dispatch [::o1-populate channels]]
             [:dispatch [::connect {:id id
                                    :channels channels
                                    :color color}]]
             [:dispatch [::o2-order-loop (count channels) channels]]]}
       {:fx [[:dispatch [::flavor/no-channels]]
             [:dispatch [::set-busy false]]]}))))

(reg-event-fx
 ::o2-order-loop
 (fn [{:keys [db]} [_ total remaining-channels]]
   (let [[active & remaining] remaining-channels
         {:keys [length id slug]} active
         pages (Math/ceil (/ length global-per))
         current-num (- total (count remaining-channels) -1)
         processing-str (str "Processing channel "
                             current-num " of " total
                             ": " slug)]
     {:db (-> db
              (assoc-in [:progress :current] current-num)
              (assoc-in [:progress :total] total))
      :fx [[:dispatch [::console/log :guide processing-str]]
           [:dispatch [::o2-GET-node-loop
                       {:id id
                        :pages pages
                        :remaining pages
                        :completion-evt [::o2-order-up total id remaining]}]]]})))

(reg-event-fx
 ::o2-order-up
 (fn [{:keys [db]} [_ total id remaining contents]]
   (let [color (get-in db [:style :o2-color])
         channels (filterv #(= (:base_class %) "Channel") contents)]
     {:fx [[:dispatch [::o2-populate {:channels channels
                                      :color color}]]
           [:dispatch [::connect {:id id
                                  :channels channels
                                  :color color}]]
           (if remaining
             [:dispatch [::o2-order-loop total remaining]]
             [:dispatch [::complete]])]})))

;; Display flavor and return app to prepared state
(reg-event-fx
 ::complete
 (fn [{:keys [db]} _]
   {:fx [(if (get-in db [:flavor :completed-explanation-seen])
           [:dispatch [::console/delayed-log :guide 1000 "Complete!"]]
           [:dispatch [::flavor/completed-explanation]])
         [:dispatch [::set-busy false]]]}))

;; ========== SUBSCRIPTIONS ====================================================
;; Use via the shortcut <get
(reg-sub
 ::get
 (fn [db [_ ks]]
   (get-in db ks)))

(reg-sub
 ::graph-dimensions
 (fn [{:keys [screen-width vertical-layout?]} _]
   (if vertical-layout?
     {:width (js/Math.floor (* 0.92 screen-width))
      :height (js/Math.floor (* 0.72 screen-width))}
     {:width 500 :height 400})))

(reg-sub
 ::version
 (fn [{:keys [version]} _]
   (str "amoeba-" (reduce * (repeat version 2)))))
