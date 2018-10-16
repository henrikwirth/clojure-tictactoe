(ns clojureproject.routes.websockets
  (:require [compojure.core :refer [GET defroutes wrap-routes]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defonce channels (atom #{}))
(defonce user-list (atom #{}))

(defn send-to-channels
  "Broadcasting to all Channels"
  [msg]
  (let [out (ByteArrayOutputStream. 4096) writer (transit/writer out :json)]
    (transit/write writer msg)
    (doseq [channel @channels]
      (async/send! channel (.toString out)))))


;; --------------------
;; Actions

(defn update-list_
  "Receive List action"
  [_]
  (send-to-channels {:action "receive-list"
                     :value @user-list}))

(defn login_
  "Login action"
  [channel value]
  (swap! user-list conj (value :user_id))
  (update-list_ channel)
  (println user-list))

(defn logout_
  "Logout action"
  [channel value]
  (swap! user-list #(remove #{(value :user_id)} %))
  (update-list_ channel))

(defn invite_
  "Invite action"
  [_ value]
  (send-to-channels {:action "invite"
                     :value {:sender_id (value :sender_id) :receiver_id (value :receiver_id)}}))

(defn accept_
  "Invite action"
  [_ value]
  (println "Accept backend" value)
  (send-to-channels {:action "accept"
                     :value {:accepter (value :accepter) :inviter (value :inviter)}}))

(defn game-hit_
  "Game hit action"
  [_ value]
  (println "Game hit backend" value)
  (send-to-channels {:action "game-hit"
                     :value {:sender_id (value :sender_id) :receiver_id (value :receiver_id) :hit_number (value :hit_number)}}))

(defn game-reset_
  "Game reset action"
  [_ value]
  (send-to-channels {:action "game-reset"
                     :value {:sender_id (value :sender_id) :receiver_id (value :receiver_id)}}))


;; --------------------

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel)
  (update-list_ channel))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients!
  "Notifies all clients. Message depends on action type."
  [channel msg]
  (do (println msg)
  (let [in (ByteArrayInputStream. (.getBytes msg)) reader (transit/reader in :json)]
    (let [parsed-msg (transit/read reader)]
      ; defines what function to call depending on action
      (case (parsed-msg :action)
        "login" (login_ channel (parsed-msg :value))
        "logout" (logout_ channel (parsed-msg :value))
        "invite" (invite_ channel (parsed-msg :value))
        "accept" (accept_ channel (parsed-msg :value))
        "game-hit" (game-hit_ channel (parsed-msg :value))
        "game-reset" (game-reset_ channel (parsed-msg :value))
        )))))



(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defroutes websocket-routes
           (GET "/ws" [] ws-handler))


