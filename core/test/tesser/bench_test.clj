(ns tesser.bench-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check [clojure-test :refer :all]
                                [generators :as gen]
                                [properties :as prop]]
            [multiset.core :refer [multiset]]
            [criterium.core :refer [with-progress-reporting quick-bench bench]]
            [tesser.core :as t]
            [tesser.simple :as s]
            [tesser.utils :refer :all]
            [clojure.core.reducers :as r]
            [clojure.set :as set]))

(def n 1000000)

(defn long-ary []
  (->> #(rand-int n)
       repeatedly
       (take n)
       long-array))

(defn long-vec []
  (->> #(rand-int n)
       repeatedly
       (take n)
       vec))

(defn sep
  ([& args]
   (prn)
   (prn)
   (apply println args)
   (prn)))

(deftest ^:bench sum
  (sep "######   Simple sum   ########")
  (dorun
    (for [collf   [#'long-ary #'long-vec]
          reducef [#'reduce #'r/fold #'s/fold]]
      (let [coll    (@collf)
            reducef @reducef]
        (sep collf reducef)
        (quick-bench (reducef + coll))))))

(deftest ^:focus ^:bench map-filter-sum
  (sep "#######   map/filter/sum   #######")
  (dorun
    (for [collf  [#'long-ary #'long-vec]]
      (let [coll (@collf)]
        (sep collf "seq/reduce")
        (quick-bench (->> coll
                          (map inc)
                          (filter even?)
                          (reduce +)))

        (sep collf "reducers fold")
        (quick-bench (->> coll
                          (r/map inc)
                          (r/filter even?)
                          (r/fold +)))

        (sep collf "tesser")
        (quick-bench (->> (t/map inc)
                          (t/filter even?)
                          (t/fold +)
                          (t/tesser (partition-all-fast 16384 coll))))))))

; For profiling
(deftest ^:stress stress
  (let [a (long-vec)]
    (dotimes [i 10000000]
      (->> (t/map inc)
           (t/filter even?)
           (t/fold +)
           (t/tesser (partition-all-fast 1024 a))))))