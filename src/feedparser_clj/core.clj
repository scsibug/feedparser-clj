(ns feedparser-clj.core
  (:import [com.sun.syndication.io SyndFeedInput XmlReader]
           [java.net URL]
           [java.io InputStreamReader]
           [com.sun.syndication.feed.synd SyndFeed])
  (:gen-class))

(defrecord feed [authors author categories contributors copyright description
                 encoding entries feed-type image language link entry-links
                 published-date title uri])

(defrecord entry [authors author categories contents contributors description
                  enclosures link published-date title updated-date url])

(defrecord enclosure [length type uri])

(defrecord person [email name uri])

(defrecord category [name taxonomy-uri])

(defrecord content [type value])

(defrecord image [description link title url])

(defrecord link [href hreflang length rel title type])

(defn obj->enclosure "Create enclosure struct from SyndEnclosure"
  [e]
  (map->enclosure {:length (.getLength e)
                   :type   (.getType e)
                   :url    (.getUrl e)}))

(defn obj->content "Create content struct from SyndContent"
  [c]
  (map->content {:type  (.getType c)
                 :value (.getValue c)}))

(defn obj->link "Create link struct from SyndLink"
  [l]
  (map->link {:href     (.getHref l)
              :hreflang (.getHreflang l)
              :length   (.getLength l)
              :rel      (.getRel l)
              :title    (.getTitle l)
              :type     (.getType l)}))

(defn obj->category "Create category struct from SyndCategory"
  [c]
  (map->category {:name         (.getName c)
                  :taxonomy-uri (.getTaxonomyUri c)}))

(defn obj->person "Create a person struct from SyndPerson"
  [sp]
  (map->person {:email (.getEmail sp)
                :name  (.getName sp)
                :uri   (.getUri sp)}))

(defn obj->image "Create image struct from SyndImage"
  [i]
  (map->image {:description (.getDescription i)
               :link        (.getLink i)
               :title       (.getTitle i)
               :url         (.getUrl i)}))

(defn obj->entry "Create feed entry struct from SyndEntry"
  [e]
  (map->entry {:authors        (map obj->person    (seq (.getAuthors e)))
               :categories     (map obj->category  (seq (.getCategories e)))
               :contents       (map obj->content   (seq (.getContents e)))
               :contributors   (map obj->person    (seq (.getContributors e)))
               :enclosures     (map obj->enclosure (seq (.getEnclosures e)))
               :description    (if-let [d (.getDescription e)] (obj->content d))
               :author         (.getAuthor e)
               :link           (.getLink e)
               :published-date (.getPublishedDate e)
               :title          (.getTitle e)
               :updated-date   (.getUpdatedDate e)
               :uri            (.getUri e)}))

(defn obj->feed "Create a feed struct from a SyndFeed"
  [f]
  (map->feed  {:authors        (map obj->person   (seq (.getAuthors f)))
               :categories     (map obj->category (seq (.getCategories f)))
               :contributors   (map obj->person   (seq (.getContributors f)))
               :entries        (map obj->entry    (seq (.getEntries f)))
               :entry-links    (map obj->link     (seq (.getLinks f)))
               :image          (if-let [i (.getImage f)] (obj->image i))
               :author         (.getAuthor f)
               :copyright      (.getCopyright f)
               :description    (.getDescription f)
               :encoding       (.getEncoding f)
               :feed-type      (.getFeedType f)
               :language       (.getLanguage f)
               :link           (.getLink f)
               :published-date (.getPublishedDate f)
               :title          (.getTitle f)
               :uri            (.getUri f)}))

(defn- parse-internal [xmlreader]
  (let [feedinput (new SyndFeedInput)
        syndfeed  (.build feedinput xmlreader)]
    (obj->feed syndfeed)))

(defn string->url [s]
  (if (string? s)
    (URL. s)
    s))

(defn parse-feed "Get and parse a feed from a URL"
  ([feedsource]
   (parse-internal (XmlReader. (string->url feedsource))))
  ([feedsource content-type]
   (parse-internal (XmlReader. (string->url feedsource) content-type)))
  ([feedsource content-type lenient]
   (parse-internal (XmlReader. (string->url feedsource) content-type lenient)))
  ([feedsource content-type lenient default-encoding]
   (parse-internal (XmlReader. (string->url feedsource) content-type lenient default-encoding))))
