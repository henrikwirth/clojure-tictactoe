(ns clojureproject.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [clojureproject.ajax :refer [load-interceptors!]]
            [clojureproject.events]
            [reagent.core :as reagent :refer [atom]]
            [clojureproject.websockets :as ws]
            [clojureproject.action-types :as action-types :refer [action]])
  (:import goog.History))



;; ------------------------
;; Atoms (local state)

(def show_login? (atom true))
(def show_invite? (atom false))
(def show_game? (atom false))
(def game_winner (atom nil))


;; -----------------------
;; Helper

(defn targets-me?
  [my_user sender_id receiver_id]
  (or
    (= my_user sender_id)
    (= my_user receiver_id)))

(defn win-variant?
  "Evaluates if the given variants values are all equal and are not zero"
  [a b c]
  (and (pos? a) (= a b c)))

(defn eval-game
  "Evaluates all possible win states"
  [game_state]
  (or
    (win-variant? (game_state 0) (game_state 1) (game_state 2))
    (win-variant? (game_state 3) (game_state 4) (game_state 5))
    (win-variant? (game_state 6) (game_state 7) (game_state 8))
    (win-variant? (game_state 0) (game_state 3) (game_state 6))
    (win-variant? (game_state 1) (game_state 4) (game_state 7))
    (win-variant? (game_state 2) (game_state 5) (game_state 8))
    (win-variant? (game_state 0) (game_state 4) (game_state 8))
    (win-variant? (game_state 2) (game_state 4) (game_state 6))))

(defn beginner?
  "Decides who begins"
  []
  (= 1 1))

(defn check-gamer?
  "Check for the integer representing the gamer that has hit first"
  [first-hitter user_id]
  (if (= first-hitter user_id)
    2                                                       ;o
    1                                                       ;x
    ))

(defn winner?
  "Checks if given user is also winner"
  [my-user game-hitter opponent]
  (if (= game-hitter my-user)
    opponent
    my-user))

(defn game-looser?
  "Check who was the looser of the game depending on winner"
  [game_winner my_user opponent]
  (if (= game_winner my_user)
    opponent
    my_user))


;; ------------------------
;; Click Handler

(defn click-invite
  "Click invite event handler"
  [sender_id receiver_id]
  (ws/send-message!
    {:action (action :invite) :value {:sender_id sender_id :receiver_id receiver_id}})
  (rf/dispatch [:set-inviting-user receiver_id]))

(defn click-logout
  "Click logout event handler"
  [my-user]
  (ws/send-message! {:action (action :logout) :value {:user_id my-user}})
  (rf/dispatch [:set-my-user nil])
  (swap! show_login? not)
  (if @show_game?
    (swap! show_game? not)))

(defn click-accept
  "Click accept invite event handler"
  [accepter inviter]
  (ws/send-message! {:action (action :accept) :value {:accepter accepter :inviter inviter}}))

(defn click-game-hit
  "Click game hit event handler"
  [sender_id receiver_id game_hitter hit_number game_state]
  (if (and (= sender_id game_hitter) (= 0 (game_state hit_number)))
    (ws/send-message! {:action (action :game-hit) :value {:sender_id sender_id :receiver_id receiver_id :hit_number hit_number}})
    (println "Not a possible action for you duuude!")))

(defn click-play-again
  "Click new game event handler"
  [sender_id receiver_id]
  (ws/send-message! {:action (action :game-reset) :value {:sender_id sender_id :receiver_id receiver_id}}))

(defn click-back-to-lobby
  "click back to lobby button"
  []
  (swap! show_game? not))


;; ------------------------
;; Components

(defn user_id_input []
  "Input form to submit user id for login"
  (let [value (atom nil)]
    (fn []
      [:input.form-control
       {:type        :text
        :placeholder "type in a username and press enter"
        :value       @value
        :on-change   #(reset! value (-> % .-target .-value))
        :on-key-down
                     #(when (= (.-keyCode %) 13)
                        (ws/send-message!
                          {:action (action :login) :value {:user_id @value}})
                        (rf/dispatch [:set-my-user @value])
                        (reset! value nil)
                        (swap! show_login? not))}])))

