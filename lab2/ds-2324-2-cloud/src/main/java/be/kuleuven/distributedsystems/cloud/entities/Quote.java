package be.kuleuven.distributedsystems.cloud.entities;

import java.util.UUID;

public class Quote {

    private String trainCompany;
    private UUID trainId;
    private UUID seatId;

    public Quote() {
    }

    public Quote(String trainCompany, UUID trainId, UUID seatId) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.seatId = seatId;
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

    public void setTrainId(UUID trainId) {
        this.trainId = trainId;
    }

    public UUID getSeatId() {
        return this.seatId;
    }

    public void setSeatId(UUID seatId) {
        this.seatId = seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quote)) {
            return false;
        }
        var other = (Quote) o;
        return this.trainCompany.equals(other.trainCompany)
                && this.trainId.equals(other.trainId)
                && this.seatId.equals(other.seatId);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode() * this.seatId.hashCode();
    }
}
