(ns clojureproject.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [clojureproject.core-test]))

(doo-tests 'clojureproject.core-test)

