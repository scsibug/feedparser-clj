(ns feedparser-clj.core
  (:import (com.sun.syndication.io SyndFeedInput XmlReader)
           (java.net URL)
           (java.io InputStreamReader)
           (com.sun.syndication.feed.synd SyndFeed))
  (:gen-class))

;; A Syndicated Feed
(defstruct feed :authors
           :categories
           :contributors
           :copyright
           :entries)

;; A Feed Entry
(defstruct entry :authors
           :categories
           :contents
           :description)

;; A Person
(defstruct person :email :name :uri)

(defn -main "Show basic information for a feed, given a URL"
  [feedurl]
  (println "Using feed:" feedurl)
  (def myfeed (getSyndFeed feedurl))
  (println "Found" (count (.getEntries myfeed)) "entries")
  )

(defn getSyndFeed "Get a SyndFeed object from a URL"
  [feedurl]
  (let [feedinput (new SyndFeedInput)
        xmlreader (new XmlReader (new URL feedurl))]
    (.build feedinput xmlreader))
  )

(defn getFeed "Get a feed from a URL"
  [feedurl]
  (let [feedinput (new SyndFeedInput)
        xmlreader (new XmlReader (new URL feedurl))
        syndfeed (.build feedinput xmlreader)]
    (struct-map feed :authors (map make-personstruct (seq (.getAuthors syndfeed))))
    )
  )

(defn make-personstruct "Create a person struct from SyndPerson"
  [sp]
  (struct-map person :email (.getEmail sp)
              :name (.getName sp)
              :uri (.getUri sp))
  )