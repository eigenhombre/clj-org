(ns clj-org.org
  (:require [clj-org.util :refer [vec* selective-walk]]
            [clojure.zip :as zip]
            [hiccup.util :refer [escape-html]]))

(defn header-value-for [field txt]
  (->> txt
       (re-seq (->> field
                    (format "\\#\\+%s: (.+?)\n")
                    re-pattern))
       (map second)
       last))

(defn get-title [txt] (header-value-for "TITLE" txt))

(defn get-draft [txt]
  (->> txt (header-value-for "DRAFT") Boolean/valueOf))

(defn get-tags [txt] (header-value-for "TAGS" txt))

(defn strip-raw-html-tags-for-now [txt]
  (-> txt
      (clojure.string/replace #"#\+(?:HTML|ATTR_HTML):.+?\n" "")
      (clojure.string/replace #"(?sx)
                                \#\+BEGIN_HTML\s*
                                .*?
                                \#\+END_HTML\s*" "")))

(defn ^:private descend? [el]
  (and (coll? el)
       (not (string? el))
       (not (map? el))
       (not= :pre (first el))))

(defn apply-fn-to-strings
  "
  Walk tree, applying f to each string.  If multiple terms result, put
  them inside a :span tag.
  "
  [f tree]
  (let [f (fn [el]
            (let [[r0 & rs :as r] (f el)]
              (cond
                (string? r) r
                rs (vec* :span r)
                :else r0)))]
    (selective-walk f descend? string? tree)))

(defn split-headers-and-body [txt]
  (let [[_ & rs]
        (re-find #"(?x)
                   (\n*     # Pick up trailing newlines
                    (?:\#   # Anything starting w/ '#'
                     (?:    # Not starting with:
                      (?!\+(?:HTML:|CAPTION|BEGIN|ATTR_HTML))
                            # Swallow all lines that match
                      .)+\n*)*)
                            # Swallow everything else as group 2
                   ((?s)(?:.+)*)"
                 txt)]
    rs))

(defn convert-body-to-sections [body]
  (let [matches
        (re-seq #"(?x)
                  (?:
                    (?:
                      (\*+)
                      \s+
                      (.+)
                      \n
                    )|
                    (
                      (?:
                        (?!\*+\s+)
                        .*\n
                      )*
                    )
                  )"
                body)]
    (->> (for [[_ stars hdr body] matches]
           (if stars
             [(-> stars
                  count
                  ((partial str "h"))
                  keyword)
              hdr]
             body))
         (remove #{""})
         (vec* :div))))

(defn find-paragraphs [s]
  (->> s
       (re-seq #"(?x)
                 ((?:.+\n?)+)
                 |
                 (?:\n{2,})")
       (map (comp (partial vec* :p) rest))))

(defn captionify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!\[\[)
                     .
                   )+
                 )?
                 (?:
                   \[\[
                   (.+?)
                   \]\]
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before img]]
                 (cond
                   (not before) [[:a {:href img} [:img {:src img
                                                        :class "caption"}]]]
                   (not img) [before]
                   :else [before [:a {:href img}
                                  [:img {:src img
                                         :class "caption"}]]])))))

(defn linkify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!
                       \[\[.+?\]\[.+?\]\]
                     )
                     .
                   )+
                 )?
                 (?:
                   \[\[
                   (.+?)
                   \]\[
                   (.+?)
                   \]\]
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before lnk body]]
                 (cond
                   (not before) [[:a {:href lnk} body]]
                   (not lnk) [before]
                   :else [before [:a {:href lnk} body]])))))

(defn boldify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!\*)
                     .
                   )+
                 )?
                 (?:
                   \*
                   (
                     (?:
                       (?!\*)
                       .
                     )+?
                   )
                   \*
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before strong]]
                 (cond
                   (not before) [[:strong strong]]
                   (not strong) [before]
                   :else [before [:strong strong]])))))

(defn emify [s]
  (->> s
       (re-seq #"(?xs)
                 (
                   (?:
                     (?!
                       (?:
                         (?<=\s|^|\")
                         \/
                         ([^\/]+)
                         \/
                       )
                     )
                     .
                   )+
                 )?
                 (?:
                   (?<=\s|^|\")
                   \/
                   ([^\/]+)
                   \/
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before em]]
                 (cond
                   (not before) [[:em em]]
                   (not em) [before]
                   :else [before [:em em]])))))

