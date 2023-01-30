(ns clj-org.test-util
  (:require [clojure.test :refer [are deftest is]]))

(defn should= [& args]
  (is (apply = args)))

(def ^:private counter (atom 0))

(defn test-name-symbol []
  (let [ret (format "examples-%d-test" @counter)]
    (swap! counter inc)
    (symbol ret)))

(defmacro describe-examples [right-fn left-fn & body]
  `(deftest ~(test-name-symbol)
     (are [~'lhs ~'rhs]
       (is (= (~left-fn ~'lhs) (~right-fn ~'rhs)))

       ~@body)))


