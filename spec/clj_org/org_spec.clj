(ns clj-org.org-spec
  (:require [clj-org.org :refer :all]
            [clj-org.util :refer [vec*]]
            [speclj.core :refer :all]))


(defmacro describe-examples [right-fn left-fn & body]
  `(describe "Examples"
     ~@(for [[l r] (partition 2 body)]
         `(~'it ~l
            (~'should= (~right-fn ~r) (~left-fn ~l))))))


(describe-examples identity get-title
  "#+TITLE: a title\n"          "a title"
  "#+TITLE: a title\nx\n"       "a title"
  "yz\n\n#+TITLE: a title\nx\n" "a title")


(describe-examples identity split-headers-and-body
  "#+A\n"                       ["#+A\n" ""]
  "#+A\n#+B\n"                  ["#+A\n#+B\n" ""]
  "#+A\nStuff"                  ["#+A\n" "Stuff"]
  "\n#+A\nStuff"                ["\n#+A\n" "Stuff"]
  "#+A\n\n#+B\nStuff"           ["#+A\n\n#+B\n" "Stuff"]
  "#+A\nStuff\nMore\n"          ["#+A\n" "Stuff\nMore\n"]
  "#+A\n# comment\n#+B\nBody"   ["#+A\n# comment\n#+B\n" "Body"]
  "#+A\n#+CAPTION: jaz\n\nBody" ["#+A\n" "#+CAPTION: jaz\n\nBody"]
  "#+Z\n#+BEGIN_SRC bada\nX"    ["#+Z\n" "#+BEGIN_SRC bada\nX"]
  "#+Z\n#+ATTR_HTML bada\nX"    ["#+Z\n" "#+ATTR_HTML bada\nX"]
  "#+A\n#+HTML: jaz\n\nBody"    ["#+A\n" "#+HTML: jaz\n\nBody"]
  "#+A\n#+HTML_HEAD: jaz\n\nX"  ["#+A\n#+HTML_HEAD: jaz\n\n" "X"])


(describe-examples #(vec* :div %) convert-body-to-sections
  "* Sec1\n"                [[:h1 "Sec1"]]
  "* Sec1\n* Sec2\n"        [[:h1 "Sec1"] [:h1 "Sec2"]]
  "* Sec1\nX\n"             [[:h1 "Sec1"] "X\n"]
  "* Sec1\nX\n* Sec2\nY\n"  [[:h1 "Sec1"] "X\n" [:h1 "Sec2"] "Y\n"]
  "* Sec1\n** Sec1a\nX\n"   [[:h1 "Sec1"] [:h2 "Sec1a"] "X\n"]
  "* S1\nB1\n** S1a\nB1a\n" [[:h1 "S1"] "B1\n" [:h2 "S1a"] "B1a\n"]
  "* Sec1\nX\nY\n"          [[:h1 "Sec1"] "X\nY\n"]
  "NoSection\nL2\n"         ["NoSection\nL2\n"]
  "*bold* stuff\n* Sec1\n"  ["*bold* stuff\n" [:h1 "Sec1"]])


(describe-examples identity find-paragraphs
  "x"          [[:p "x"]]
  "x\n"        [[:p "x\n"]]
  "p1\n\np2"   [[:p "p1\n"] [:p "p2"]]
  "p1\n\np2\n" [[:p "p1\n"] [:p "p2\n"]])


(describe-examples identity linkify
  "nonlink"       ["nonlink"]
  "line1\nline2"  ["line1\nline2"]
  "[[a]]"         ["[[a]]"]  ;; it leaves 'caption' style links alone
  "[[a][b]]"      [[:a {:href "a"} "b"]]
  "zoop [[a][b]]" ["zoop " [:a {:href "a"} "b"]]
  "[[a][b]] pooz" [[:a {:href "a"} "b"] " pooz"]
  "x [[a][b]] y"  ["x " [:a {:href "a"} "b"] " y"]
  "x [[a][b]] y [[c][d]] z" ["x "
                             [:a {:href "a"} "b"]
                             " y "
                             [:a {:href "c"} "d"]
                             " z"])


(describe-examples identity captionify
  "nonlink"       ["nonlink"]
  "line1\nline2"  ["line1\nline2"]
  "[[a]]"         [[:a {:href "a"} [:img {:src "a" :class "caption"}]]]
  "zoop [[a]]"    ["zoop " [:a {:href "a"} [:img {:src "a" :class "caption"}]]]
  "[[a]] pooz"    [[:a {:href "a"} [:img {:src "a" :class "caption"}]] " pooz"]
  "x [[b]] y"     ["x " [:a {:href "b"}
                         [:img {:src "b" :class "caption"}]] " y"]
  "x [[a]] y [[b]] z"
  ["x "
   [:a {:href "a"} [:img {:src "a" :class "caption"}]]
   " y "
   [:a {:href "b"} [:img {:src "b" :class "caption"}]]
   " z"])


(describe-examples identity boldify
  "zazza"         ["zazza"]
  "line1\nline2"  ["line1\nline2"]
  "*strong*"      [[:strong "strong"]]
  "good *stuff*"  ["good " [:strong "stuff"]]
  "*good* stuff"  [[:strong "good"] " stuff"])


(describe-examples identity emify
  "zazza"                          ["zazza"]
  "line1\nline2"                   ["line1\nline2"]
  "/em/"                           [[:em "em"]]
  "good /stuff/"                   ["good " [:em "stuff"]]
  "/good/ stuff"                   [[:em "good"] " stuff"]
  "an \"/em quote/\""              ["an \"" [:em "em quote"] "\""]
  "http://foo"                     ["http://foo"]
  "http://bit.ly/simple-made-easy" ["http://bit.ly/simple-made-easy"])


(describe-examples identity tree-emify
  "no em part"            "no em part"
  "xxx /yy/ zzz"          [:span "xxx " [:em "yy"] " zzz"]
  [:p "gimme /more/ em!"] [:p [:span "gimme " [:em "more"] " em!"]])


(describe-examples identity code-ify
  "aaabbb"         ["aaabbb"]
  "l1\nl2"         ["l1\nl2"]
  "=code="         [[:code "code"]]
  "a =code="       ["a " [:code "code"]]
  "=code= red"     [[:code "code"] " red"]
  "l1\nl2 =x=\n z" ["l1\nl2 " [:code "x"] "\n z"])


(describe-examples identity strike-ify
  "aabb"           ["aabb"]
  "l1\nl2"         ["l1\nl2"]
  "1 + 2 + 3"      ["1 + 2 + 3"]
  "+strike+"       [[:strike "strike"]]
  "a +strike+"     ["a " [:strike "strike"]]
  "+strike+ me"   [[:strike "strike"] " me"]
  "l1\nl2 +x+\n z" ["l1\nl2 " [:strike "x"] "\n z"])


(describe-examples identity hr-ify
  "asdf"     ["asdf"]
  ;; "a - b"    ["a - b"]
  ;; "a -- b"   ["a &#x2013            ; b"]
  ;; "a --- b"  ["a &#x2014; b"]
  ;; "a ---- b" ["a &#x2014;- b"]
  ;; "----"     ["&#x2014;-"]
  "a\n-----\nb\n" ["a\n" [:hr] "\nb\n"])


(describe-examples identity srcify
  "asdf"                            ["asdf"]
  "#+BEGIN_SRC x\n123\n#+END_SRC\n" [[:pre
                                      [:code {:class "lang_x"}
                                       "123\n"]]]
  "#+begin_src x\n123\n#+end_src\n" [[:pre
                                      [:code {:class "lang_x"}
                                       "123\n"]]]
  "#+begin_SRC x\n123\n#+END_src\n" [[:pre
                                      [:code {:class "lang_x"}
                                       "123\n"]]]
  "asdf\n#+begin_SRC x\n123\n#+END_src\nasdf\n"
  ["asdf\n"
   [:pre
    [:code {:class "lang_x"}
     "123\n"]]
   "asdf\n"])


(describe-examples identity example-ify
  "asdf"                                  ["asdf"]
  "#+BEGIN_EXAMPLE\n123\n#+END_EXAMPLE\n" [[:pre "123\n"]]
  "#+BEGIN_EXAMPLE
<hr>
<script lang=\"ada\"></script>
#+END_EXAMPLE\n"                         [[:pre (str "&lt;hr&gt;\n&lt;script "
                                                     "lang=&quot;ada&quot;&gt;"
                                                     "&lt;/script&gt;\n")]])


