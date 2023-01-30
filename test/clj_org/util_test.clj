(ns clj-org.util-test
  (:require [clojure.test :refer [are deftest is testing]]
            [clj-org.test-util :refer [should=]]
            [clj-org.util :refer :all]))

(deftest vec*-test
  (are [inp expected]
    (is (= expected (apply vec* inp)))

    [:p ()] [:p]
    [:p (range 4)] [:p 0 1 2 3]))


(defn- always [x] true)
(defn- never [x] false)
(defn- even-number? [x] (and (number? x) (even? x)))
(defn- odd-number? [x] (and (number? x) (odd? x)))
(defn- starts-w-nine? [x] (and (coll? x)
                               (number? (first x))
                               (not= 9 (first x))))
(defn- starts-w-pre? [x]
  (and (coll? x) (not= (first x) :pre)))

(deftest selective-walk-test
  (testing "changes values in a trivial tree"
    (should= [2] (selective-walk inc always number? [1]))
    (should= (type [2]) (type (selective-walk inc always number? [1])))
    (should= [2 4 9] (selective-walk inc always number? [1 3 8]))
    (should= [2 [4 1 [10 30]] 9]
             (selective-walk inc always number? [1 [3 0 [9 29]] 8])))
  (testing "allows me to specify when to transform"
    (should= [1 3 9] (selective-walk inc always even-number? [1 3 8]))
    (should= [2 [4 0 [10 30]] 8]
             (selective-walk inc always odd-number? [1 [3 0 [9 29]] 8])))
  (testing "allows me to specify when to descend"
    (should= [2 [4 1 [9 29]] 9]
             (selective-walk inc starts-w-nine? number? [1 [3 0 [9 29]] 8])))
  (testing "allows me to avoid :pre sections of a hiccup parse tree"
    (should= [:p "CAPS" [:pre "not caps"] "CAPS"]
             (selective-walk clojure.string/upper-case starts-w-pre? string?
                             [:p "caps" [:pre "not caps"] "caps"])))
  (testing "should directly transform toplevel form if asked to"
    (should= "UPCASE"
             (selective-walk clojure.string/upper-case
                             never
                             always
                             "upcase")))
  (testing "shouldn't descend top form if I don't want it to"
    (should= "locase"
             (selective-walk clojure.string/upper-case
                             never
                             never
                             "locase"))))

(vec* :p ())
