(defproject org.clj-grenada/grenada-ex "1.2.0-SNAPSHOT"
  :description "Examples for and experiments with Grenada"
  :url "https://github.com/clj-grenada/grenada-ex"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clj-grenada/lib-grenada "0.2.1"]
                 [prismatic/plumbing "0.4.4"]
                 [me.arrdem/guten-tag "0.1.4"]
                 [org.clj-grenada/jolly "0.1.0-SNAPSHOT"]
                 ;; Remove the (remove … ".git") when bumping.
                 [org.clojure-grimoire/lib-grimoire "0.10.2"]

                 ; dependencies to be scraped – should go in dev profile
                 [clj-grenada/darkestperu "0.1.0-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["dev"]}})