(describe-examples identity dashify
  "aabb" "aabb"
  "-"    "-"
  "--"   "&#x2013;"
  "---"  "&#x2014;"
  "a--b" "a&#x2013;b")


(describe-examples identity tree-dashify
  "no dashes"             "no dashes"
  "xxx -- zzz"            "xxx &#x2013; zzz"
  [:p "xxx -- zzz"]       [:p "xxx &#x2013; zzz"])


(describe-examples identity get-plain-lists
  "a"                     [["a" nil]]
  "- a\n"                 [[nil "- a\n"]]
  "a\n- b\n"              [["a\n" "- b\n"]]
  "a\n- b\nc\n"           [["a\n" "- b\n"]
                           ["c\n" nil]]
  "a\n- b\nc\n- d\n"      [["a\n" "- b\n"]
                           ["c\n" "- d\n"]])


(describe-examples identity parse-plain-list
  "
- a
"      [:ul [:li "a\n"]]
"
- a
- b
"      [:ul [:li "a\n"] [:li "b\n"]]
"
- a
  - a1
"      [:ul [:li "a\n"] [:ul [:li "a1\n"]]]
"
- a
- b
  - a1
  - a2
"      [:ul
        [:li "a\n"]
        [:li "b\n"]
        [:ul
         [:li "a1\n"]
         [:li "a2\n"]]]
"
- a
  - a1
  - a2
- b
"      [:ul
        [:li "a\n"]
        [:ul
         [:li "a1\n"]
         [:li "a2\n"]]
        [:li "b\n"]]
"
- a
- b
  - b1
  - b2
    - b2a
- c
"      [:ul
        [:li "a\n"]
        [:li "b\n"]
        [:ul
         [:li "b1\n"]
         [:li "b2\n"]
         [:ul
          [:li "b2a\n"]]]
        [:li "c\n"]]
"
- a
- b
  - b1
  - b2
    - b2a
  - b3
"      [:ul
        [:li "a\n"]
        [:li "b\n"]
        [:ul
         [:li "b1\n"]
         [:li "b2\n"]
         [:ul
          [:li "b2a\n"]]
         [:li "b3\n"]]]
;; Don't start new list item w/out intervening newline:
"- a - b\n" [:ul [:li "a - b\n"]]
;; Strange pathological minimum case (regression test):
"   - -- \n" [:ul [:li "-- \n"]]
)


(describe-examples identity tree-listify
  "a"                     "a"
  "- a\n"                 [:ul [:li "a\n"]]
  "a\n- b\n"              [:span "a\n" [:ul [:li "b\n"]]]
  "a\n- b\nc\n"           [:span "a\n" [:ul [:li "b\n"]] "c\n"]
  "a\n- b\nc\n- d\n"      [:span
                           "a\n"
                           [:ul [:li "b\n"]]
                           "c\n"
                           [:ul [:li "d\n"]]])