(defn code-ify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!
                       =
                       (.+?)
                       =
                     )
                     .
                   )+
                 )?
                 (?:
                   =
                   (.+?)
                   =
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before code]]
                 (cond
                   (not before) [[:code code]]
                   (not code) [before]
                   :else [before [:code code]])))))

(defn strike-ify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!
                       \+
                       (?!\s+)
                       (.+?)
                       (?!\s+)
                       \+
                     )
                     .
                   )+
                 )?
                 (?:
                   \+
                   (?!\s+)
                   (.+?)
                   (?!\s+)
                   \+
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before strike]]
                 (cond
                   (not before) [[:strike strike]]
                   (not strike) [before]
                   :else [before [:strike strike]])))))

(defn hr-ify [s]
  (->> s
       (re-seq #"(?sx)
                 (
                   (?:
                     (?!
                       (?<=^|\n)
                       -{5,}
                     )
                     .
                   )+
                 )?
                 (
                   (?<=^|\n)
                   -{5,}
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before hr]]
                 (cond
                   (not before) [[:hr]]
                   (not hr) [before]
                   :else [before [:hr]])))))

(defn srcify [txt]
  (->> txt
       (re-seq #"(?xs)
                 (
                   (?:
                     (?!
                       \#\+(?i)\bBEGIN_SRC\b\s+
                       \S+
                       \n
                       .+?
                       \#\+(?i)\bEND_SRC\b\n
                     )
                     .
                   )+
                 )?
                 (?:
                   \#\+(?i)\bBEGIN_SRC\b\s+
                   (\S+)
                   \n
                   (.+?)
                   \#\+(?i)\bEND_SRC\b\n
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before lang block]]
                 (cond
                   (not before)
                   [[:pre [:code {:class (str "lang_" lang)}
                           block]]]
                   (not block) [before]
                   :else [before
                          [:pre
                           [:code {:class (str "lang_" lang)}
                            block]]])))))

