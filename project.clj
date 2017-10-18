(defproject org.clojars.quan/feedparser-clj "0.6.0"
  :description "a fork of scsibug/feedparser-clj, clojure-friendly RSS/Atom feed parser API."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.jdom/jdom2 "2.0.6"]
                 [com.rometools/rome "1.8.0"]]
  :deploy-repositories [["clojars" {:sign-releases false}]]
  :main feedparser-clj.core)
