(ns nativekloud.pubsub

  (:import
   (com.google.pubsub.v1
    PubsubMessage
    ProjectTopicName
    ProjectSubscriptionName)
   (com.google.api.core
    ApiService$Listener
    ApiFuture)
   (com.google.cloud.pubsub.v1
    TopicAdminClient
    SubscriptionAdminClient
    Publisher
    AckReplyConsumer
    MessageReceiver
    AckReplyConsumer
    Subscriber
    )
   com.google.protobuf.ByteString
   [java.util.concurrent TimeUnit]
   ))


(defn publisher [topic]
  (.build (Publisher/newBuilder topic)))

(defn topic [project-id topic]
  (ProjectTopicName/of project-id topic))

(defn set-data [data]
  (.build (.setData (PubsubMessage/newBuilder) (ByteString/copyFromUtf8 data))))

(defn list-topics [project_id]
  (let [client (TopicAdminClient/create)]
    (mapv #(.getName %) (iterator-seq (.iterator
                                       (.iterateAll
                                        (.listTopics client project_id)))))))

(defn list-subscriptions [project_id]
  (let [client (SubscriptionAdminClient/create)]
    (mapv #(.getName %) (iterator-seq (.iterator
                                       (.iterateAll
                                        (.listSubscriptions client project_id)))))))


(defn publish-async 
  "publish async returns future"
  [project_id topic-name msg]
  (let [topic (topic project_id topic-name)
        publisher (publisher topic)
        data  (set-data msg)]
    (try (.publish publisher data)
         (catch Exception e (prn "handle this ..."))
         (finally (if publisher (.awaitTermination (.shutdown publisher) 1 TimeUnit/MINUTES))))))

(defn subscribe
  "pull subscription and asynchronously pull messages from it."
  [project_id subscription callback stream]
  (let [subscription (ProjectSubscriptionName/of project_id subscription)
        reciver (reify MessageReceiver
                  (^void receiveMessage [_ ^PubsubMessage msg ^AckReplyConsumer consumer]
                   (callback  (.toStringUtf8 (.getData msg)) consumer)))
        subscriber (.build (Subscriber/newBuilder subscription reciver))]

    (.awaitRunning (.startAsync subscriber))
    subscriber)
  )
