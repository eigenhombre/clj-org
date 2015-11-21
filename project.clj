(defproject clj-org "0.0.1-SNAPSHOT"
  :description "A Parser for Emacs Org Mode Files"
  :url "https://github.com/eigenhombre/clj-org"
  :license {:name "MIT"}
  :aliases {"autotest" ["spec" "-a"]
            "autoquiet" ["spec" "--format=progress" "-r" "v"]}
  :test-paths ["spec"]
  :profiles {:dev {:dependencies [[speclj "3.2.0"]]
                   :plugins [[speclj "3.2.0"]]}}
  :dependencies [[org.clojure/clojure "1.7.0"]])
