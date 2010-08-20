(ns feedparser-clj.core
  (:gen-class))

(import '(com.sun.syndication.io SyndFeedInput XmlReader)
	'(java.net URL)
	'(java.io InputStreamReader)
	'(com.sun.syndication.feed.synd SyndFeed))

(defn -main "Show basic information for a feed, given a URL"
  [feedurl]
  (println "Using feed:" feedurl)
  (def myfeed (getFeed feedurl))
  (println "Found" (count (.getEntries myfeed)) "entries")
  )

(defn getFeed "Get a SyndFeed object from a URL"
  [feedurl]
  (let [feedinput (new SyndFeedInput)
        xmlreader (new XmlReader (new URL feedurl))]
    (.build feedinput xmlreader))
  )
