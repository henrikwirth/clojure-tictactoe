(ns clojureproject.app
  (:require [clojureproject.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
