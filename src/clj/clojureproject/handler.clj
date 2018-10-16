(ns clojureproject.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [clojureproject.layout :refer [error-page]]
            [clojureproject.routes.home :refer [home-routes]]
            [compojure.route :as route]
            [clojureproject.env :refer [defaults]]
            [mount.core :as mount]
            [clojureproject.middleware :as middleware]
            [clojureproject.routes.websockets :refer [websocket-routes]]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
      (-> #'home-routes
          (wrap-routes middleware/wrap-csrf)
          (wrap-routes middleware/wrap-formats))
      (-> #'websocket-routes
          (wrap-routes middleware/wrap-csrf)
          (wrap-routes middleware/wrap-formats))
      (route/not-found
        (:body
          (error-page {:status 404
                       :title "page not found"}))))))


