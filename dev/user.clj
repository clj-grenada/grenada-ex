(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.pprint :refer [pprint]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get safe-get-in ?>] :as plumbing]
            [guten-tag.core :as gt]
            [grenada.converters :as converters]
            [grimin.core :as grimin]
            [grimoire
             [api :as grim]
             [either :as either]]
            [grimoire.api.fs :as api.fs]))

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

(comment

  (->> (grimin/read-all-things lib-grim-config)
       (grimin/attach-meta lib-grim-config)
       (grimin/grim-with-meta->gren)
       last)

  (->> (grim/search lib-grim-config
                   [:def "org.clojure" "clojure" "1.6.0" "clj" "clojure.core" "re-seq"])
       either/result
       first
       (grim/list-examples lib-grim-config)
       either/result
       (map #(grim/read-example lib-grim-config %))
       (map (fn [x]
              (either/result x)))
       pprint)

  (grim/list-groups lib-grim-config)

  (grimin/read-all-things lib-grim-config)


  )
