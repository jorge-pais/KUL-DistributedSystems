package hotel;

import shared.IBookingManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HotelServer {

    private static final String _rentalCompanyName = "HotelTuga";
    private static final Logger logger = Logger.getLogger(HotelServer.class.getName());

    public HotelServer() {
    }

    // Faltam Exceptions
    public static void main(String[] args) {
        if (System.getSecurityManager() != null) {
            System.setSecurityManager((SecurityManager)null);
        }

        // Talvez indicar como argumento número de quartos do hotel?
        IBookingManager bm = new BookingManager();
        Registry registry = null;

        try {
            System.setProperty("java.rmi.server.hostname", "localhost");
            registry = LocateRegistry.createRegistry(8081);
        } catch (RemoteException var7) {
            logger.log(Level.SEVERE, "Could not locate RMI registry.");
            System.exit(-1);
        }

        try {
            IBookingManager stub = (IBookingManager) UnicastRemoteObject.exportObject(bm, 0);
            registry.rebind("HotelTuga", stub);
            logger.log(Level.INFO, "<{0}> Hotel {0} is registered.", "HotelTuga");
        } catch (RemoteException var6) {
            logger.log(Level.SEVERE, "<{0}> Could not get stub bound of Hotel {0}.", "HotelTuga");
            var6.printStackTrace();
            System.exit(-1);
        }

    }
}
