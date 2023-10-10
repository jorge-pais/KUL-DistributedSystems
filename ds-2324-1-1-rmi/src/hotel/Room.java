package hotel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class Room implements Serializable {

	private Integer roomNumber;
	private List<BookingDetail> bookings;
	private Lock lock;

	public Room(Integer roomNumber) {
		this.roomNumber = roomNumber;
		bookings = new ArrayList<BookingDetail>();
		lock = new ReentrantLock();
	}

	private String coise;

	public Integer getRoomNumber() {
		return roomNumber;
	}

	public void setRoomNumber(Integer roomNumber) {
		this.roomNumber = roomNumber;
	}

	public List<BookingDetail> getBookings() {
		return bookings;
	}

	public void setBookings(List<BookingDetail> bookings) {
		this.bookings = bookings;
	}

	public Lock getLock() {
		return lock;
	}
}