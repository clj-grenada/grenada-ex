(ns poomoo.transformers
  (:require [clojure.string :as string]
            [plumbing.core :refer [safe-get]]))

;;;; The transformer

(defn- empty-example? [ex]
  (= ex {:name "" :content ""}))

;; TODO: (among many other things) Add a specification for example maps. (RM
;;       2015-06-21)
(defn- transform-raw-example [jar-coords m]
  (fn transform-raw-example-infn [e]
    (if (empty-example? e)
      nil
      {:content (string/replace (safe-get e :content) #"\n\s*" "\n")
       :name (safe-get e :name)
       :coords jar-coords})))

(defn process-raw [jar-coords]
  (fn process-raw-infn [m v]
    (let [processed-exs (remove nil?
                                (mapv (transform-raw-example jar-coords m) v))]
      (if (empty? processed-exs)
        nil
        [:poomoo.ext/examples processed-exs]))))
