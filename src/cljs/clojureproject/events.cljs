(ns clojureproject.events
  (:require [clojureproject.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-sub]]))


(defn contains-val?
  "Checks if collections contains given value"
  [coll val]
  (when (seq coll)
    (or (= val (first coll))
        (recur (next coll) val))))

(defn in?
  "true if coll contains val"
  [coll val]
  (some #(= val %) coll))

;;-------------------
;; dispatchers

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :update-user-list
  (fn [db [_ user-list]]
    (assoc db :user-list user-list)))

(reg-event-db
  :set-my-user
  (fn [db [_ user_id]]
    (assoc db :my-user user_id)))

(reg-event-db
  :set-invited-by-user
  (fn [db [_ user_id]]
    (assoc db :invited-by-user user_id)))

(reg-event-db
  :set-inviting-user
  (fn [db [_ user_id]]
    (assoc db :inviting-user user_id)))

(reg-event-db
  :set-opponent
  (fn [db [_ user_id]]
    (assoc db :opponent user_id)))

(reg-event-db
  :set-game-hitter
  (fn [db [_ user_id]]
    (assoc db :game-hitter user_id)))

(reg-event-db
  :set-game-first-hitter
  (fn [db [_ user_id]]
    (assoc db :game-first-hitter user_id)))

(reg-event-db
  :reset-game-state
  (fn [db [_]]
    (assoc db :game-state [0 0 0 0 0 0 0 0 0])))

(reg-event-db
  :set-game-state
  (fn [db [_ hitter hit_number]]
    (assoc-in db [:game-state hit_number] hitter)))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :user-login
  (fn [db [_ user_id]]
    (let [active_user (:user-list db)]
      (if (in? active_user user_id)
          (do
            (println user_id "already exists!")
            db)
          (update db :user-list conj user_id)
        ))))

(reg-event-db
  :user-logout
  (fn [db [_ user_id]]
    (update db :user-list
      (fn [user_ids]
        (remove #(= % user_id) user_ids)))))


;;-------------------
;; subscriptions
;;
;; This acts like a listener to the keys in the state, if a key gets updated
;; it sends an event to all subscribers of this key.

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
  :user-list
  (fn [db _]
    (:user-list db)))

(reg-sub
  :my-user
  (fn [db _]
    (:my-user db)))

(reg-sub
  :invited-by-user
  (fn [db _]
    (:invited-by-user db)))

(reg-sub
  :inviting-user
  (fn [db _]
    (:inviting-user db)))

(reg-sub
  :opponent
  (fn [db _]
    (:opponent db)))

(reg-sub
  :game-hitter
  (fn [db _]
    (:game-hitter db)))

(reg-sub
  :game-first-hitter
  (fn [db _]
    (:game-first-hitter db)))

(reg-sub
  :game-state
  (fn [db _]
    (:game-state db)))