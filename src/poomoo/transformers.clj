(ns poomoo.transformers)

;; TODO: (among many other things) Add a specification for example maps. (RM
;;       2015-06-21)
(defn transform-raw-example [m e]
  {:content}
  )

(defn process-raw [v]
  [:poomoo.ext/examples (mapv transform-raw-example v)])
