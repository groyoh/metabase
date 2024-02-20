(ns metabase.sync.sync-metadata.fks
  "Logic for updating FK properties of Fields from metadata fetched from a physical DB."
  (:require
   [medley.core :as m]
   [metabase.driver :as driver]
   [metabase.driver.util :as driver.u]
   [metabase.models.field :refer [Field]]
   [metabase.models.table :as table :refer [Table]]
   [metabase.sync.fetch-metadata :as fetch-metadata]
   [metabase.sync.interface :as i]
   [metabase.sync.util :as sync-util]
   [metabase.util :as u]
   [metabase.util.log :as log]
   [metabase.util.malli :as mu]
   [toucan2.core :as t2]))

(def ^:private FKRelationshipObjects
  "Relevant objects for a foreign key relationship."
  [:map
   [:source-field i/FieldInstance]
   [:dest-table   i/TableInstance]
   [:dest-field   i/FieldInstance]])

(defn- active-field [table-id field-name]
  (t2/select-one Field
                 :table_id           table-id
                 :%lower.name        (u/lower-case-en field-name)
                 :active             true
                 :visibility_type    [:not= "retired"]))

(mu/defn ^:private fetch-fk-relationship-objects :- [:maybe FKRelationshipObjects]
  "Fetch the Metabase objects (Tables and Fields) that are relevant to a foreign key relationship described by FK."
  [database :- i/DatabaseInstance
   table    :- i/TableInstance
   fk       :- i/FKMetadataEntry]
  (when-let [source-field (active-field (u/the-id table) (:fk-column-name fk))]
    ;; TODO: this preserves existing behaviour but seems wrong.
    (when (nil? (:fk_target_field_id source-field))
      (when-let [dest-table (t2/select-one Table
                                           :db_id           (u/the-id database)
                                           :%lower.name     (u/lower-case-en (-> fk :dest-table :name))
                                           :%lower.schema   (when-let [schema (-> fk :dest-table :schema)]
                                                              (u/lower-case-en schema))
                                           :active          true
                                           :visibility_type nil)]
        (when-let [dest-field (active-field (u/the-id dest-table) (:dest-column-name fk))]
          {:source-field source-field
           :dest-table   dest-table
           :dest-field   dest-field})))))

(mu/defn ^:private mark-fk!
  [{:keys [pk-table fk-table pk-field fk-field]}]
  (log/info (u/format-color 'cyan "Marking foreign key from %s %s -> %s %s"
                            (sync-util/name-for-logging fk-table)
                            (sync-util/name-for-logging fk-field)
                            (sync-util/name-for-logging pk-table)
                            (sync-util/name-for-logging pk-field)))
  (t2/update! Field (u/the-id fk-field)
              {:semantic_type      :type/FK
               :fk_target_field_id (u/the-id pk-field)}))

(mu/defn sync-fks-for-tables!
  "Sync the foreign keys for a specific `table`."
  [database :- i/DatabaseInstance
   tables   :- [:sequential i/TableInstance]]
  (sync-util/with-error-handling (format "Error syncing FKs for %s" (sync-util/name-for-logging database))
    (let [fk-metadata    (fetch-metadata/fast-fk-metadata database)
          table->ident   (juxt :schema :name)
          ident->table   (m/index-by table->ident tables)
          fks            (keep (fn [metadata]
                                 (when-let [pk-table (ident->table (table->ident (:pk-table metadata)))]
                                   (when-let [fk-table (ident->table (table->ident (:fk-table metadata)))]
                                     (when-let [pk-field (active-field (u/the-id pk-table) (:pk-column-name metadata))]
                                       (when-let [fk-field (active-field (u/the-id fk-table) (:fk-column-name metadata))]
                                         {:pk-table pk-table
                                          :fk-table fk-table
                                          :pk-field pk-field
                                          :fk-field fk-field})))))
                               fk-metadata)]
      {:total-fks   (count fk-metadata)
       :updated-fks (count (map mark-fk! fks))})))

(mu/defn sync-fks-for-table!
  "Sync the foreign keys for a specific `table`."
  ([table :- i/TableInstance]
   (sync-fks-for-table! (table/database table) table))

  ([database :- i/DatabaseInstance
    table    :- i/TableInstance]
   (sync-util/with-error-handling (format "Error syncing FKs for %s" (sync-util/name-for-logging table))
     (let [fks-to-update (fetch-metadata/fk-metadata database table)]
       {:total-fks   (count fks-to-update)
        :updated-fks (sync-util/sum-numbers
                      (fn [fk]
                        (if-let [{:keys [source-field
                                         dest-table
                                         dest-field]} (fetch-fk-relationship-objects database table fk)]
                          (do (mark-fk! {:pk-table dest-table
                                         :fk-table table
                                         :pk-field dest-field
                                         :fk-field source-field})
                              1)
                          0))
                      fks-to-update)}))))

(mu/defn sync-fks!
  "Sync the foreign keys in a `database`. This sets appropriate values for relevant Fields in the Metabase application
  DB based on values from the `FKMetadata` returned by [[metabase.driver/describe-table-fks]]."
  [database :- i/DatabaseInstance]
  (if (driver/database-supports? (driver.u/database->driver database)
                                 :fast-sync-fks
                                 database)
    (->> (sync-util/db->sync-tables database)
         (sync-fks-for-tables! database))
    (reduce (fn [update-info table]
              (let [table-fk-info (sync-fks-for-table! database table)]
              ;; Mark the table as done with its initial sync once this step is done even if it failed, because only
              ;; sync-aborting errors should be surfaced to the UI (see
              ;; `:metabase.sync.util/exception-classes-not-to-retry`).
                (sync-util/set-initial-table-sync-complete! table)
                (if (instance? Exception table-fk-info)
                  (update update-info :total-failed inc)
                  (merge-with + update-info table-fk-info))))
            {:total-fks    0
             :updated-fks  0
             :total-failed 0}
            (sync-util/db->sync-tables database))))
