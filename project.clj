(defproject clj-org "0.0.3"
  :description "A Parser for Emacs Org Mode Files"
  :url "https://github.com/eigenhombre/clj-org"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:dependencies []
                   :plugins [[jonase/eastwood "0.9.9"]
                             [lein-bikeshed "0.5.2"]
                             [lein-kibit "0.1.8"]]}})
