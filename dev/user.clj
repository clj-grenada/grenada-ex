(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.pprint :refer [pprint]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get safe-get-in ?>] :as plumbing]
            [guten-tag.core :as gt]
            [grenada.utils :refer [fnk*]]
            [grenada.core :as gr]
            [grenada.transformers :as gr-trans]
            [grenada
             [aspects :as a]
             [bars :as b]
             [things :as t]]
            [grenada.things.utils :as t-utils]
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

;; A bit repetitive, but at least it allows me to easily check whether an
;; I've covered all the cases and whether there is anything foul in the state of
;; the input data.
(def grim-tag->gren-tag {::grim-t/group     ::t/group
                         ::grim-t/artifact  ::t/artifact
                         ::grim-t/version   ::t/version
                         ::grim-t/platform  ::t/platform
                         ::grim-t/namespace ::t/namespace
                         ::grim-t/def       ::t/find})

(defn get-all-coords [grim-thing]
  (let [gren-tag (safe-get grim-tag->gren-tag (gt/tag grim-thing))
        ncoords (safe-get-in t/def-for-aspect [gren-tag :ncoords])]
    (-> (for [depth (reverse (range ncoords))
              :let [ks (-> (repeat depth :parent)
                           vec
                           (conj :name))]]
          (safe-get-in grim-thing ks))
        vec)))

(defn determine-aspects [d]
  (let [meta-m (safe-get d :meta)
        t      (safe-get meta-m :type)]
   (case t
    :fn      [::a/var-backed ::a/fn]
    :macro   [::a/var-backed ::a/macro]
    :var     [::a/var-backed]
    :special (-> [::a/special]
                 (?> (get meta-m :special-form) (conj ::a/var-backed))
                 (?> (get meta-m :macro)        (conj ::a/macro)))

    (throw (IllegalArgumentException.
             (str "Unknown type of def: " t))))))

(defmulti grim-thing->gren-thing gt/tag)

(defmethod grim-thing->gren-thing ::grim-t/def [d]
  (->> (t/map->thing {:coords (get-all-coords d)})
       (t/attach-aspect t/def-for-aspect ::t/find)
       (t/attach-aspects a/def-for-aspect (determine-aspects d))))

(defmethod grim-thing->gren-thing :default [t]
  (if-let [gren-tag (get grim-tag->gren-tag (gt/tag t))]
    (->> (t/map->thing {:coords (get-all-coords t)})
         (t/attach-aspect t/def-for-aspect gren-tag))
    (throw (IllegalArgumentException.
             (str "Unknown Grimoire type of Grimoire thing: " (pr-str t))))))

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
           (map #(assoc %
                        :meta
                        (or (either/result (grim/read-meta lib-grim-config %))
                            {})))
           (remove #(and (grim-t/def? %)
                         (= :sentinel (safe-get-in % [:meta :type]))))))))

(def everything-grenada
  (memoize
    (fn everything-grenada-fn []
      (map (fn map-fn [grim-t]
             (->> grim-t
                  grim-thing->gren-thing
                  (t/attach-bar b/def-for-bar-type
                                ::b/any
                                (t-utils/safe-get grim-t :meta))))
           (everything)))))

(comment

  (grim/list-groups lib-grim-config)

  (keys (first (everything)))


  (everything-grenada)


  (get-all-coords (first (first (everything))))

  )
