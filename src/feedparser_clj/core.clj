(ns feedparser-clj.core
  (:import (com.sun.syndication.io SyndFeedInput XmlReader)
           (java.net URL)
           (java.io InputStreamReader)
           (com.sun.syndication.feed.synd SyndFeed))
  (:gen-class))

(defstruct feed :authors :categories :contributors :copyright :description
           :encoding :entries :feed-type :image :language :link :entry-links
           :published-date :title :uri)
(defstruct entry :authors :categories :contents :contributors :description
           :enclosures :link :published-date :title :updated-date :url)
(defstruct enclosure :length :type :uri)
(defstruct person :email :name :uri)
(defstruct category :name :taxonomyURI)
(defstruct content :type :value)
(defstruct image :description :link :title :url)
(defstruct link :href :hreflang :length :rel :title :type)

(defn make-enclosure "Create enclosure struct from SyndEnclosure"
  [e]
  (struct-map enclosure :length (.getLength e) :type (.getType e)
              :url (.getUrl e)))

(defn make-content "Create content struct from SyndContent"
  [c]
  (struct-map content :type (.getType c) :value (.getValue c)))

(defn make-link "Create link struct from SyndLink"
  [l]
  (struct-map link :href (.getHref l) :hreflang (.getHreflang l)
              :length (.getLength l) :rel (.getRel l) :title (.getTitle l)
              :type (.getType l)))

(defn make-category "Create category struct from SyndCategory"
  [c]
  (struct-map category :name (.getName c)
              :taxonomyURI (.getTaxonomyUri c)))

(defn make-person "Create a person struct from SyndPerson"
  [sp]
  (struct-map person :email (.getEmail sp)
              :name (.getName sp)
              :uri (.getUri sp)))

(defn make-image "Create image struct from SyndImage"
  [i]
  (struct-map image :description (.getDescription i)
              :link (.getLink i)
              :title (.getTitle i)
              :url (.getUrl i)))

(defn make-entry "Create feed entry struct from SyndEntry"
  [e]
  (struct-map entry :authors (map make-person (seq (.getAuthors e)))
              :categories (map make-category (seq (.getCategories e)))
              :contents (map make-content (seq (.getContents e)))
              :contributors (map make-person (seq (.getContributors e)))
              :description (if-let [d (.getDescription e)] (make-content d))
              :enclosures (map make-enclosure (seq (.getEnclosures e)))
              :link (.getLink e)
              :published-date (.getPublishedDate e)
              :title (.getTitle e)
              :updated-date (.getUpdatedDate e)
              :uri (.getUri e)))

(defn make-feed "Create a feed struct from a SyndFeed"
  [f]
  (struct-map feed :authors (map make-person (seq (.getAuthors f)))
              :categories (map make-category (seq (.getCategories f)))
              :contributors (map make-person (seq (.getContributors f)))
              :copyright (.getCopyright f)
              :description (.getDescription f)
              :encoding (.getEncoding f)
              :entries (map make-entry (seq (.getEntries f)))
              :feed-type (.getFeedType f)
              :image (if-let [i (.getImage f)] (make-image i))
              :language (.getLanguage f)
              :link (.getLink f)
              :entry-links (map make-link (seq (.getLinks f)))
              :published-date (.getPublishedDate f)
              :title (.getTitle f)
              :uri (.getUri f)))

(defn- parse-internal [xmlreader]
  (let [feedinput (new SyndFeedInput)
        syndfeed (.build feedinput xmlreader)]
    (make-feed syndfeed)))

(defn parse-feed "Get and parse a feed from a URL"
  ([feedsource]
     (parse-internal (new XmlReader (if (string? feedsource)
                                      (URL. feedsource)
                                      feedsource))))
  ([feedsource content-type]
     (parse-internal (new XmlReader feedsource content-type))))

(defn -main "Show basic information for a feed, given a URL"
  [feedsource]
  (println "Using feed:" feedsource)
  (let [myfeed (parse-feed feedsource)]
    (println "Found" (count (:entries myfeed)) "entries")
    (println myfeed)))
