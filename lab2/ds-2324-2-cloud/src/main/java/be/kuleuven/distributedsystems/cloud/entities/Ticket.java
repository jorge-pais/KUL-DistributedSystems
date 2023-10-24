package be.kuleuven.distributedsystems.cloud.entities;

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
