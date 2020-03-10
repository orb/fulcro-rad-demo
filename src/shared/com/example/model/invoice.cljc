(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.type-support.decimal :as math]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]))

(defattr id :invoice/id :uuid
  {::attr/identity?                                      true
   :com.fulcrologic.rad.database-adapters.datomic/schema :production
   ::auth/authority                                      :local})

(defattr date :invoice/date :instant
  {::form/field-style                                        :date-at-noon
   ::datetime/default-time-zone                              "America/Los_Angeles"
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr line-items :invoice/line-items :ref
  {::attr/target                                             :line-item/id
   ::attr/cardinality                                        :many
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr total :invoice/total :decimal
  {:com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production
   ::attr/read-only?                                         true}
  #_{::attr/computed-value (fn [{::form/keys [props]} attr]
                             (let [total (reduce
                                           (fn [t {:line-item/keys [quantity quoted-price]}]
                                             (math/+ t (math/* quantity quoted-price)))
                                           (math/zero)
                                           (:invoice/line-items props))]
                               total))})

(defattr customer :invoice/customer :ref
  {::attr/cardinality                                        :one
   ::attr/target                                             :account/id
   ::attr/required?                                          true
   :com.fulcrologic.rad.database-adapters.datomic/entity-ids #{:invoice/id}
   :com.fulcrologic.rad.database-adapters.datomic/schema     :production})

(defattr all-invoices :invoice/all-invoices :ref
  {::attr/target    :invoice/id
   ::auth/authority :local
   ::pc/output      [{:invoice/all-invoices [:invoice/id]}]
   ::pc/resolve     (fn [{:keys [query-params] :as env} _]
                      #?(:clj
                         {:invoice/all-invoices (queries/get-all-invoices env query-params)}))})

(def attributes [id date line-items customer all-invoices total])

