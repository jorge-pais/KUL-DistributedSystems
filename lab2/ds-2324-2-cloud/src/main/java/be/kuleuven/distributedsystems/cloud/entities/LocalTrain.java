package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public class LocalTrain {

    private String name;
    private String location;
    private String image;
    private Collection<LocalSeat> seats;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Collection<LocalSeat> getSeats() {
        return seats;
    }

    public void setSeats(Collection<LocalSeat> seats) {
        this.seats = seats;
    }

    public static class LocalSeat{
        private String time;
        private String type;
        private String name;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        private double price;
    }

}