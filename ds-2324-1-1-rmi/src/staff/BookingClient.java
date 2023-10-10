package staff;

import java.time.LocalDate;
import java.util.Set;

import hotel.BookingDetail;
import hotel.BookingManager; // WE SHOULD NOT IMPORT THIS. BUT AN INTERFACE

public class BookingClient extends AbstractScriptedSimpleTest {

	private BookingManager bm = null;

	public static void main(String[] args) throws Exception {
		BookingClient client = new BookingClient();
		client.run();
	}

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public BookingClient() {
		try {
			//Look up the registered remote instance
			bm = new BookingManager();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	@Override
	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		//Implement this method
		bm.isRoomAvailable(roomNumber, date);
		return true;
	}

	@Override
	public void addBooking(BookingDetail bookingDetail) {
		//Implement this method
		bm.addBooking(bookingDetail);
	}

	@Override
	public Set<Integer> getAvailableRooms(LocalDate date) {
		//Implement this method
		return bm.getAvailableRooms(date);
	}

	@Override
	public Set<Integer> getAllRooms() {
		return bm.getAllRooms();
	}
}
