(ns demo.ui
 (:require
    [reagent.core :as r]
    [clojure.string :as string]))

(defn yt-inject-iframe-script! []
  (let [tag (.. js/document (createElement "script"))
        first-script-tag (aget (.. js/document (getElementsByTagName "script")) 0)]
    (set! (.-src tag) "https://www.youtube.com/iframe_api")
    (.. first-script-tag -parentNode (insertBefore tag first-script-tag))))

(defonce yt-player (atom nil))

(defonce state
  (r/atom {:yt-script-loaded? false
           :song-list-text "Coldplay - Clocks\nWesley Willis - Rock N Roll McDonalds"
           :song-list ["Coldplay - Clocks"
                       "Wesley Willis - Rock N Roll McDonalds"]
           :current-song-index 0
           :video-id nil}))

(defn yt-search!
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

#_(.then (yt-search! "Coldplay Clocks") (fn [id] (println id)))

(defn play-current-song! []
 (when-let [song-name (get (:song-list @state) (:current-song-index @state))]
  (.then (yt-search! song-name)
         (fn [id] (.loadVideoById ^js/Object @yt-player id)))))

(defn play-next-song-if-able! []
  (swap! state assoc :current-song-index (let [next-index (inc (:current-song-index @state))
                                               song-count (count (:song-list @state))]
                                           (if (<= next-index (dec song-count))
                                            next-index
                                            0)))
  (play-current-song!))

(defn on-yt-player-ready! []
   (println "PLAYER READY"))

(defn on-yt-state-change! [e]
   (case (.-data e)
     0 ;; ended
     (play-next-song-if-able!)
     nil))

(defn on-yt-iframe-api-ready! []
  (reset! yt-player
          (js/YT.Player. "player" #js {:height 390
                                       :width 640
                                       :videoId "M7lc1UVf-VE"
                                       :playerVars #js {:playsinline 1}
                                       :events #js {:onReady on-yt-player-ready!
                                                    :onStateChange on-yt-state-change!}})))

;; youtube iframe script expects window.onYouTubeIframeAPIReady to exist
(set! (.-onYouTubeIframeAPIReady js/window) on-yt-iframe-api-ready!)

(defn app-view []
  [:div
   [:textarea {:value (:song-list-text @state)
               :style {:width "40em"}
               :on-change (fn [e]
                            (swap! state assoc :song-list-text (.. e -target -value))
                            (swap! state assoc :song-list
                                   (string/split (.. e -target -value) #"\n")))}]
   "Current Song: " (get (:song-list @state) (:current-song-index @state))
   [:button {:on-click play-current-song!}
    "Play"]
   [:button {:on-click play-next-song-if-able!}
    "Next"]
   [:div#player {:ref (fn [] (when (:yt-script-loaded? @state)
                                (on-yt-iframe-api-ready!)))}]
   (when-not (:yt-script-loaded? @state)
     (yt-inject-iframe-script!)
     nil)])
