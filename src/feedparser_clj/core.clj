(ns feedparser-clj.core)

(import '(com.sun.syndication.io SyndFeedInput XmlReader)
	'(java.net URL)
	'(java.io InputStreamReader)
	'(com.sun.syndication.feed.synd SyndFeed))

(defn -main [& args]
  (let [feedinput (new SyndFeedInput)
	xmlreader (new XmlReader (new URL (first args)))]
    (println (.build feedinput xmlreader))
    )
  )