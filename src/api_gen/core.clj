(ns api-gen.core
  (:gen-class)
  (:require [clojure.java.jdbc :as jdbc]
            [ring.adapter.jetty :as ring]
            [clojure.data.json :as json]))

(defn db-spec [name]
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname name})

(defn gen-get-table [table]
  `(compojure.core/GET ~(str "/" (:name table)) []
     (~'index (jdbc/query ~'db-spec [(str "SELECT * FROM " ~(:name table))]))))

(defn gen-routes [name tables]
  `(compojure.core/defroutes ~name
     ~@(map gen-get-table tables)))

(defn gen-program
  [programname dbname tables]
  `((~'ns ~(symbol (str programname ".core"))
          (:gen-class)
          (:require [clojure.java.jdbc :as ~'jdbc]
                    [compojure.core :refer [~'defroutes ~'GET]]
                    [ring.adapter.jetty :as ~'ring]
                    [clojure.data.json :as ~'json]))
    (~'def ~'db-spec
           {:classname "org.sqlite.JDBC"
            :subprotocol "sqlite"
            :subname ~dbname})
    (~'defn ~'index [~'sql-request]
            {:headers {"Content-type" "application/json"}
             :status 200
             :body (json/write-str ~'sql-request)})
    ~(gen-routes 'routes tables)
    (~'defn ~'-main []
            (ring/run-jetty ~'#'routes {:port 8080 :join? false}))))

(defn gen-project [name]
  `(defproject ~(symbol name) "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.23.1"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/data.json "2.4.0"]]
  :main ^:skip-aot ~(symbol name)
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}))

(defn -main [& args]
  (try
    (let [programname (first args)
          dbname (second args)
          tables (jdbc/query (db-spec dbname) ["SELECT name FROM sqlite_master WHERE type = \"table\";"])]
      (->> (gen-program programname dbname tables)
           (map clojure.pprint/pprint)
           (dorun)))
    (catch Exception e (println (str  "ERROR: " (.getMessage e))))))
