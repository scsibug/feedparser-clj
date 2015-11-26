(ns feedparser-clj.core
  (:import (com.sun.syndication.io SyndFeedInput XmlReader WireFeedInput)
           (java.net URL)
           (java.io Reader InputStream File)
           (com.sun.syndication.feed.synd SyndFeedImpl SyndFeed SyndEntry SyndImage SyndPerson SyndCategory SyndLink SyndContent SyndEnclosure)
           (javax.xml XMLConstants)))


(defrecord feed [authors categories contributors copyright description
                 encoding entries feed-type image language link entry-links
                 published-date title uri])

(defrecord entry [authors categories contents contributors description
           enclosures link published-date title updated-date uri])

(defrecord enclosure [length type uri])

(defrecord person [email name uri])

(defrecord category [name taxonomyURI])

(defrecord content [type value])

(defrecord image [description link title url])

(defrecord link [href hreflang length rel title type])

(defn make-enclosure "Create enclosure struct from SyndEnclosure"
  [^SyndEnclosure e]
  (map->enclosure {:length (.getLength e) :type (.getType e)
                   :url (.getUrl e)}))

(defn make-content "Create content struct from SyndContent"
  [^SyndContent c]
  (map->content {:type (.getType c) :value (.getValue c)}))

(defn make-link "Create link struct from SyndLink"
  [^SyndLink l]
  (map->link {:href (.getHref l) :hreflang (.getHreflang l)
              :length (.getLength l) :rel (.getRel l) :title (.getTitle l)
              :type (.getType l)}))

(defn make-category "Create category struct from SyndCategory"
  [^SyndCategory c]
  (map->category {:name (.getName c)
                  :taxonomyURI (.getTaxonomyUri c)}))

(defn make-person "Create a person struct from SyndPerson"
  [^SyndPerson sp]
  (map->person {:email (.getEmail sp)
                :name (.getName sp)
                :uri (.getUri sp)}))

(defn make-image "Create image struct from SyndImage"
  [^SyndImage i]
  (map->image {:description (.getDescription i)
               :link (.getLink i)
               :title (.getTitle i)
               :url (.getUrl i)}))

(defn make-entry "Create feed entry struct from SyndEntry"
  [^SyndEntry e]
  (map->entry {:authors (map make-person (seq (.getAuthors e)))
               :categories (map make-category (seq (.getCategories e)))
               :contents (map make-content (seq (.getContents e)))
               :contributors (map make-person (seq (.getContributors e)))
               :description (if-let [d (.getDescription e)] (make-content d))
               :enclosures (map make-enclosure (seq (.getEnclosures e)))
               :link (.getLink e)
               :published-date (.getPublishedDate e)
               :title (.getTitle e)
               :updated-date (.getUpdatedDate e)
               :uri (.getUri e)}))

(defn make-feed "Create a feed struct from a SyndFeed"
  [^SyndFeed f]
  (map->feed  {:authors (map make-person (seq (.getAuthors f)))
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
               :uri (.getUri f)}))

(defn ^WireFeedInput gen-feed-input
  []
  (proxy [WireFeedInput] []
    (createSAXBuilder []
      (doto (proxy-super createSAXBuilder)
        (.setFeature XMLConstants/FEATURE_SECURE_PROCESSING true)
        (.setFeature "http://apache.org/xml/features/disallow-doctype-decl" true)))))

(defn ^SyndFeedInput gen-syndfeedinput
  []
  (proxy [SyndFeedInput] []
      (build [^Reader rdr]
        (SyndFeedImpl. (.build (gen-feed-input) rdr) false))))

(defn- parse-internal [^XmlReader xmlreader]
  (let [feedinput (gen-syndfeedinput)
        syndfeed (.build feedinput xmlreader)]
    (make-feed syndfeed)))

(defn parse-feed "Get and parse a feed from a URL"
  ([feedsource]
     (parse-internal (cond
                       (string? feedsource) (XmlReader. (URL. feedsource))
                       (instance? InputStream feedsource) (XmlReader. ^InputStream feedsource)
                       (instance? File feedsource) (XmlReader. ^File feedsource)
                       :else (throw (ex-info "Unsupported source" {:source feedsource
                                                                   :type (type feedsource)})))))
  ([feedsource content-type]
     (parse-internal (new XmlReader ^InputStream feedsource content-type))))
