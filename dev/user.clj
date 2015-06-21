(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get]]
            [grenada-lib.util :refer [fnk*]]
            [grenada-lib.sources.clj :as gr-clj-src]
            [grenada-lib.core :as gr]
            [grenada-lib.transformers :as gr-trans]
            [grenada-lib.exporters :as gr-exp]
            [grenada-lib.mergers :as gr-merge]
            poomoo.transformers))

;; TODO: This has to become a Guten-tag predicate. (RM 2015-06-21)
(defn def? [m]
  (= (safe-get m :level) :grimoire.things/def))

(def produce-skeleton-graph
  "Input nodes: dp-metadata"
  {:stripped
   (fnk* [dp-metadata] (map gr-trans/strip-all))

   :with-ext
   (fnk* [stripped] (map (gr-trans/apply-if
                           def?
                           (gr-trans/add-ext
                             :poomoo.ext/ex-raw
                             [{:name "" :content ""}]))))

   :nicely-sorted
   (fnk* [with-ext] gr-trans/reorder-for-output)

   :export-res
   (fnk [nicely-sorted]
     (gr-exp/pprint-fs-flat nicely-sorted "darkestperu-examples.edn"))})

(def produce-jar-graph
  "Input nodes: dp-metadata ext-metadata-file hier-out-dir jar-out-dir
                jar-coords"
  {:ext-metadata
   (fnk* [ext-metadata-file] grenada-lib.util/read-metadata)

   :postprocessed
   (fnk* [ext-metadata]
     (map (gr-trans/transform-ext :poomoo.ext/ex-raw
                                   poomoo.transformers/process-raw)))

   :merged
   (fnk* [dp-metadata postprocessed] gr-merge/simple-merge)

   :exported-files
   (fnk* [:merged :hier-out-dir] gr-exp/fs-hier)

   :jar-export-res
   (fnk* [:hier-out-dir :jar-out-dir :jar-coords] gr/jar-from-files)})

(comment

  (refresh)
  (((graph/eager-compile produce-skeleton-graph)
    {:dp-metadata
     (gr-clj-src/clj-entity-src
       ['org.clojure/clojure "1.7.0-RC2"]
       ['clj-grenada/darkestperu "0.1.0-SNAPSHOT"]
       {:group "clj-grenada"
        :name "darkestperu"
        :version "0.1.0-SNAPSHOT"})})
   :export-res)

  )
