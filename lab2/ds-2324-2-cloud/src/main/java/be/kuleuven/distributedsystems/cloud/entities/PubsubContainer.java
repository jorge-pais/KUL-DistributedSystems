package be.kuleuven.distributedsystems.cloud.entities;

import java.util.Map;

public class PubsubContainer {
    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    private String subscription;

    public PubsubMessageWrapper getMessage() {
        return message;
    }

    public void setMessage(PubsubMessageWrapper message) {
        this.message = message;
    }

    private PubsubMessageWrapper message;

    // Getters and setters

    public static class PubsubMessageWrapper {
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        private String data;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        private String messageId;

        public String getMessage_id() {
            return message_id;
        }

        public void setMessage_id(String message_id) {
            this.message_id = message_id;
        }

        private String message_id;

        public String getPublishTime() {
            return publishTime;
        }

        public void setPublishTime(String publishTime) {
            this.publishTime = publishTime;
        }

        public String getPublish_time() {
            return publish_time;
        }

        public void setPublish_time(String publish_time) {
            this.publish_time = publish_time;
        }

        private String publishTime;

        private String publish_time;

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        private Map<String, String> attributes;

        // Getters and setters
    }
}
