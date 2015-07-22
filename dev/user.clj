(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.pprint :refer [pprint]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get] :as plumbing]
            [guten-tag.core :as gt]
            [grenada.utils :refer [fnk*]]
            [grenada.core :as gr]
            [grenada.transformers :as gr-trans]
            [grenada.things :as t]
            [grimoire.api :as grim]
            [grimoire.either :as either]
            [grimoire.api
             [fs :as api.fs]]
            grimoire.api.fs.read))

;;; In this example I will read in the Clojure documentation as provided by
;;; Grimoire and convert it to the Grenada format. That doesn't sound much, but
;;; will require a bunch of transformation work, I guess.
;;;
;;; Prepare for this by running
;;;
;;;   $ git clone https://github.com/clojure-grimoire/doc-clojure-core.git
;;;   $ git clone https://github.com/clojure-grimoire/datastore.git
;;;
;;; in the project root directory. You should now have the directories
;;; doc-clojure-core and datastore in the same directory as the project.clj.
;;;
;;; If that goes reasonably well, I will also read in the extra documentation
;;; from Thalia and merge it with the Grimoire data.

(clojure.tools.namespace.repl/set-refresh-dirs
  "src"
  "dev"
  "checkouts/grenada-lib/src")

(def ^:dynamic *concise-printing* true)

;; TODO: Add something like this to guten-tag. (RM 2015-07-05)
(defmethod clojure.pprint/simple-dispatch guten_tag.core.ATaggedVal [v]
  (print (if *concise-printing*
           "#g/t "
           "#guten/tag "))
  (pprint [(gt/-tag v) (gt/-val v)]))

(def lib-grim-config (api.fs/->Config "doc-clojure-core"
                                      "datastore"
                                      "datastore"))

(def everything
  (memoize
    (fn everything-fn []
      (->> (for [[i k] (plumbing/indexed [:group
                                          :artifact
                                          :version
                                          :platform
                                          :ns
                                          :def])]
             (grim/search lib-grim-config
                          (into [k] (repeat (inc i) :any))))
           (map either/result)
           plumbing/aconcat
           (remove #(= ".git" (safe-get % :name)))
           (map #(vector %
                         (either/result (grim/read-meta lib-grim-config %))))))))
