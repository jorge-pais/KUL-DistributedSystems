package hotel;

import java.time.LocalDate;
import java.util.*;

public class BookingManager {

	private Room[] rooms;

	public BookingManager() {
		this.rooms = initializeRooms();
	}

	public Set<Integer> getAllRooms() {
		Set<Integer> allRooms = new HashSet<Integer>();
		Iterable<Room> roomIterator = Arrays.asList(rooms);
		for (Room room : roomIterator) {
			allRooms.add(room.getRoomNumber());
		}
		return allRooms;
	}

	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		//implement this method

		Room room = rooms[0];
		for (Room i : rooms)
			if (i.getRoomNumber().equals(roomNumber)) {
				room = i;
				break;
			}

		List<BookingDetail> bookings = room.getBookings();

		for (BookingDetail i: bookings)
			if(i.getDate().equals(date))
				return false;

		return true;
	}

	public void addBooking(BookingDetail bookingDetail) {
		//implement this method

		if(!isRoomAvailable(bookingDetail.getRoomNumber(), bookingDetail.getDate())) {
			System.out.println("Num dá bro");
			return;
		}

		Room room = rooms[bookingDetail.getRoomNumber()];

		room.getBookings().add(bookingDetail);
	}

	public Set<Integer> getAvailableRooms(LocalDate date) {
		//implement this method

		Set<Integer> availableRooms = new HashSet<>();

		for(Room i: rooms)
			if(isRoomAvailable(i.getRoomNumber(), date))
				availableRooms.add(i.getRoomNumber());

		return availableRooms;
	}

	private static Room[] initializeRooms() {
		Room[] rooms = new Room[4];
		rooms[0] = new Room(101);
		rooms[1] = new Room(102);
		rooms[2] = new Room(201);
		rooms[3] = new Room(203);
		return rooms;
	}
}
