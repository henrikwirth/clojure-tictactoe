(ns clojureproject.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [clojureproject.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[clojureproject started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[clojureproject has shut down successfully]=-"))
   :middleware wrap-dev})
