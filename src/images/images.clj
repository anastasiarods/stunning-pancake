(ns images.images
  (:require [mikera.image.core :as imagez]
            [images.config :refer [system-config]]
            [clojure.java.io :as io])
  (:import java.awt.Color
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

(def support-alpha #{"image/png"})
(def default-extension "jpg")
(def export-quality 0.7)
(def returned-resolutions [:small :med :large])

(def resizers
  {:small    #(imagez/resize % 128)
   :med      #(imagez/resize % 256)
   :large    #(imagez/resize % 512)
   :original identity})

(defn remove-alpha [image]
  (let [width          (.getWidth image)
        height         (.getHeight image)
        buffered-image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
    (doto (.createGraphics buffered-image)
      (.setColor Color/WHITE)
      (.fillRect 0 0 width height)
      (.drawImage image nil 0 0)
      (.dispose))
    buffered-image))

;; TODO: keep exif rotation
(defn keep-rotation [image exif]
  image)

(defn support-alpha? [mime-type]
  (contains? support-alpha mime-type))

;; NOTE: all images are a subclass of Image from java.awt package
;; transforming png to jpg before the optimization is an unnecessary step
;; because the next steps will be applied on java.awt.image.BufferedImage
(defn pre-process-image [{{:keys [mime-type tempfile]} :image :as params}]
  (let [processed (cond-> (ImageIO/read tempfile)
                    :always                   (keep-rotation {})
                    (support-alpha? mime-type) remove-alpha)]
    (assoc params :image processed)))

;;NOTE: Keeping the Image objects in memory is expensive and can be used as a vector of attack (DDoS which will fulfill java heap),
;; while storing the assets on disk is cheap and faster.
;; To use less memory and computations one could serve the static folder using NGINX which could be configured for serving static content.
;; TODO: inspect a better image format for web
;; https://blog.cloudflare.com/generate-avif-images-with-image-resizing/
(defn write-to-file! [{:keys [previews id] :as params}]
  (doseq [[resolution image] previews
          :let [path (str (:path system-config)
                          id "-" (name resolution) "."
                          default-extension)]]
    (io/make-parents path)
    (imagez/write
     image
     path
     default-extension
     :quality export-quality
     :progressive true))
  params)

(defn resize [{:keys [image] :as params}]
  (assoc params
         :previews
         (reduce-kv
          (fn [m k resize-fn]
            (assoc m k (resize-fn image)))
          {}
          resizers)))

(defn form-image-url [{:keys [resolution id]}]
  (str (:protocol system-config) "://"
       (:host system-config) ":"
       (:port system-config) "/"
       (:path system-config)
       id "-" (name resolution) "." default-extension))

(defn generate-preview-links [{:keys [id] :as params}]
  (let [links (map #(assoc {} % (form-image-url {:resolution % :id id}))
                   returned-resolutions)]
    (assoc params :links links)))

(defn handle-image [params]
  (-> params
      pre-process-image
      resize
      write-to-file!
      generate-preview-links))
