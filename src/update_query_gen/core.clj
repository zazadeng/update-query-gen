;;todo 
;; - try batch update:
;; conn.prepareStatement, conn.prepareStatement.addbatch, finally conn.prepareStatement.executebatch
(ns update-query-gen.core
  (:import (java.io PushbackReader FileReader)
    (com.jolbox.bonecp BoneCPDataSource))
  (:require 
    [clojure.edn :as ed]
    [clojure.java.jdbc :as jdbc])
  (:gen-class 
    :methods [ ^:static [getUpdateQueryDDL [String String String String] String ]
              ]))

;"ONLY one option, \"sqlite\" for now ..."
(def jdbc-db
  {"sqlite" "org.sqlite.JDBC" } )

(defn get-db-spec [db-url db-subprotocol] 
  {:classname   (get jdbc-db db-subprotocol)
   :subprotocol db-subprotocol 
   :subname     db-url 
   }
  )

(defn sqlite-pool
  [dbUrl dbSubprotocol]
  (let [spec (get-db-spec dbUrl dbSubprotocol)
        partitions 1
        url (str "jdbc:" (:subprotocol spec) ":" (:subname spec))
        cpds (doto (BoneCPDataSource.)
               (.setJdbcUrl url)
               ;(.setUsername (:user spec))
               ;(.setPassword (:password spec))
               (.setMinConnectionsPerPartition 5)
               (.setMaxConnectionsPerPartition 10)
               (.setPartitionCount partitions)
               ;(.setStatisticsEnabled true);;require config for log4j
               ;; test connections every 25 mins (default is 240):
               (.setIdleConnectionTestPeriodInMinutes 25)
               ;; allow connections to be idle for 3 hours (default is 60 minutes):
               (.setIdleMaxAgeInMinutes (* 3 60))
               ;; consult the BoneCP documentation for your database:
               (.setConnectionTestStatement "/* ping *\\/ SELECT 1"))]
    {:datasource cpds}))

(def pooled-sqlite-db (delay (sqlite-pool "./testdata/example.db" "sqlite")))
(defn db-sqlite-connection [] @pooled-sqlite-db)
(defn get-sqlite-db-pool [dbPath] (sqlite-pool dbPath "sqlite"))

;(dm-to-string "A" "B" "C")
;(dm-to-string "A")
;(dm-to-string "")
;(dm-to-string)
#_(defn ^String dm-to-string
   {:static true}
   ([] "")
   ([x & ys] 
     (if (nil? ys) (str x) (str x "_" (clojure.string/join "_" ys)))))

;(first-letters "  Zaza    Deng")
#_(defn first-letters [string]
   (reduce (fn [s word] 
             (str s (subs word 0 1))) 
     "" (clojure.string/split (clojure.string/trim string) #"\s+")))

;(keyword-to-lower-case '(str "Mask owner" (first-letters (db-val :OWNER)) "_" (db-val :addressid) :addressid))
(defn keyword-to-lower-case [form]
  (clojure.walk/prewalk (fn [f] (if (=(type f) clojure.lang.Keyword) (keyword (clojure.string/lower-case (name f))) f)) form))

(defn mask-logic [data mask-set-entry retrived-data] 
  (reduce (fn [d e] 
            (let [form (val e)
                  ac (first form)
                  patterns (re-seq (re-pattern (key e)) d)
                 ] 
              (reduce (fn [dd s] 
                      (clojure.string/replace 
                        dd
                        (re-pattern (clojure.string/trim s))
                        (clojure.string/re-quote-replacement 
                          (eval (clojure.walk/prewalk-replace retrived-data (keyword-to-lower-case (val e)))))))
              d patterns)))
    data (val mask-set-entry)))

(defn get-update-columns-map [columns retrived-row]
  (reduce (fn [result col-spec] 
            (merge result (reduce (fn [m e] 
                                    (if-let [c-val ((keyword (clojure.string/lower-case (key col-spec))) retrived-row)]
                                      (if (= (keyword "mask-set") (key e))
                                        {(keyword (clojure.string/lower-case (key col-spec))) (mask-logic c-val e retrived-row)}
                                        {})
                                      {}))
                            {} (val col-spec)))) 
    {} columns))

(defn get-update-query-map [subsys-schema table-edn-path dbConnection]
  (with-open [reader (PushbackReader. (FileReader. table-edn-path))]
    (let [edn-data (ed/read reader)
           sub-sch ((symbol subsys-schema) edn-data)
            table (:table sub-sch)
            k-col (:primary-key sub-sch)
            v-cols (keys (:mask-columns sub-sch))]
      (jdbc/query dbConnection [(:mask-select-query sub-sch)] 
         :row-fn (fn [row] 
                   {
                     [(str k-col "=?") ((keyword (clojure.string/lower-case k-col)) row)]
                     (get-update-columns-map (:mask-columns sub-sch) row) 
                     }
                   )
         :result-set-fn (partial reduce merge)))))

#_(gen-class
  :name update_query_gen.api
  :prefix "api-"
  :methods [#^{:static true} [getUpdateQueryDDL [String String String String] String] 
            [dmToString []]])

(defn -getUpdateQueryDDL [dbPath vendor tableSpecPath targetDbSubSystemSchemaName]
  (cond
    (= vendor "sqlite") (do
                          (println "initialize for JDBC Class: <" (. Class (forName "org.sqlite.JDBC")) ">")
                          (println-str (get-update-query-map tableSpecPath targetDbSubSystemSchemaName (get-sqlite-db-pool dbPath))))
    :else (println-str "todo"))
  )


