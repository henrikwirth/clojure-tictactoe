(ns clojureproject.db)

(def default-db
  {:page :home
   :user-list []
   :my-user nil
   :invited-by-user nil
   :inviting-user nil
   :opponent nil
   :game-hitter nil
   :game-first-hitter nil
   :game-state [0 0 0 0 0 0 0 0 0]})
