feedparser-clj
==============

About
-----

Parse RSS/Atom feeds with a simple, clojure-friendly API.
Uses the Java ROME library, wrapped in StructMaps.

Status
------

Usable for parsing and exploring feeds.  No escaping of potentially-malicious content is performed, and we've inherited any quirks that ROME itself has.

Usage
-----

For a more detailed understanding about supported feed types and meanings, the ROME javadocs (under [`com.sun.syndication.feed.synd`](https://rome.dev.java.net/apidocs/0_8/com/sun/syndication/feed/synd/package-summary.html)) are a good resource.

There is only one function, `parseFeed`, which takes a URL and returns a StructMap with all the feed's structure and content.

The following REPL session should give an idea about the capabilities and usage of `feedparser-clj`.

Load the package into your namespace:

    user=> (ns user (:use feedparser-clj.core))

Retrieve and parse a feed in any of the following syndication formats: 

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

    user=> (def f (parseFeed "http://gregheartsfield.com/atom.xml"))

`f` is a map that can be accessed by key to retrieve feed information:

    user=> (keys f)
    (:authors :categories :contributors :copyright :description :encoding :entries :feed-type :image :language :link :entry-links :published-date :title :uri)

A key applied to the feed gives the value, or nil if it was not defined for the feed.

    user=> (:title f)
    "Greg Heartsfield"

Some feed attributes are maps themselves (like `:image`) or lists of structs (like `:entries` and `:authors`):

    user=> (map :email (:authors f))
    ("scsibug@imap.cc")

    user=> (count (:entries f))
    18

    user=> (map :title (take 3 (:entries f)))
    ("Version Control Diagrams with TikZ" "Introducing cabal2doap" "hS3, with ByteString")

 
Installation
------------

FIXME: write

License
-------

Distributed under the BSD-3 License.

Copyright
---------

Copyright (C) 2010 Greg Heartsfield
