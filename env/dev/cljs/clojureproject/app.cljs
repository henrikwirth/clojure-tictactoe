(ns ^:figwheel-no-load clojureproject.app
  (:require [clojureproject.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
