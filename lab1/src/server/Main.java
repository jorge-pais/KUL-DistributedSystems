package server;

import hotel.BookingManager;

public class Main {

    public static void main(String[] args){

        BookingManager bookingManager = new BookingManager();

        bookingManager.getAllRooms();
    }
}
