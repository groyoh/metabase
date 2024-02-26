(ns metabase.lib.drill-thru.extract-column
  ""
  (:require
    [metabase.lib.expression :as lib.expression]
    [metabase.lib.drill-thru.common :as lib.drill-thru.common]
    [metabase.lib.metadata.calculation :as lib.metadata.calculation]
    [metabase.lib.schema :as lib.schema]
    [metabase.lib.schema.drill-thru :as lib.schema.drill-thru]
    [metabase.lib.schema.metadata :as lib.schema.metadata]
    [metabase.lib.schema.ref :as lib.schema.ref]
    [metabase.lib.schema.temporal-bucketing
     :as lib.schema.temporal-bucketing]
    [metabase.lib.temporal-bucket :as lib.temporal-bucket]
    [metabase.lib.types.isa :as lib.types.isa]
    [metabase.util.malli :as mu]))

(mu/defn extract-column-drill :- [:maybe ::lib.schema.drill-thru/drill-thru.extract-column]
  ""
  [query            :- ::lib.schema/query
   stage-number     :- :int
   {:keys [column]} :- ::lib.schema.drill-thru/context]
  (when (lib.types.isa/temporal? column)
    {:lib/type    :metabase.lib.drill-thru/drill-thru
     :type        :drill-thru/extract-column
     :extractions (mapv
                    (fn [unit]
                      {:lib/type :drill-thru/extract-column-type
                      :unit       unit})
                    lib.schema.temporal-bucketing/ordered-date-truncation-units)}))

(mu/defn extract-column-types :- [:sequential ::lib.schema.drill-thru/drill-thru.extract-column-type]
  ""
  [drill-thru :- [:and ::lib.schema.drill-thru/drill-thru
                   [:map [:type [:= :drill-thru/extract-column]]]]]
  (:extractions drill-thru))

(defmethod lib.drill-thru.common/drill-thru-info-method :drill-thru/extract-column
  [_query _stage-number _drill]
  {:type :drill-thru/extract-column})

(defmethod lib.metadata.calculation/display-info-method :drill-thru/extract-column-type
  [query stage-number {:keys [unit]}]
  {:display-name (lib.temporal-bucket/describe-temporal-unit unit)})

(defmethod lib.drill-thru.common/drill-thru-method :drill-thru/extract-column
  [query stage-number {:keys [column]} & [extraction]]
  (lib.expression/expression
    query
    stage-number
    "Year"
   (lib.expression/get-year column)))
