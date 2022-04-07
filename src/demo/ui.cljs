(ns demo.ui
 (:require
    [reagent.core :as r]
    [clojure.string :as string]))

(defn search
  "Given query, returns a promise that returns the videoID of the first result"
  [query]
  (-> (js/fetch (str "https://www.googleapis.com/youtube/v3/search?"
                     (->> {:key "CHANGEME"
                           :part "snippet"
                           :q query
                           :type "video"
                           :videoCategoryId "10"
                           :order "relevance" #_"viewCount"}
                          (map (fn [[k v]] (str (name k) "=" v)))
                          (string/join "&"))))
      (.then (fn [response]
              (.json response)))
      (.then (fn [response]
              (let [items ^js/Array (.-items response)]
               (.. items (at 0) -id -videoId))))))

(defonce state (r/atom {:song-list ["Coldplay - Clocks"
                                    "Wesley Willis - Rock N Roll McDonalds"]
                        :current-song-index 0
                        :video-id nil}))

(defn play-current-song! []
 (.then (search (get (:song-list @state) (:current-song-index @state)))
        (fn [id] (swap! state assoc :video-id id))))

(defn app-view []
  [:div
   [:textarea {:value (string/join "\n" (:song-list @state))
               :style {:width "40em"}
               :on-change (fn [e] (swap! state assoc :query (.. e -target -value)))}]
   "Current Song: " (get (:song-list @state) (:current-song-index @state))
   [:button {:on-click play-current-song!}
    "Play"]
   (when (:video-id @state)
    [:iframe {:id "player"
              :type "text/html"
              :width "640"
              :height "390"
              :src (str "http://www.youtube.com/embed/" (:video-id @state) "?enablejsapi=1&origin=http://localhost")
              :frame-border "0"}])])

#_(.then (search "Coldplay Clocks") (fn [id] (println id)))
