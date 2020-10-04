(ns images.middleware
  (:require [pantomime.mime :as pm]
            [clojure.data.json :as json]
            [ring.util.response :as resp]))

(defn wrap-mime-type-check [handler allowed-types]
  (fn [request]
    (let [image-file (get-in request [:multipart-params "image" :tempfile])
          mime-type  (pm/mime-type-of image-file)]
      (if (contains? allowed-types mime-type)
        (handler (assoc-in request [:multipart-params "image" :mime-type] mime-type))
        {:status  415
         :headers {}
         :body    "Unsupported Media Type"}))))

(defn wrap-cache-policy [handler]
  (fn [request]
    (-> (handler request)
        (resp/header "Cache-Control" "public"))))

(defn wrap-json-response [handler]
  (fn [request]
    (let [resp      (handler request)
          json-resp (update resp :body json/write-str)]
      (if (contains? (:headers resp) "Content-Type")
        json-resp
        (resp/content-type json-resp "application/json; charset=utf-8")))))

;; TODO: add explicit exception handler
(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception _
           {:status 500
            :body   "Exception caught"}))))

(defn wrap-not-found [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))
