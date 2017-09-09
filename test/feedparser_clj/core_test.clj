(ns feedparser-clj.core-test
  (:require [clojure.test :refer :all]
            [feedparser-clj.core :refer :all])
  (:import [java.io File]))


(defn test-file [filename]
  (File. (str "./test/feedparser_clj/resources/" filename)))

(defn first-entry [src & args]
  (-> (apply parse-feed src args) :entries first))

(deftest test-parse-feed
  (let [trends (test-file "google-trends-custom.xml")
        itunes (test-file "topmusic-itunes.xml")]
    (testing "extra field is empty"
      (is (empty? (-> (first-entry trends :extra nil) :extra))))

    (testing "extra field has data"
      (testing "extracts a single xmlns"
        (is (= "//t3.gstatic.com/images?q=tbn:ANd9GcQfgpW-VQEdBHDEZ1XqC1pQ_KsnvGA0KXC5uaNpsVbfu0XUCMhP0udQNTBzybD6ESbJVZ6wH4QH"
               (-> (first-entry trends :extra {:ht [{:elem-name :picture}]})
                   :extra :picture ffirst)))

        (is (= {:name (list ["Work (feat. Drake)" {}])
                :artist (list ["Rihanna" {:href "https://itunes.apple.com/us/artist/rihanna/id63346553?uo=2"}])
                :price (list ["$1.99" {:amount "1.99000" :currency "USD"}])
                :releaseDate (list ["2016-02-23T00:00:00-07:00" {:label "February 23, 2016"}])}
               (-> (first-entry itunes :extra {:im [{:elem-name :name}
                                                    {:elem-name :artist}
                                                    {:elem-name :price}
                                                    {:elem-name :releaseDate}]}) :extra))))

      (testing "extracts multiple xmlns and supports multiple values"
        (is (= {:picture (list ["//t3.gstatic.com/images?q=tbn:ANd9GcQfgpW-VQEdBHDEZ1XqC1pQ_KsnvGA0KXC5uaNpsVbfu0XUCMhP0udQNTBzybD6ESbJVZ6wH4QH" {}])
                :news-item-source (list ["Chicago Tribune" {}])}
               (-> (first-entry trends :extra {:ht [{:elem-name :picture}]
                                               :mi [{:elem-name :news_item_source}]}) :extra))))

      (testing "attributes are parsed"
        (is (= "53"
               (-> (first-entry itunes :extra {:im [{:elem-name :image}]})
                   :extra :image first second :height)))
        (is (= "omg, awesome value"
               (-> (first-entry itunes :extra {:im [{:elem-name :image}]})
                   :extra :image first second :awesomeAttri))))

      (testing "extra key is dasherized"
        (is (= :approx-traffic
               (-> (first-entry trends :extra {:ht [{:elem-name :approx_traffic}]}) :extra keys first))))

      (testing "multiple extra values for the same key"
        (is (= 2 (-> (first-entry trends :extra {:ht [{:elem-name :picture_source}]}) :extra
                      :picture-source count)))
        (is (some #{"Made-up source"}
                  (->> (first-entry trends :extra {:ht [{:elem-name :picture_source}]}) :extra
                      :picture-source (map first))))
        (is (some #{"Chicago Tribune"}
                  (->> (first-entry trends :extra {:ht [{:elem-name :picture_source}]}) :extra
                      :picture-source (map first)))))

      (testing "no extra value found for given element name/namespace"
        (is (false? (-> (first-entry trends :extra {:ht [{:elem-name :non-exist-name}]})
                        :extra (contains? :non-exist-name))))
        (is (false? (-> (first-entry trends :extra {:woot-woot [{:elem-name :picture_source}]})
                        :extra (contains? :picture_source))))
        (testing "some names return value and some don't"
          (let [extra-map (-> (first-entry trends
                                   :extra {:ht [{:elem-name :picture_source :transform rest}
                                                {:elem-name :non-exist-name :transform identity}]})
                              :extra)]
            (is (false? (-> extra-map (contains? :non-exist-name))))
            (is (some #{"Made-up source" "Chicago Tribune"} (->> extra-map
                                                                 :picture-source
                                                                 (map first))))
            (is (= 1 (count (:picture-source extra-map)))))))

      (testing "transform function is correctly applied to extra data"
        (is (some #{"Made-up source" "Chicago Tribune"}
                  (-> (first-entry trends
                                   :extra {:ht [{:elem-name :picture_source :transform ffirst}]})
                      :extra :picture-source list)))
        (let [extra-value (-> (first-entry trends
                                   :extra {:ht [{:elem-name :picture_source :transform rest}]})
                      :extra :picture-source)]
          (is (some #{"Made-up source" "Chicago Tribune"} (list (ffirst extra-value))))
          (is (= 1 (count extra-value))))))))

(deftest test-media-rss
  (let [gigaom (test-file "gigaom-quicktime-large.xml")]
    (testing "Gigaom Quicktime mRSS feed"
      (testing "contains expected number of items"
        (is (= 25 (-> gigaom parse-feed :entries count)))))

    (testing "Bloomberg Video Clips mRSS feed"
      (let [bb-videos (test-file "bbvideo.xml")
            bb-videos-first-entry (-> bb-videos first-entry)]
        (testing "contains expected number of items"
          (is (= 2 (-> bb-videos parse-feed :entries count))))

        (testing "first entry"
          (testing "has some correct non-media-rss data"
            (is (= (:title bb-videos-first-entry) "Apple iPhone Suppliers Are Struggling"))
            (is (= (-> bb-videos-first-entry :description :value) "\nMay 11 -- Three of Apple's Asian suppliers of iPhone parts offered a bleak view for the company with their earnings results. Bloomberg Intelligence's Anand Srinivasan reports on \"Bloomberg Markets.\"\n")))

          (testing "has correct media-rss data..."
            (testing "thumbnails"
              (is (= 1 (-> bb-videos-first-entry :media-rss-data :thumbnails count)))
              (is (= "http://assets.stored.somewhere.secret/look-at-me.jpg"
                     (-> bb-videos-first-entry :media-rss-data :thumbnails first :url)))
              (is (= 640 (-> bb-videos-first-entry :media-rss-data :thumbnails first :width)))
              (is (= 360 (-> bb-videos-first-entry :media-rss-data :thumbnails first :height))))
            (testing "keywords"
              (is (= 23 (-> bb-videos-first-entry :media-rss-data :keywords count)))
              (is (= (-> bb-videos-first-entry :media-rss-data :keywords first) "Apple Inc.")))
            (testing "credits"
              (is (empty? (-> bb-videos-first-entry :media-rss-data :credits))))
            (testing "copyright"
              (is (nil? (-> bb-videos-first-entry :media-rss-data :copyright))))
            (testing "text"
              (is (empty? (-> bb-videos-first-entry :media-rss-data :texts))))
            (testing "description"
              (is (nil? (-> bb-videos-first-entry :media-rss-data :description :value)))
              (is (nil? (-> bb-videos-first-entry :media-rss-data :description :type))))
            (testing "categories"
              (is (= 15 (-> bb-videos-first-entry :media-rss-data :categories count)))
              (is (= (-> bb-videos-first-entry :media-rss-data :categories first :value) "Asia; Pacific Rim"))
              (is (= (-> bb-videos-first-entry :media-rss-data :categories first :scheme) "http://www.bloomberg.com"))
              (is (= (-> bb-videos-first-entry :media-rss-data :categories first :label) "ASIA")))

            (testing "images"
              (is (empty? (-> bb-videos-first-entry :media-rss-data :images))))

            (testing "videos"
              (is (= 1 (-> bb-videos-first-entry :media-rss-data :videos count)))
              (is (= (-> bb-videos-first-entry :media-rss-data :videos first :url)
                     "http://blah.blah.blah/nice-url/da-video.mp88"))
              (is (= (-> bb-videos-first-entry :media-rss-data :videos first :type) "video/mp4"))
              (is (= 1280 (-> bb-videos-first-entry :media-rss-data :videos first :width)))
              (is (= 720 (-> bb-videos-first-entry :media-rss-data :videos first :height)))
              (is (= 1800.0 (-> bb-videos-first-entry :media-rss-data :videos first :bitrate)))
              (is (= 185 (-> bb-videos-first-entry :media-rss-data :videos first :duration)))
              (is (= 42605992 (-> bb-videos-first-entry :media-rss-data :videos first :filesize)))))))))

    (testing "Bloomberg news mRSS feed (older)"
      (let [bb-news (test-file "bloomberg_sample_news_feed.xml")
            bb-news-first-entry (first-entry bb-news)]
        (testing "contains expected number of items"
          (is (= 2 (-> bb-news parse-feed :entries count))))
        (testing "first entry"
          (testing "has some correct non-media-rss data"
            (is (= (:title bb-news-first-entry) "Shocking news title shocks the news reader (1)"))
            (is (= (:author bb-news-first-entry) "Bonnie Cao"))
            (is (= (-> bb-news-first-entry :description :value) "So shock, much tears")))

          (testing "has correct media-rss data..."
            (testing "credits"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :credits count)))
              (is (contains? (set (-> bb-news-first-entry :media-rss-data :credits)) "Bonnie Cao")))
            (testing "copyright"
              (is (= (-> bb-news-first-entry :media-rss-data :copyright) "©2016 Bloomberg L.P.")))
            (testing "description"
              (is (= (-> bb-news-first-entry :media-rss-data :description :value) "So media, much description"))
              (is (nil? (-> bb-news-first-entry :media-rss-data :description :type))))
            (testing "categories"
              (is (empty? (-> bb-news-first-entry :media-rss-data :categories))))
            (testing "text"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :texts count)))
              (is (contains? (set (-> bb-news-first-entry :media-rss-data :texts)) "CODE-CODE-CODE")))))))

    (testing "Bloomberg news mRSS feed (newer)"
      (let [bb-news (test-file "bb_news_feed_new.xml")
            bb-news-first-entry (-> bb-news parse-feed :entries first)]
        (testing "contains expected number of items"
          (is (= 1 (-> bb-news parse-feed :entries count))))

        (testing "first entry"
          (testing "has some correct non-media-rss data"
            (is (= (:title bb-news-first-entry) "Is Sears or Gitmo a Worse Problem?: Shelly Banjo & Tobin Harshaw"))
            (is (= (:author bb-news-first-entry) "Shelly Banjo and Tobin Harshaw"))
            (is (= (->> bb-news-first-entry :description :value) "<p>blah blah blah, I am the description :)</p>")))

          (testing "has correct media-rss data..."
            (testing "thumbnails"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :thumbnails count)))
              (is (= "very_literal_thumbnail_url"
                     (-> bb-news-first-entry :media-rss-data :thumbnails first :url)))
              (is (nil? (-> bb-news-first-entry :media-rss-data :thumbnails first :width)))
              (is (nil? (-> bb-news-first-entry :media-rss-data :thumbnails first :height))))
            (testing "keywords"
              (is (= 68 (-> bb-news-first-entry :media-rss-data :keywords count)))
              (is (= (-> bb-news-first-entry :media-rss-data :keywords first) "ALLTOP")))
            (testing "credits"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :credits count)))
              (is (contains? (set (-> bb-news-first-entry :media-rss-data :credits)) "Shelly Banjo and Tobin Harshaw")))
            (testing "copyright"
              (is (= (-> bb-news-first-entry :media-rss-data :copyright) "©2016 Bloomberg L.P.")))
            (testing "text"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :texts count)))
              (is (contains? (set (-> bb-news-first-entry :media-rss-data :texts)) "BC-SEARS-HAS-PROBLEMS-GITMO-HAS-BIGGER-ONES")))
            (testing "description"
              (is (= (-> bb-news-first-entry :media-rss-data :description :value) "lalala, I am a media description."))
              (is (nil? (-> bb-news-first-entry :media-rss-data :description :type))))
            (testing "categories"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :categories count)))
              (is (= (-> bb-news-first-entry :media-rss-data :categories first :value) "i"))
              (is (= (-> bb-news-first-entry :media-rss-data :categories first :scheme) "http://generic-category-scheme.com"))
              (is (nil? (-> bb-news-first-entry :media-rss-data :categories first :label))))

            (testing "images"
              (is (= 1 (-> bb-news-first-entry :media-rss-data :images count)))
              (is (= (-> bb-news-first-entry :media-rss-data :images first :url)
                     "very-literal-image-url"))
              (is (= (-> bb-news-first-entry :media-rss-data :images first :type) "image/jpeg"))
              (is (nil? (-> bb-news-first-entry :media-rss-data :images first :height)))
              (is (nil? (-> bb-news-first-entry :media-rss-data :images first :width)))
              (is (nil? (-> bb-news-first-entry :media-rss-data :images first :filesize))))

            (testing "videos"
              (is (empty? (-> bb-news-first-entry :media-rss-data :videos))))))))

    (testing "Non-media-rss feeds don't have media-rss data"
      (let [trends (test-file "google-trends-custom.xml")
        itunes (test-file "topmusic-itunes.xml")]
        (is (nil? (-> trends first-entry  :media-rss-data)))
        (is (nil? (-> itunes first-entry  :media-rss-data))))))