(defn player-list
  "Defining the list of players"
  []
  [:div.player-list-container
   [:h3 "Game Lobby"]
   [:h5 "player online:"]
   [:ul.player-list
    (let [user_ids @(rf/subscribe [:user-list])
          my-user @(rf/subscribe [:my-user])]
      (for [[index user_id] (map-indexed vector user_ids)]
        ;just write all other users not my-user
        (if-not (= user_id my-user)
          ^{:key index}
          [:li
           [:button.player-list-button
            {:on-click #(click-invite @(rf/subscribe [:my-user]) user_id)}
            [:img.player-list-icon {:src "/img/add_user.png"}]
            [:span.player-name user_id]]])))]])

(defn game-status
  "Defining the game status information"
  []
  [:div.game-status-container
   [:h2 "Let's play!"]
   [:div (str "You are: " @(rf/subscribe [:my-user]))]
   [:div (str @(rf/subscribe [:game-hitter]) " is playing...")]
   (if @game_winner
     [:div
      [:div (str "The winner is: " @game_winner "!")]
      [:button.left-side-button
       {:on-click #(click-play-again @(rf/subscribe [:my-user]) @(rf/subscribe [:opponent]))}
       "Play again"]
      [:button.left-side-button
       {:on-click #(click-back-to-lobby)}
       "Go back to Lobby"]])])


;; -------------------------
;; Page container

(defn game-container
  "Game content container"
  []
  [:div.container.game-container
   [:div
    [:ul.game
     (let [my-user @(rf/subscribe [:my-user])
           opponent @(rf/subscribe [:opponent])
           game-hitter @(rf/subscribe [:game-hitter])
           game-state @(rf/subscribe [:game-state])
           first-hitter @(rf/subscribe [:game-first-hitter])]
       (if (eval-game game-state)
         (reset! game_winner (winner? my-user game-hitter opponent)))
       (for [index (range 0 9)]
         (case (game-state index)
           ;check if the user id is 1 or 2 to set the right hover color for unclicked fields
           0
           (if (= (check-gamer? first-hitter my-user) 2)
             ^{:key index}
             [:li.game-field.game-field-hover-x
              {:on-click #(click-game-hit my-user opponent game-hitter index game-state)}]
             ^{:key index}
             [:li.game-field.game-field-hover-o
              {:on-click #(click-game-hit my-user opponent game-hitter index game-state)}])
           ;set the right color for already clicked fields
           1
           ^{:key index}
           [:li.game-field.game-field-clicked-x
            {:on-click #(click-game-hit my-user opponent game-hitter index game-state)}]
           2
           ^{:key index}
           [:li.game-field.game-field-clicked-o
            {:on-click #(click-game-hit my-user opponent game-hitter index game-state)}])))]]])


(defn main-container
  []
  [:div.container.main-container
   (if-not @show_game?
     [:div.login-title "TicTacToe"]
     [game-container])
   [:div.left-side-container
    (if @show_login?
      [:div.login-field-container
       [user_id_input]])
    (if @show_invite?
      [:button.top-right-side-button.invite-button
       {:on-click #(click-accept @(rf/subscribe [:my-user]) @(rf/subscribe [:invited-by-user]))}
       (str "Accept invitation by: ") @(rf/subscribe [:invited-by-user])])
    (if-not @show_login?
      [:div
       (if-not @show_game?
         [:div [player-list]]
         [:div [game-status]])
       [:div
        [:div.top-right-user-name @(rf/subscribe [:my-user])]
        [:button.top-right-side-button.logout-button
         {:on-click #(click-logout @(rf/subscribe [:my-user]))}
         "Logout"
         [:img.logout-icon {:src "/img/logout.png"}]]]])]])


;; -------------------------
;; Page template

(defn home-page
  "Defining the main container of the home page"
  []
  [main-container])


(def pages
  {:home #'home-page})

(defn page []
  [:div
   [(pages @(rf/subscribe [:page]))]])


;; -----------------------
;; Actions (after websocket message receive)

(defn _update-list
  "Defines what happens when receive-list event is received"
  [value]
  (rf/dispatch [:update-user-list value]))

(defn _invite
  "Defines what happens when invite event is received"
  [value]
  (if (= @(rf/subscribe [:my-user]) (value :receiver_id))
    (do (swap! show_invite? not)
        (rf/dispatch [:set-invited-by-user (value :sender_id)]))))

(defn _accept
  "Defines what happens when accept event is received"
  [value]
  (println "Accept receive: ")
  (let [my-user @(rf/subscribe [:my-user])]
    (if (or
          (= my-user (value :accepter))
          (= my-user (value :inviter)))
      (do (println "Lets gaaaame!!!")
          (if (= my-user (value :accepter))
            (rf/dispatch [:set-opponent (value :inviter)])
            (rf/dispatch [:set-opponent (value :accepter)]))

          ; Sets beginner of the game randomly
          (if (beginner?)
            (do
              (rf/dispatch [:set-game-hitter (value :inviter)])
              (rf/dispatch [:set-game-first-hitter (value :inviter)]))
            (do
              (rf/dispatch [:set-game-hitter (value :accepter)])
              (rf/dispatch [:set-game-first-hitter (value :accepter)])))
          (reset! show_invite? false)
          (rf/dispatch [:reset-game-state])
          (swap! show_game? not)))))



(defn _game-hit
  "Defines what happens when game-hit event is received"
  [value]
  (let [my-user @(rf/subscribe [:my-user])
        sender_id (value :sender_id)
        receiver_id (value :receiver_id)
        hit_number (value :hit_number)
        first-hitter @(rf/subscribe [:game-first-hitter])]
    (if (targets-me? my-user sender_id receiver_id)
      (do
        (if (= my-user receiver_id)
          ; happens when my-user is receiver of the hit
          (do
            (println "My opponent hitted field:" hit_number)
            (rf/dispatch [:set-game-state (check-gamer? first-hitter my-user) hit_number])
            (rf/dispatch [:set-game-hitter my-user]))

          ; happens when my-user was sender of the hit
          (do
            (println "I hitted field" hit_number)
            (rf/dispatch [:set-game-state (check-gamer? first-hitter receiver_id) hit_number])
            (rf/dispatch [:set-game-hitter receiver_id])))))))

(defn _game-reset
  "Defines what happens when receive-list event is received"
  [value]
  (println "Game Reset receive: ")
  (if (targets-me? @(rf/subscribe [:my-user]) (value :sender_id) (value :receiver_id))
    (do
      (rf/dispatch [:reset-game-state])
      ; Sets looser as first game hitter for the next game
      (let [game_looser (game-looser? @game_winner @(rf/subscribe [:my-user]) @(rf/subscribe [:opponent]))]
        (rf/dispatch [:set-game-first-hitter game_looser])
        (rf/dispatch [:set-game-hitter game_looser]))
      (println "Game should be resetted"))))

(defn message-receiver!
  "Update Function that receives all websocket messages"
  [{:keys [action value]}]
  (case action
    "receive-list" (_update-list value)
    "invite" (_invite value)
    "accept" (_accept value)
    "game-hit" (_game-hit value)
    "game-reset" (_game-reset value)))


;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
                    (rf/dispatch [:set-active-page :home]))


;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


;; -------------------------
;; Initialize app


; (defn fetch-docs! []
;  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}) )

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  ;  (fetch-docs!)
  (hook-browser-navigation!)
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") message-receiver!)
  (mount-components))




