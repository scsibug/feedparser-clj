(ns feedparser-clj.core)

(import '(com.sun.syndication.io SyndFeedInput XmlReader)
	'(java.net URL)
	'(java.io InputStreamReader)
	'(com.sun.syndication.feed.synd SyndFeed))

(defn -main [& args]
  (println "Hello world!")
  )
