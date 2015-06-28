(defproject org.clj-grenada/grenada-ex "1.2.0-SNAPSHOT"
  :description "Examples for and experiments with Grenada"
  :url "https://github.com/clj-grenada/grenada-ex"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [org.clj-grenada/grenada-lib "0.2.0-SNAPSHOT"]
                 [prismatic/plumbing "0.4.4"]

                 ; dependencies to be scraped
                 [clj-grenada/darkestperu "0.1.0-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["dev"]}})
