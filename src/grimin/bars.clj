(ns grimin.bars
  "Bar types for holding Grimoire data."
  (:require [grenada.things.def :as things.def]
            [schema.core :as s]))

(def examples-def
  "Definition of the Bar type `::examples`.

  ## Model

  Bars of this type can hold examples taken from a **Grimoire** data store. For
  the shape, see the Schema below.

   - `:name` is the name of the example. – We take this from lib-grimoire, so
     it's probably the file name and won't be fancy.

   - `:version` is the version coordinate of the Thing the example was
     originally added to. This is a Grimoire peculiarity: when you add an
     example to a Thing, it means you're adding it to this Thing and all later
     versions of it.

     So if you ask for the examples of `clojure.core/map` in JVM Clojure 1.7.0,
     you will also get examples for `clojure.core/map` in JVM Clojure 1.6.0,
     1.5.0 etc.

   - `:name`, `:version` of an example and the coordinates of the Thing it is
     attached to are expected to uniquely identify the example within one
     version of a Grenada artifact.

   - The `:contents` are simply the data returned by
     clj::grimoire.api/read-example (minus `Success` wrapper).

  ## Prerequisites

  None. Can be attached to any Thing.

  ## Remarks

  Grimoire examples have a handle, which in the FS implementation is their
  absolute path, as far as I understand. And they are `assoc`ed with their path
  relative to the root of the examples store. This doesn't make much sense to
  me. The handle is supposed to be unique, but if it's an absolute path on the
  individual machine, it's too unique. And the relative path is redundant with
  the parent field. I guess these things are required internally, but they're
  not helpful in a Datadoc JAR, which is why I leave them out."
  (things.def/map->bar-type
    {:name ::examples
     :schema [{:name s/Str
               :version s/Str
               :contents s/Str}]}))

(def def-for-bar-type
  #{examples-def})
