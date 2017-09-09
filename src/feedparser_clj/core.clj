(ns feedparser-clj.core
  (require [base64-clj.core :as base64])
  (:import (com.rometools.rome.io SyndFeedInput XmlReader WireFeedInput)
           (com.rometools.modules.mediarss MediaEntryModule)
           (com.rometools.modules.mediarss.types Thumbnail Metadata MediaContent Reference UrlReference Category)
           (java.net URL HttpURLConnection)
           (java.io Reader InputStream File)
           (com.rometools.rome.feed.synd SyndFeedImpl SyndFeed SyndEntry SyndImage SyndPerson SyndCategory SyndLink SyndContent SyndEnclosure)
           (javax.xml XMLConstants)))

(defn- dasherize
  "Returns string `s` underscores replaced with hyphens"
  [s]
  (clojure.string/replace s #"_" "-"))

(defn- update-map-keys [f m]
  (reduce-kv (fn [hsh k v]
    (assoc hsh (f k) v)) {} m))

(defn- find-first
  [pred coll]
  (first (filter pred coll)))

(defn- merge-maps
  "Given a collection containing collections of maps, each collection's contents is merged
   into a single map."
  [colls]
  (map (partial apply merge) colls))

(defn- interleave-maps
  "Given [[{:a 1}{:a 2}][{:b 1}{:b 2}][{:c 1}{:c 2}]]
   the following would be returned:
     (({:a 1}{:b 1}{:c 1})({:a 2}{:b 2}{:c 2})({:a 3}{:b 3}{:c 3}))"
  [colls]
  (apply map list colls))

(defn- zip-merge-maps [colls]
  (merge-maps (interleave-maps colls)))

;; TODO normalize image paths e.g. //some.path.com should become http://some.path.com
(defn- children-with-namespace-prefix [children ns-prefix]
  (filter #(= (name ns-prefix) (.getNamespacePrefix %)) children))

(defn- foreign-elements [entry ns-prefix]
  (-> entry .getForeignMarkup (children-with-namespace-prefix ns-prefix)))

(defn- extra-attributes-map
  "Returns a map representation of extra element attributes where attribute
   names (turned into keywords) map to attribute values as strings."
   [extra-attributes]
   (reduce
     (fn [attr-map attribute]
       (assoc attr-map
              (keyword (.getQualifiedName attribute))
              (.getValue attribute)))
     {}
     extra-attributes))

(defn- element-value-tuples [elements]
  (map #(vector (.getName %)
                 (.getTextNormalize %)
                 (extra-attributes-map (.getAttributes %)))
        elements))

(defn- entry-elements [entry element-names ns-prefix]
  (->> (foreign-elements entry ns-prefix)
       element-value-tuples
       (filter (fn [[k & _]] (contains? element-names k)))))

(defn- extra-elements-map
  "Returns a list of element-maps (one for each entry), where each element-map
   maps element names each to a list of values parsed.

  Example: ({:picture_source (\"Chicago Tribune\" \"Made-up source\")
             :approx_traffic (\"1,000,000+\")})"
  [entries element-names ns-prefix]
  (->> entries
       (map (comp #(update-map-keys keyword %)
                  #(apply merge-with
                          concat
                          (map (fn [[extra-key extra-value extra-attributes]]
                                 {extra-key (list [extra-value
                                                   extra-attributes])}) %1))
                  #(entry-elements % element-names ns-prefix)))))

(defn- get-transform-from-specs
  "Returns the transform function specified in element-specs given the
   elem-key. If no transform function is found, returns the identity
   function."
  [element-specs elem-key]
  (or (->> element-specs
           (find-first #(= elem-key (:elem-name %)))
           :transform)
      identity))

(defn- apply-transform-funcs
  "Returns the elements-map with transformed values. The transform functions
   used for data under some element name are to be specified in element-specs
   (see doc for parse-feed for the format), otherwise a default function is used
   (also see doc for parse-feed)."
  [element-specs elements-map]
  (reduce-kv (fn [transformed-elements-map elem-key ignore-elem-values]
               (update transformed-elements-map
                       elem-key
                       (get-transform-from-specs element-specs elem-key)))
             elements-map
             elements-map))

(defn- dasherize-map-keys
  "Returns the map with the keys dasherized. Expects keys to be keywords"
  [m]
  (update-map-keys (comp keyword dasherize name) m))

(defn- extra-elements [synd-feed extra-map]
  (when (seq extra-map)
    (let [entries (.getEntries synd-feed)]
      (->> extra-map
           (map (fn [[ns-prefix element-specs]]
                  (let [name-set (->> element-specs (map (comp name :elem-name)) (into #{}))
                        prefix (name ns-prefix)
                        element-maps (extra-elements-map entries name-set prefix)]
                    (map (comp dasherize-map-keys
                               (partial apply-transform-funcs element-specs))
                         element-maps))))
           zip-merge-maps))))

(defn- add-extra-to-entries [feed-map extra-maps]
  (let [entries (map-indexed
                  (fn [idx entry]
                    (assoc entry :extra (nth extra-maps idx)))
                  (:entries feed-map))]
    (assoc feed-map :entries entries)))

(defn- media-content-is-image?
  "Returns true if given MediaContent is an image, false otherwise"
  [^MediaContent m-content]
  (= "image" (.getMedium m-content)))

(defn- media-content-is-video?
  "Returns true if given MediaContent is an video, false otherwise"
  [^MediaContent m-content]
  (= "video" (.getMedium m-content)))

(defrecord feed [authors categories contributors copyright description
                 encoding entries feed-type image language link entry-links
                 published-date title uri])

(defrecord entry [authors categories contents media-rss-data contributors
                  description enclosures link published-date title updated-date uri])

(defrecord thumbnail [url width height])

; represents an image under the MRSS specs
(defrecord media-image [url type width height filesize])

(defrecord media-video [url type width height bitrate duration filesize])

; represents a description under the MRSS specs
(defrecord description [value type])

; represents a category under the MRSS specs
(defrecord media-category [value scheme label])

; represents data parsed from an MRSS-formatted feed
(defrecord media-data [thumbnails keywords credits copyright texts description
                       categories images videos])

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

(defn make-thumbnail "Create thumbnail struct from com.rometools.modules.mediarss.types.Thumbnail"
  [^Thumbnail t]
  (map->thumbnail {:url (-> t .getUrl .toString)
                   :width (.getWidth t)
                   :height (.getHeight t)}))

(defn make-media-image "Create media-image struct from com.rometools.modules.mediarss.types.MediaContent
                        if it is of medium 'image'; returns nil otherwise"
  [^MediaContent m-content]
  (when (media-content-is-image? m-content)
    (map->media-image {:url (when-let [reference (.getReference m-content)]
                              (if (instance? UrlReference reference)
                                (.toString reference)))
                       :type (.getType m-content)
                       :width (.getWidth m-content)
                       :height (.getHeight m-content)
                       :filesize (.getFileSize m-content)})))

(defn make-media-video "Create media-video struct from com.rometools.modules.mediarss.types.MediaContent
                        if it is of medium 'video'; returns nil otherwise"
  [^MediaContent m-content]
  (when (media-content-is-video? m-content)
    (map->media-video {:url (when-let [reference (.getReference m-content)]
                              (if (instance? UrlReference reference)
                                (.toString reference)))
                       :type (.getType m-content)
                       :width (.getWidth m-content)
                       :height (.getHeight m-content)
                       :bitrate (.getBitrate m-content)
                       :duration (.getDuration m-content)
                       :filesize (.getFileSize m-content)})))

(defn make-description "Create description struct from com.rometools.modules.mediarss.types.Metadata"
  [^Metadata metadata]
  (when metadata
    (map->description {:value (.getDescription metadata)
                       :type (.getDescriptionType metadata)})))

(defn make-media-category "Create media-category struct from com.rometools.modules.mediarss.types.Category"
  [^Category category]
  (when category
    (map->media-category {:value (.getValue category)
                          :scheme (.getScheme category)
                          :label (.getLabel category)})))

(defn make-media-data "Create media-data struct from com.rometools.modules.mediarss.MediaEntryModule"
  [^MediaEntryModule module]
  (when module
    (let [metadata (.getMetadata module)
          images (map make-media-image
                      (filter media-content-is-image? (.getMediaContents module)))
          videos (map make-media-video
                      (filter media-content-is-video? (.getMediaContents module)))
          meta-attributes (when metadata
                            {:thumbnails (map make-thumbnail (.getThumbnail metadata))
                             :keywords (-> metadata .getKeywords seq)
                             :credits (map #(.getName %1) (.getCredits metadata)) ;; NOTE MRSS spec allows for more attributes than we're extracting here
                             :copyright (.getCopyright metadata)
                             :texts (map #(.getValue %1) (.getText metadata)) ;; NOTE MRSS spec allows for more attributes than we're extracting here
                             :description (make-description metadata)
                             :categories (map make-media-category (.getCategories metadata))})]
        (map->media-data (merge {:images images
                                 :videos videos} meta-attributes)))))

(defn make-entry "Create feed entry struct from SyndEntry"
  [^SyndEntry e]
  (map->entry {:author (.getAuthor e)
               :authors (map make-person (seq (.getAuthors e)))
               :categories (map make-category (seq (.getCategories e)))
               :contents (map make-content (seq (.getContents e)))
               :media-rss-data (when-let [media-module (.getModule e "http://search.yahoo.com/mrss/")]
                                 (make-media-data media-module))
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
  (map->feed  {:author (.getAuthor f)
               :authors (map make-person (seq (.getAuthors f)))
               :categories (map make-category (seq (.getCategories f)))
               :contributors (map make-person (seq (.getContributors f)))
               :copyright (.getCopyright f)
               :description (.getDescription f)
               :generator (.getGenerator f)
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
  (let [feedinput (gen-syndfeedinput)]
    (.build feedinput xmlreader)))

(defn- with-user-agent [connection user-agent]
  "Adds User-Agent property to the given HttpURLConnection to avoid HTTP 429 errors"
  (if user-agent
    (.setRequestProperty connection "User-Agent" user-agent)
    connection))

(defn- with-basic-authentication [connection username password]
  "Adds Basic Authentication property to the given HttpURLConnection"
  (if username
    (let [encoding (base64/encode (str username ":" password))]
      (.setRequestProperty connection "Authorization" (str "Basic " encoding) ))
    connection))

(defn- connection-with-properties [url {:keys [user-agent username password]}]
  "Returns an HttpURLConnection for `url`.
  Options:
  * :user-agent for setting the user agent header
  * :username and :password for setting a Basic authentication header"
  (doto (cast HttpURLConnection (.openConnection (URL. url)))
    (with-user-agent user-agent)
    (with-basic-authentication username password)))

(defn- url-feed? [feedsource]
  (string? feedsource))

(defn parse-feed
  "Get and parse a feed from a URL string, XML File, InputStream or an HttpURLConnection.

   ## Options

  * :user-agent for setting the user agent header (for URL `feedsource` only)

  * :username and :password for setting a Basic authentication header (for URL `feedsource` only)

   * Provide an `:extra` option in order to extract elements from the feed that are not
   supported by the RSS/Atom xmlns. This must be a map with the keys being namespace prefixes
   (either a string or keyword) and the values being a collection of maps in the format:

   {:elem-name :name :transform function-name}

   where the elem-name value is the element name without their namespace (either
   strings or keywords) and the transform value is a function (defaulted to identity)
   applied to the list of  values found with the specified name, to produce the final
   output. All matching elements for each namespace will be merged into a single map
   and then added to each entry's hash-map under the `:extra` key.

   All element values with a specific namespace:name, are put into one list.
   Each element value is a ordered pair (2-tuple) like [value attributes], where
   value is the value and attributes is a map of attribute names (as keywords)
   to attribute values (as strings).

   This list of element values is then fed to the transform function to produce
   the final output.

   The extra data keys will be dasherized (underscores replaced with hyphens)
   and cast to keywords. Also note that at this time this only supports elements
   that are direct descendants of the entry's root node.

   Example: the Google Trends feed uses a xmlns called 'ht' for some of its elements.
   We want to extract the `ht:picture` and `ht:approx_source` elements. To do this the
   `:extra` map would be set to:

   `{:ht [{:key :picture :transform first} {:key :approx_source}]}`

   And one of the returned entries may look like this:

   `{:title 'Mother Teresa'
     :extra {:picture ['http://example.com' {:language 'English'}]
             :approx-source (['1,000,000+' {}])}}`."
  [feedsource & {:keys [extra] :as options}]
  (let [source (if (url-feed? feedsource)
                 (connection-with-properties feedsource options)
                 feedsource)
        synd-feed (-> (cond
                        (url-feed? source) (XmlReader. (URL. source))
                        (instance? HttpURLConnection source) (XmlReader. ^HttpURLConnection source)
                        (instance? InputStream source) (XmlReader. ^InputStream source)
                        (instance? File source) (XmlReader. ^File source)
                        :else (throw (ex-info "Unsupported source"
                                              {:source source :type (type source)})))
                      parse-internal)
        extra-maps (extra-elements synd-feed extra)]
    (println "extra" extra-maps)
    (-> (make-feed synd-feed)
        (add-extra-to-entries extra-maps))))

