(ns kees.arena-graph.api
  (:require [ajax.core :as ajax]
            [clojure.string :as s]))

(def ^:private auth "FILL WITH YOUR OWN!")
(def ^:private location "http://api.are.na/v2/")

#_{:clj-kondo/ignore [:unresolved-var]}
(defn GET
  "Format a generic GET request"
  [{:keys [path] :as params-map}]
  (let [req-keys [:handler :error-handler :params]
        supplied-params (select-keys params-map req-keys)
        default-opts {:format :json
                      :response-format :json
                      :keywords? true
                      :error-handler js/console.error
                      :params {}}
        params (-> (merge default-opts supplied-params)
                   (assoc-in [:headers :authorization] auth))
        uri (->> path
                 (mapv (comp #(s/replace % "^/|/$" "") str))
                 (interpose "/")
                 (apply str location))]
    (ajax/GET uri params)))
