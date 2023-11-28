package be.kuleuven.distributedsystems.cloud.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Train {
    private String trainCompany;
    private UUID trainId;
    private String name;
    private String location;
    private String image;

    public Train() {
    }

    public Train(String trainCompany, UUID trainId, String name, String location, String image) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.name = name;
        this.location = location;
        this.image = image;
    }

    public Train(LocalTrain localTrain){
        this.trainCompany = "InternalTrains";
        this.trainId = UUID.randomUUID();
        this.name = localTrain.getName();
        this.location = localTrain.getLocation();
        this.image = localTrain.getImage();
    }

    public String getTrainCompany() {
        return trainCompany;
    }

    public UUID getTrainId() {
        return trainId;
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return this.location;
    }

    public String getImage() {
        return this.image;
    }

    public void setTrainId(String id) {
        this.trainId = UUID.fromString(id);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Train)) {
            return false;
        }
        var other = (Train) o;
        return this.trainCompany.equals(other.trainCompany)
                && this.trainId.equals(other.trainId);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode();
    }

    // Add this method to your Train class
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("trainCompany", this.trainCompany);
        map.put("trainId", this.trainId.toString());
        map.put("name", this.name);
        map.put("location", this.location);
        map.put("image", this.image);
        return map;
    }

}