(defproject feedparser-clj "0.1.0-SNAPSHOT"
  :description "Parse RSS/Atom feeds with a simple, clojure-friendly API."
  :dependencies [[org.clojure/clojure "1.2.0-RC3"]
                 [org.clojure/clojure-contrib "1.2.0-RC3"]
		 [lein-run "1.0.0-SNAPSHOT"]
		 [org.jdom/jdom "1.1"]
		 [rome/rome "1.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :main feedparser-clj.core)