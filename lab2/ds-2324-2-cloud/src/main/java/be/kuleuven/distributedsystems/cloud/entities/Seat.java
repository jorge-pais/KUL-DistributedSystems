package be.kuleuven.distributedsystems.cloud.entities;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Seat {
    private String trainCompany;
    private UUID trainId;
    private UUID seatId;
    private LocalDateTime time;
    private String type;
    private String name;
    private double price;

    public Seat() {
    }

    public Seat(String trainCompany, UUID trainId, UUID seatId, LocalDateTime time, String type, String name, double price) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
    }

    public Seat(LocalTrain.LocalSeat localSeat, Train train){
        this.time = LocalDateTime.parse(localSeat.getTime());
        this.name = localSeat.getName();
        this.seatId = UUID.randomUUID();
        this.price = localSeat.getPrice();
        this.type = localSeat.getType();
        this.trainCompany = train.getTrainCompany();
        this.trainId = train.getTrainId();
    }

    public String getTrainCompany() {
        return trainCompany;
    }

    public UUID getTrainId() {
        return trainId;
    }

    public UUID getSeatId() {
        return this.seatId;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public double getPrice() {
        return this.price;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Seat)) {
            return false;
        }
        var other = (Seat) o;
        return this.trainCompany.equals(other.trainCompany)
                && this.trainId.equals(other.trainId)
                && this.seatId.equals(other.seatId);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode() * this.seatId.hashCode();
    }

    public static Comparator<Seat> seatComparator = (Seat s1, Seat s2) -> {
        Pattern pattern = Pattern.compile("(\\d+)(\\D+)");
        Matcher m1 = pattern.matcher(s1.getName());
        Matcher m2 = pattern.matcher(s2.getName());

        if (m1.find() && m2.find()) {
            int number1 = Integer.parseInt(m1.group(1));
            int number2 = Integer.parseInt(m2.group(1));
            String row1 = m1.group(2);
            String row2 = m2.group(2);

            int numberComparison = Integer.compare(number1, number2);
            if (numberComparison != 0) {
                return numberComparison;
            } else {
                return row1.compareTo(row2);
            }
        }

        return 0;
    };

    public Map<String, Object> toMap() {
        Map<String, Object> seatMap = new HashMap<>();
        seatMap.put("trainCompany", trainCompany);
        seatMap.put("trainId", trainId.toString());
        seatMap.put("seatId", seatId.toString());
        seatMap.put("time", time.toString());
        seatMap.put("type", type);
        seatMap.put("name", name);
        seatMap.put("price", price);
        return seatMap;
    }

    public void setTrainId(String id) {
        this.trainId = UUID.fromString(id);
    }

    public void setSeatId(String id) {
        this.seatId = UUID.fromString(id);
    }

    public void setTime(String time) {
        this.time = LocalDateTime.parse(time);
    }

}