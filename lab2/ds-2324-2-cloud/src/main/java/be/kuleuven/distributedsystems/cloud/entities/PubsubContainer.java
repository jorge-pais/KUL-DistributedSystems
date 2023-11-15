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

