(ns clj-org.util)


(defn vec*
  "
  Like list*, but for vectors.  (vec* :a :b [:c :d]) => [:a :b :c :d].
  "
  [& args]
  (let [l (last args)
        bl (butlast args)]
    (vec (concat bl l))))


(defn selective-walk
  "
  Walk tree recursively, descending into subtrees only when descend?
  on the subtree is truthy, and transforming elements only when
  transform? is truthy.
  FIXME: generalize to maps like clojure.walk does?  Currently, the
  supplied descend? function should reject maps if they are in the
  supplied form.
  "
  [action descend? transform? form]
  (let [walk-fn
        (fn [el]
          (cond
            (transform? el) (action el)
            (and (coll? el)
                 (descend? el)) (selective-walk action descend? transform? el)
                 :else el))]
    (cond
      (descend? form) (into (empty form) (map walk-fn form))
      (transform? form) (action form)
      :else form)))
