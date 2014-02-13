(ns update-query-gen.core-test
  (:import (java.io PushbackReader FileReader))
  (:require 
    [clojure.test :refer :all]
    [midje.sweet :refer :all]
    [update-query-gen.core :refer :all]
    [clojure.edn :as ed]))

(fact "return a column-maskedValue map"
  (let [colunmns (:mask-columns ('CMLY_CMSPROD (ed/read (PushbackReader. (FileReader. "./testdata/address.edn")))))
        r-data (ed/read (PushbackReader. (FileReader. "./testdata/retrieved-address.edn")))]
    (get-update-columns-map colunmns r-data) => {:addressdata "ADD1=Mask address_2 \r ADD2=another address \r CITY=Mask city \r PROVINCE=BC \r COUNTRY=Canada", :owner "Mask owner_ZD"}))

(fact "return a vector of update queries"
 (get-update-query-map "CMLY_CMSPROD" "./testdata/address.edn" (get-sqlite-db-pool "./testdata/example.db")) 
 => {["ADDRESSID=?" 1] {:addressdata "ADD1=Mask address_1 \\r ADD2=another address \\r CITY=Mask city\\r PROVINCE=BC \\r COUNTRY=Canada", :owner "Mask owner_ZD"}, 
     ["ADDRESSID=?" 2] {:addressdata "ADD1=Mask address_2 \\r ADD2=another address \\r CITY=Mask city \\r PROVINCE=BC \\r COUNTRY=Canada", :owner "Mask owner_KD"}}
 )



;todo -main

;todo tofile
#_(dotimes [n 100]
   (with-open [testresult (clojure.java.io/writer  "./testresult.txt" :append true)]
     (.write testresult (str (clojure.string/join (repeat 10 "0123456789")) ";" "\r"))))




