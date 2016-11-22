;;;; Copyright © 2011 Paul Stadig
;;;;
;;;; Licensed under the Apache License, Version 2.0 (the "License"); you may not
;;;; use this file except in compliance with the License.  You may obtain a copy
;;;; of the License at
;;;;
;;;;   http://www.apache.org/licenses/LICENSE-2.0
;;;;
;;;; Unless required by applicable law or agreed to in writing, software
;;;; distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
;;;; WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
;;;; License for the specific language governing permissions and limitations
;;;; under the License.
(ns leiningen.migratus
  (:require [migratus.core :as core]
            [config.core]
            [config.core :refer [env]]
            [leiningen.core.eval :as eval]))

(def default-config {:store :database
                     :migration-dir "migrations"})

(defn migratus
  "Maintain database migrations.

Run migrations against a store.  The :migratus key in project.clj is passed to
migratus as configuration.

Usage `lein migratus [command & ids]`.  Where 'command' is:

migrate  Bring up any migrations that are not completed.
rollback Bring down the last applied migration.
up       Bring up the migrations specified by their ids.  Skips any migrations
         that are already up.
down     Bring down the migrations specified by their ids.  Skips any migrations
         that are already down.
create   Create a new migration file with the current date and the given name.
reset    Bring down all migrations, then bring them all back up.
pending  Return a list of pending migrations.

If you run `lein migratus` without specifying a command, then the 'migrate'
command will be executed."
  [project & [command & args]]
  (if-let [config (assoc default-config :db
                         (:db (config.core/read-config-file "resources/config.edn"))
                         (:db (config.core/read-config-file "config.edn")))]
    (case command
      "up"
      (do
        (println "migrating" args)
        (eval/eval-in-project
          project
          `(apply core/up ~config ~(cons 'vector (map #(Long/parseLong %) args)))
          '(require 'migratus.core)))

      "down"
      (do
        (println "rolling back" args)
        (eval/eval-in-project
          project
          `(apply core/down
                  ~config
                  ~(cons 'vector (map #(Long/parseLong %) args)))
          '(require 'migratus.core)))

      "rollback"
      (do
        (println "rolling back last migration")
        (eval/eval-in-project project `(core/rollback ~config) '(require 'migratus.core)))

      "pending"
      (do
        (println "listing pending migrations")
        (eval/eval-in-project project `(println (core/pending-list ~config)) '(require 'migratus.core)))

      "create"
      (do
        (println "creating migration files for" (clojure.string/join " " args))
        (println
          (eval/eval-in-project project `(core/create ~config ~(clojure.string/join " " args)) '(require 'migratus.core))))

      "reset"
      (do
        (println "resetting the database")
        (eval/eval-in-project project `(core/reset ~config) '(require 'migratus.core)))

      (if (and (or (= command "migrate") (nil? command)) (empty? args))
        (do
          (println "CONFIG: " config)
          (println "migrating all outstanding migrations")
          (eval/eval-in-project project `(core/migrate ~config) '(require 'migratus.core)))
        (println "Unexpected arguments to 'migrate'")))
    (println "Missing :migratus config in project.clj")))
