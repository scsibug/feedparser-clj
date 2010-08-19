(ns feedparser-clj.core
  (:gen-class))

(import '(com.sun.syndication.io SyndFeedInput XmlReader)
	'(java.net URL)
	'(java.io InputStreamReader)
	'(com.sun.syndication.feed.synd SyndFeed))

(defn -main [feedurl]
  (println (str "Using feed: " feedurl))
  (let [feedinput (new SyndFeedInput)
	xmlreader (new XmlReader (new URL feedurl))]
    (println (.build feedinput xmlreader))
    )
  )