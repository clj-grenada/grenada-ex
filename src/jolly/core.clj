(ns jolly.core
  (:require
    [plumbing.core :refer [fnk safe-get safe-get-in ?>] :as plumbing]
    [grenada
     [aspects :as a]
     [bars :as b]
     [things :as t]
     [utils :as g-utils]]
    [grenada.things.utils :as t-utils]
    jolly.bars
    [grimoire
     [api :as grim]
     [either :as either]
     [things :as grim-t]]
    grimoire.api.fs.read
    [guten-tag.core :as gt]))

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

(def read-all-things
  (memoize
    (fn read-all-things-fn [lib-grim-config]
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
           (remove #(= ".git" (safe-get % :name)))))))

;; REVIEW: New versions of lib-grimoire might allow examples on non-def Things.
;;         (RM 2015-07-25)
;;
;; Notes:
;;
;;  - The doc string of grimoire.api/list-examples is a bit strange. It sounds
;;    like list-examples would return Fail if there are no examples for a
;;    given Thing. This would be a behaviour I wouldn't approve of. – Returning
;;    a Success with an empty list would make more sense to me. Luckily this is
;;    indeed what happens.
;;
;;  - Don't be fooled by the doc string of grimoire.api/list-examples.
;;
(def read-examples
  (memoize
    (fn read-examples-fn [config thing]
      (if (grim-t/def? thing)
        (map #(assoc % :contents (either/result (grim/read-example config %)))
             (either/result (grim/list-examples config thing)))
        []))))

;; Note:
;;
;;  - This looks kind of similar to read-examples, but it doesn't have the
;;    grim-t/def? clause, because all Things support notes.
;;
;;  - Unfortunately it also doesn't do the thing that's more sensible than the
;;    doc string as list-examples does. (See note above.) It Fails if there are
;;    no notes on a group. So we need a different clause.
;;
(def read-notes
  (memoize
    (fn read-notes-fn [config thing]
      (let [notes (grim/list-notes config thing)]
        (if (or (not (grim-t/group? thing)) (either/succeed? notes))
          (map #(assoc % :contents (either/result (grim/read-note config %)))
               (either/result notes))
          [])))))

(defn maybe-attach
  "

  Works for examples and notes, since they have the same shape.

  sths … somethings"
  [bar-type-tag grim-sths gren-thing]
  (if-not (empty? grim-sths)
    (t/attach-bar jolly.bars/def-for-bar-type
                  bar-type-tag
                  (map #(t-utils/safe-select-keys % #{:name :contents})
                       grim-sths)
                  gren-thing)))

;; REFACTOR: Merge this with grim-with-meta->gren. It doesn't make sense to put
;;           :meta, :examples and :notes just do pull them out again in the next
;;           step. (RM 2015-07-25)
(def attach-meta
  (memoize
    (fn attach-meta-fn [lib-grim-config grim-things]
      (->> grim-things
           (map #(assoc %
                        :meta
                        (or (either/result (grim/read-meta lib-grim-config %))
                            {})))
           (remove #(and (grim-t/def? %)
                         (= :sentinel (safe-get-in % [:meta :type]))))
           (map #(assoc % :examples
                        (read-examples lib-grim-config %)))
           (map #(assoc % :notes
                        (read-notes lib-grim-config %)))))))

(defn grim-with-meta->gren [grim-things-with-meta]
  (map (fn map-fn [grim-t]
         (->> grim-t
              grim-thing->gren-thing
              (t/attach-bar b/def-for-bar-type
                            ::b/any
                            (t-utils/safe-get grim-t :meta))
              (maybe-attach :jolly.bars/examples
                            (t-utils/safe-get grim-t :examples))
              (maybe-attach :jolly.bars/notes
                            (t-utils/safe-get grim-t :notes))))
       grim-things-with-meta))
