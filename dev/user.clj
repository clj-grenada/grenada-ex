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
            [grimoire.things :as grim-t]
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

(def grim-tag->gren-tag {::grim-t/group    ::t/group
                         ::grim-t/artifact ::t/artifact
                         ::grim-t/version  ::t/version
                         ::grim-t/platform ::t/platfom
                         ::grim-t/ns       ::t/namespace
                         ::grim-t/def      ::t/find})

(defmulti grim-thing->gren-thing gt/tag)

(defmethod grim-thing->gren-thing ::grim-t/group [g]
  (->> (t/make-thing {:coords [(safe-get g :name)]})
       (t/attach-aspect t/def-for-aspect ::t/group)))

(defmethod grim-thing->gren-thing ::grim-t/artifact [a]
  (->> (t/make-thing {:coords [(plumbing/safe-get-in a [:parent :name])
                               (safe-get a :name)]})
       (t/attach-aspect t/def-for-aspect ::t/artifact)))

(defn get-all-coords [grim-thing]
  (let [gren-tag (safe-get grim-tag->gren-tag (gt/tag grim-thing))
        ncoords (plumbing/safe-get-in t/def-for-aspect [gren-tag :ncoords])]
    (-> (for [depth (reverse (range ncoords))
              :let [ks (-> (repeat depth :parent)
                           vec
                           (conj :name))]]
          (plumbing/safe-get-in grim-thing ks))
        vec)))

(defmethod grim-thing->gren-thing ::grim-t/def [d]
  (->> (t/make-thing ))
  )

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

(comment


  (grim-thing->gren-thing (first (second (everything))))

  (get-all-coords (first (first (everything))))

  )
