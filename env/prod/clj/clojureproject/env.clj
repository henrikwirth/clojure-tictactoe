(ns clojureproject.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[clojureproject started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[clojureproject has shut down successfully]=-"))
   :middleware identity})
