(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get]]
            [grenada.utils :refer [fnk*]]
            [grenada.sources.clj :as gr-clj-src]
            [grenada.core :as gr]
            [grenada.transformers :as gr-trans]
            [grenada.exporters :as gr-exp]
            [grenada.mergers :as gr-merge]
            [grenada.things :as t]
            poomoo.transformers))

(clojure.tools.namespace.repl/set-refresh-dirs
  "src"
  "dev"
  "checkouts/grenada-lib/src")

(def produce-skeleton-graph
  "Input nodes: dp-metadata ext-metadata-file"
  {:nicely-sorted
   (fnk* [dp-metadata] gr-trans/reorder-for-output)

   :stripped
   (fnk* [nicely-sorted] (map gr-trans/strip-all))

   :with-ext
   (fnk* [stripped] (map (gr-trans/apply-if
                           t/def?
                           (gr-trans/add-ext
                             :poomoo.ext/ex-raw
                             [{:name "" :content ""}]))))

   :export-res
   (fnk [with-ext ext-metadata-file]
     (gr-exp/pprint-fs-flat with-ext ext-metadata-file :overwrite))})

(def produce-jar-graph
  "Input nodes: dp-metadata ext-metadata-file hier-out-dir jar-out-dir
                jar-coords"
  {:ext-metadata
   (fnk* [ext-metadata-file] gr/read-metadata)

   :postprocessed
   (fnk [ext-metadata jar-coords]
     (map (gr-trans/apply-if t/def?
                             (gr-trans/transform-ext
                               :poomoo.ext/ex-raw
                               (poomoo.transformers/process-raw jar-coords)))
          ext-metadata))

   ;; TODO: We need a merger for examples. (RM 2015-06-24)
   :merged
   (fnk* [dp-metadata postprocessed] ((gr-merge/simple-merge)))
     ; Strange syntax because of fnk*.

   :exported-files
   (fnk* [merged hier-out-dir] gr-exp/fs-hier)

   :jar-export-res
   (fnk [exported-files hier-out-dir jar-out-dir jar-coords]
     (gr/jar-from-files hier-out-dir jar-out-dir jar-coords))})
     ; We have to export the files first, but don't need the actual argument.

(def get-common-config
  (memoize
    (fn []
      {:dp-metadata
       (gr-clj-src/clj-entity-src
         ['org.clojure/clojure "1.7.0"]
         ['clj-grenada/darkestperu "0.1.0-SNAPSHOT"]
         {:group "clj-grenada"
          :artifact "darkestperu"
          :version "0.1.0-SNAPSHOT"})

       :ext-metadata-file "darkestperu-examples-gt.edn"})))

(comment

  (refresh)
  (((graph/eager-compile produce-skeleton-graph)
    (get-common-config))
   :export-res)

  (refresh)
  (((graph/eager-compile produce-jar-graph)
   (merge (get-common-config)
          {:hier-out-dir "/home/erle/repos/dp-examples/grenada-data"
           :jar-out-dir "/home/erle/repos/dp-examples/target/grenada"
           :jar-coords {:group "org.clojars.rmoehn"
                        :artifact "darkestperu-examples"
                        :version "0.1.0-SNAPSHOT"}}))
   :jar-export-res)

  )
