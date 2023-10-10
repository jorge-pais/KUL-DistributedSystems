package hotel;

import shared.IBookingManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingManager implements IBookingManager {

	private final Room[] rooms;

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

		for (Room room: this.rooms) {
			if (room.getRoomNumber().equals(roomNumber)) {
				List<BookingDetail> bookings = room.getBookings();
				for (BookingDetail booking : bookings) {
					if (booking.getDate().isEqual(date)) {
						return false; // Room is already booked for the given date
					}
				}
				return true; // Room is available for the given date
			}
		}
		return false; // Room not found
	}

	public boolean addBooking(BookingDetail bookingDetail) {

		Room room = getRoomById(bookingDetail.getRoomNumber());
		assert room != null;
		room.getLock().lock();
		try{
			if (isRoomAvailable(bookingDetail.getRoomNumber(), bookingDetail.getDate())) {
				room.getBookings().add(bookingDetail);
				room.getLock().unlock();
				return true;
			}
		}catch (Exception e){
			e.printStackTrace();
			room.getLock().unlock();
			return false;
		}
		room.getLock().unlock();
		return false;
	}

	public Set<Integer> getAvailableRooms(LocalDate date) {
		//implement this method
		Set<Integer> availableRooms = new HashSet<>();
		for (Room room : rooms) {
			if (isRoomAvailable(room.getRoomNumber(), date)) {
				availableRooms.add(room.getRoomNumber());
			}
		}
		return availableRooms;
	}
	
	private Room getRoomById(Integer roomNumber){
		for (Room room: this.rooms)
			if(room.getRoomNumber().equals(roomNumber))
				return room;
		
		return null;
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
