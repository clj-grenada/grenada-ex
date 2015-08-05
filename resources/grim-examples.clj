These are taken as test input from http://conj.io/contributing.

```clojure
4
;; => 4

(inc  [1])
;; Ouch!
;; => ClassCastException clojure.lang.PersistentVector cannot be cast to
;; java.lang.Number

(read)
;; < (1 2 3)
;; => (1 2 3)

(do  (.write *err* "To error!\n")
     (.write *out* "To out!\n")
    nil)
;; >> To error!
;; > To out!
;; => nil

(print  (meta #'clojure.core/for))
;; => {:arglists ([seq-exprs body-expr]),
;;     :added 1.0,
;;     :line 4444,
;;     :column 1,
;;     :file clojure/core.clj,
;;     :name for,
;;     :macro true}
```
