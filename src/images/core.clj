(ns images.core
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [bidi.ring :as bidi.ring :refer [->Files
                                             ->WrapMiddleware]]
            [images.middleware :as mw]
            [ring.util.response :as resp]
            [images.config :refer [system-config]]
            [images.images :as images]))

(def allowed-types #{"image/jpeg" "image/png"})

(defonce server (atom nil))

(defonce store (atom {}))

;; TODO: store meta information
(defn save-images [{:keys [id]}]
  (swap! store update :images conj id))

(defn upload-handler [request]
  (let [image     (get-in request [:multipart-params "image"])
        image-id  (java.util.UUID/randomUUID)
        result    (images/handle-image {:image image :id image-id})]
    (save-images result)
    (resp/response {:images (:links result)})))

(defn not-found [_]
  {:status  404
   :headers {"Content-Type" "text/plain"}
   :body    "Not found"})

(def handler
  (bidi.ring/make-handler
   ["/"
    {"images" (-> (->Files {:dir (:path system-config)})
                  (->WrapMiddleware mw/wrap-not-found)
                  (->WrapMiddleware mw/wrap-cache-policy))
     "upload" (-> upload-handler
                  (->WrapMiddleware mw/wrap-json-response)
                  (->WrapMiddleware (fn [handler]
                                      (mw/wrap-mime-type-check handler
                                                               allowed-types)))
                  (->WrapMiddleware wrap-multipart-params))
     true     not-found}]))

(def app
  (-> handler
      mw/wrap-exception))

(defn start []
  (reset! server (run-jetty app
                            {:port  (:port system-config)
                             :join? true})))

(defn stop []
  (when @server
    (.stop @server))
  (reset! server nil))

(defn -main
  [& _]
  (start))
