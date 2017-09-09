feedparser-clj
==============

Parse RSS/Atom feeds with a simple, clojure-friendly API.
Uses the Java ROME library, wrapped in Clojure records.

Status
------

Usable for parsing and exploring feeds.  No escaping of potentially-malicious content is performed, and we've inherited any quirks that ROME itself has.

Supports the following syndication formats:

* RSS 0.90
* RSS 0.91 Netscape
* RSS 0.91 Userland
* RSS 0.92
* RSS 0.93
* RSS 0.94
* RSS 1.0
* RSS 2.0
* Atom 0.3
* Atom 1.0

Usage
-----

For a more detailed understanding about supported feed types and meanings, the ROME javadocs (under [`com.sun.syndication.feed.synd`](https://rome.dev.java.net/apidocs/0_8/com/sun/syndication/feed/synd/package-summary.html)) are a good resource.

There is only one function, `parse-feed`, which takes a URL and returns a StructMap with all the feed's structure and content.

The following REPL session should give an idea about the capabilities and usage of `feedparser-clj`.

Load the package into your namespace:

    user=> (require '[feedparser-clj.core :as fp :refer [parse-feed]])

Retrieve and parse a feed: 

    user=> (def f (parse-feed "https://clojure.org/feed.xml"))

`f` is now a map that can be accessed by key to retrieve feed information:

    user=> (keys f)
    (:authors :categories :contributors :copyright :description :encoding :entries :feed-type :image :language :link :entry-links :published-date :title :uri :generator)

`parse-feed` also accepts a java.io.InputStream for reading from a file or other sources (see [clojure.java.io/input-stream](http://richhickey.github.com/clojure/clojure.java.io-api.html#clojure.java.io/input-stream)):

    ;; Contents of resources/feed.rss
    <rss>
      ...
    </rss>

    user=> (def f (with-open
                    [feed-stream (-> "feed.rss"
                                     clojure.java.io/resource
                                     clojure.java.io/input-stream)]
                    (parse-feed feed-stream)))


A key applied to the feed gives the value, or nil if it was not defined for the feed.

    user=> (:title f)
    "Greg Heartsfield"

Feed/entry ID or GUID can be obtained with the `:uri` key:

    user=> (:uri f)
    "http://gregheartsfield.com/"

Some feed attributes are maps themselves (like `:image`) or lists of structs (like `:entries` and `:authors`):

    user=> (map :email (:authors f))
    ("scsibug@imap.cc")

Check how many entries are in the feed:

    user=> (count (:entries f))
    18

Determine the feed type:

    user=> (:feed-type f)
    "atom_1.0"

Look at the first few entry titles:

    user=> (map :title (take 3 (:entries f)))
    ("Version Control Diagrams with TikZ" "Introducing cabal2doap" "hS3, with ByteString")

Find the most recently updated entry's title:

    user=> (first (map :title (reverse (sort-by :updated-date (:entries f)))))
    "Version Control Diagrams with TikZ"

Installation
------------

This library uses the [Leiningen](http://github.com/technomancy/leiningen#readme) build tool.

Simply add the following dependency in your project.clj.

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.scsibug/feedparser-clj.svg)](https://clojars.org/org.clojars.scsibug/feedparser-clj)

License
-------

Distributed under the BSD-3 License.

Copyright
---------

Copyright (C) 2010 Greg Heartsfield
