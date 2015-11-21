(defproject clj-org "0.0.2"
  :description "A Parser for Emacs Org Mode Files"
  :url "https://github.com/eigenhombre/clj-org"
  :license {:name "MIT"}
  :aliases {"autotest" ["spec" "-a"]
            "autoquiet" ["spec" "--format=progress" "-r" "v"]}
  :test-paths ["spec"]
  :source-paths ["src" "spec"]
  :profiles {:dev {:dependencies [[speclj "3.3.1"]]
                   :plugins [[speclj "3.3.1"]]}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [hiccup "1.0.5"]])
