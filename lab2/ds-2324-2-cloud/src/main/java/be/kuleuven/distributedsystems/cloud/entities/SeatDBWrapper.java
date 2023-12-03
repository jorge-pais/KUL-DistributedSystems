package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SeatDBWrapper {

    private String trainCompany;
    private UUID trainId;
    private UUID seatId;
    private LocalDateTime time;
    private String type;
    private String name;
    private double price;
    private boolean available;

    public SeatDBWrapper(){}

    public SeatDBWrapper(LocalTrain.LocalSeat localSeat, Train train){
        this.time = LocalDateTime.parse(localSeat.getTime());
        this.name = localSeat.getName();
        this.seatId = UUID.randomUUID();
        this.price = localSeat.getPrice();
        this.type = localSeat.getType();
        this.trainCompany = train.getTrainCompany();
        this.trainId = train.getTrainId();
        this.available = true;
    }

    public SeatDBWrapper(Seat seat, boolean available){
        this.time = seat.getTime();
        this.name = seat.getName();
        this.seatId = seat.getSeatId();
        this.price = seat.getPrice();
        this.type = seat.getType();
        this.trainCompany = seat.getTrainCompany();
        this.trainId = seat.getTrainId();
        this.available = available;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> seatMap = new HashMap<>();
        seatMap.put("trainCompany", trainCompany);
        seatMap.put("trainId", trainId.toString());
        seatMap.put("seatId", seatId.toString());

        // Format LocalDateTime with seconds
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        seatMap.put("time", time.format(formatter));

        seatMap.put("type", type);
        seatMap.put("name", name);
        seatMap.put("price", price);
        seatMap.put("available", available);
        return seatMap;
    }

    public String getTrainCompany() {
        return trainCompany;
    }

    public void setTrainCompany(String trainCompany) {
        this.trainCompany = trainCompany;
    }

    public UUID getTrainId() {
        return trainId;
    }

    public void setTrainId(String trainId) {
        this.trainId = UUID.fromString(trainId);
    }

    public UUID getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = UUID.fromString(seatId);
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = LocalDateTime.parse(time);
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
