(defproject clj-org "0.0.3"
  :description "A Parser for Emacs Org Mode Files"
  :url "https://github.com/eigenhombre/clj-org"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:dependencies []
                   :plugins [[jonase/eastwood "0.9.9"]
                             [lein-bikeshed "0.5.2"]
                             [lein-file-replace "0.1.0"]
                             [lein-kibit "0.1.8"]]}}
  :deploy-repositories [["releases" :clojars]]
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version"
                   "release"]
                  ["file-replace" "README.md"
                   "\\[eigenhombre/clj-org \"" "\"]"
                   "version"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