(defn example-ify [txt]
  (->> txt
       (re-seq #"(?xs)
                 (
                   (?:
                     (?!
                       \#\+BEGIN_EXAMPLE\n
                       .+?
                       \#\+END_EXAMPLE\n
                     )
                     .
                   )+
                 )?
                 (?:
                   \#\+BEGIN_EXAMPLE\n
                   (.+?)
                   \#\+END_EXAMPLE\n
                 )?")
       (remove (partial every? empty?))
       (mapcat (fn [[_ before block]]
                 (cond
                   (not before) [[:pre (escape-html block)]]
                   (not block) [before]
                   :else [before [:pre (escape-html block)]])))))

(defn dashify [txt]
  (-> txt
      (clojure.string/replace #"---" "&#x2014;")
      (clojure.string/replace #"--" "&#x2013;")))

(defn get-plain-lists
  "
  Get plain lists and surrounding content out of txt.  Defer actual
  parsing of plain lists.
  "
  [txt]
  (->> txt
       (re-seq #"(?xs)
                 (
                  (?:
                   (?!
                    (?<=\n|^)
                    \ *
                    -
                    \ +
                    [^\n]
                    +\n
                    (?:
                     (?<=\n)
                     (?:\ *-\ +|\ +)
                     [^\n]+
                     \n
                    )*
                   )
                   .
                  )+
                 )?
                 (
                  (?<=\n|^)
                  \ *
                  -
                  \ +
                  [^\n]+
                  \n
                  (?:
                   (?<=\n)
                   (?:\ *-\ +|\ +)
                   [^\n]+
                   \n
                  )*
                 )?")
       (map rest)
       (remove (partial every? empty?))))

(defn items-seq-to-tree
  "
  Convert seq of [level, content] pairs into a tree using zippers.
  Assumes deltas are a whole multiple of two for now.
  "
  [s]
  (loop [[[level x] & more] s
         prev 0
         ret (-> [:ul] zip/vector-zip zip/down)]
    (if-not x
      (zip/root ret)  ;; We're done.
      ;; ... otherwise, figure out where in tree to insert node:
      (recur more level
             (let [delta (/ (- prev level) 2)]
               (cond
                 (> level prev) (-> ret
                                    (zip/insert-right [:ul])
                                    zip/right
                                    zip/down
                                    (zip/insert-right [:li x])
                                    zip/right)
                 (< level prev) (-> ret
                                    (#(last (take (inc delta)
                                                  (iterate zip/up %))))
                                    (zip/insert-right [:li x])
                                    zip/right)
                 :else ;; Simple case -- same level:
                 (-> ret
                     (zip/insert-right [:li x])
                     zip/right)))))))

(defn strip-leading-spaces
  "
  Strip leading spaces from every line in input.
  "
  [txt]
  (let [txt-lines (clojure.string/split txt #"\n")
        spaces-to-strip (->> txt-lines
                             (map (partial re-find #"^( *)"))
                             (map (comp count second))
                             (apply min))]
    (apply str
           (interleave
            (map (comp (partial apply str)
                       (partial drop spaces-to-strip))
                 txt-lines)
            (repeat \newline)))))

(defn parse-plain-list [txt]
  (->> txt
       strip-leading-spaces
       (re-seq #"(?xs)
                 (?<=\n|^)
                 (\ *)-\ +
                 (
                   (?:
                     (?!(?<=\n|^)\ *-\ )
                     .
                   )+
                 )")
       (map rest)
       (map (juxt (comp count first) second))
       items-seq-to-tree))

(defn plain-listify [txt]
  (->> txt
       get-plain-lists
       (mapcat (fn [[before-txt list-txt]]
                 (cond
                   (not before-txt) [(parse-plain-list list-txt)]
                   (not list-txt) [before-txt]
                   :else [before-txt (parse-plain-list list-txt)])))))

(defn tree-linkify [tree] (apply-fn-to-strings linkify tree))
(defn tree-captionify [tree] (apply-fn-to-strings captionify tree))
(defn tree-boldify [tree] (apply-fn-to-strings boldify tree))
(defn tree-emify [tree] (apply-fn-to-strings emify tree))
(defn tree-code-ify [tree] (apply-fn-to-strings code-ify tree))
(defn tree-strike-ify [tree] (apply-fn-to-strings strike-ify tree))
(defn tree-hr-ify [tree] (apply-fn-to-strings hr-ify tree))
(defn tree-srcify [tree] (apply-fn-to-strings srcify tree))
(defn tree-example-ify [tree] (apply-fn-to-strings example-ify tree))
(defn tree-pars [tree] (apply-fn-to-strings find-paragraphs tree))
(defn tree-dashify [tree] (apply-fn-to-strings dashify tree))
(defn tree-listify [tree] (apply-fn-to-strings plain-listify tree))

(defn ^:private txt->lines [txt]
  (clojure.string/split txt #"\n"))

(defn parse-org [txt]
  (let [title (get-title txt)
        [hdrs body] (split-headers-and-body txt)
        slurped-lines (-> txt escape-html txt->lines)
        content (-> body
                    strip-raw-html-tags-for-now
                    convert-body-to-sections
                    tree-srcify
                    tree-example-ify
                    tree-listify
                    tree-pars
                    tree-linkify
                    tree-captionify
                    tree-boldify
                    tree-emify
                    tree-code-ify
                    tree-strike-ify
                    tree-hr-ify
                    tree-dashify)]
    {:title title
     :headers hdrs
     :content content}))

(comment

  (parse-org "#+TITLE: This is an Org Mode file.

    * This is the outer section
    ** This is an inner section
    Inner section body -- /with italic text/!  And *bold text* too.

    - Plain List Item 1
    - Plain List Item 2
    [[http://eigenhombre.com][A link to a Web site]]
    ")
  ;;=>
  {:title "This is an Org Mode file.",
   :headers "\n#+TITLE: This is an Org Mode file.\n\n",
   :content
   [:div
    [:h1 [:p "This is the outer section"]]
    [:h2 [:p "This is an inner section"]]
    [:span
     [:p
      [:span
       [:span
        "Inner section body &#x2013; "
        [:em "with italic text"]
        "!  And "]
       [:strong "bold text"]
       " too.\n"]]
     [:ul
      [:li [:p "Plain List Item 1\n"]]
      [:li [:p "Plain List Item 2\n"]]]
     [:p
      [:span
       [:a {:href "http://eigenhombre.com"} "A link to a Web site"]
       "\n"]]]]})
