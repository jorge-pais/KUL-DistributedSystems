package be.kuleuven.distributedsystems.cloud.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ticket {
    private String trainCompany;
    private UUID trainId;
    private UUID seatId;
    private UUID ticketId;
    private String customer;
    private String bookingReference;

    public Ticket() {
    }

    public Ticket(String trainCompany, UUID trainId, UUID seatId, UUID ticketId, String customer, String bookingReference) {
        this.trainCompany = trainCompany;
        this.trainId = trainId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
        this.bookingReference = bookingReference;
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

    public UUID getTicketId() {
        return this.ticketId;
    }

    public String getCustomer() {
        return this.customer;
    }

    public String getBookingReference() {
        return this.bookingReference;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("trainCompany", this.trainCompany);
        map.put("trainId", this.trainId.toString());
        map.put("seatId", this.seatId.toString());
        map.put("ticketId", this.ticketId.toString());
        map.put("customer", this.customer);
        map.put("bookingReference", this.bookingReference);
        return map;
    }

    public void setseatId(String seatId) {
        this.seatId = UUID.fromString(seatId);
    }

    public void setTicketId(String ticketId) {
        this.ticketId = UUID.fromString(ticketId);
    }

    public void setTrainId(String trainId) {
        this.trainId = UUID.fromString(trainId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ticket)) {
            return false;
        }
        var other = (Ticket) o;
        return this.ticketId.equals(other.ticketId)
                && this.seatId.equals(other.seatId)
                && this.trainId.equals(other.trainId)
                && this.trainCompany.equals(other.trainCompany);
    }

    @Override
    public int hashCode() {
        return this.trainCompany.hashCode() * this.trainId.hashCode() * this.seatId.hashCode() * this.ticketId.hashCode();
    }
}