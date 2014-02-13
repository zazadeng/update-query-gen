(defproject update-query-gen "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.3"] 
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [com.jolbox/bonecp "0.7.1.RELEASE"]
                 [org.slf4j/slf4j-log4j12 "1.5.0"]
                 ]
  :main update-query-gen.core
  :profiles {:dev {:dependencies [[midje "1.6.0"]]
                 :plugins [[lein-midje "3.1.3"]]}})
