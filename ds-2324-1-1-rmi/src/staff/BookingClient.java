package staff;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.util.Set;

import hotel.BookingDetail;
import shared.IBookingManager; // WE SHOULD NOT IMPORT THIS. BUT AN INTERFACE

public class BookingClient extends AbstractScriptedSimpleTest {
//public class BookingClient extends AbstractBetterTest {

	// TODO configure program arguments
	private static final String _hotelName = "HotelTuga";

	private IBookingManager bm = null;

	public static void main(String[] args) throws Exception {


		BookingClient client = new BookingClient();
		client.run();
	}

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public BookingClient() {
		try {
			if(System.getSecurityManager() != null)
				System.setSecurityManager((SecurityManager)null);

			//Look up the registered remote instance
			Registry registry = LocateRegistry.getRegistry();

			bm = (IBookingManager) registry.lookup(_hotelName);

			System.out.println("Registry for hotel found!");
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	@Override
	public boolean isRoomAvailable(Integer roomNumber, LocalDate date) {
		//Implement this method
		try{
			bm.isRoomAvailable(roomNumber, date);
		}catch (RemoteException e){
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void addBooking(BookingDetail bookingDetail) {
		//Implement this method
		try {
			if(bm.addBooking(bookingDetail))
				System.out.println(ProcessHandle.current().pid() + " Booking successful");
			else
				System.out.println(ProcessHandle.current().pid() + " Could not book the room");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<Integer> getAvailableRooms(LocalDate date) {
		//Implement this method
		try {
			return bm.getAvailableRooms(date);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Set<Integer> getAllRooms() {
		try {
			return bm.getAllRooms();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
}
