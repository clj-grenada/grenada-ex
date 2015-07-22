(ns user
  (:require [clojure.java.io :as io]
            [clojure.repl :refer [pst doc find-doc]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [plumbing.graph :as graph]
            [plumbing.core :refer [fnk safe-get]]
            [grenada.utils :refer [fnk*]]
            [grenada.core :as gr]
            [grenada.transformers :as gr-trans]
            [grenada.things :as t]))

(clojure.tools.namespace.repl/set-refresh-dirs
  "src"
  "dev"
  "checkouts/grenada-lib/src")
