package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.Http;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.pubsub.v1.*;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.util.StringList;
import org.checkerframework.checker.units.qual.A;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.google.cloud.firestore.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/*
* TODO!!!!!
* 1.    Replace all method signatures to include a responseEntity
*       i guess so far the spring framework has been handling this for us
*       but we should keep in mind that things fail and we should send the appropriate responses.
*       Right now unreliable trains breaks the application (sort of, it still works but it's shit)
*
* 2.    Do the firebase thing. Apart from storing the bookings, we should probably keep some of
*       trains and seats, so that the cart doesn't break.
*
* 3.    Cloud PUB/SUB. we have to modify the confirmQuotes so that it issues some job to a
*       remote worker in the cloud that deals with these instead of the user. Then out application
*       merely sends what is needed for it to work
*
* 4.    Do the ACID thing. I still don't understand it very well
* */

@RestController
@RequestMapping("/api")
public class TrainRestController {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;
    @Resource(name= "publisher")
    private Publisher publisher;
    @Resource(name= "subscriber")
    private Subscriber subscriber;
    @Resource(name = "db")
    private Firestore db;

    public static final Map<String, List<Booking>> allBookings = new HashMap<>();
    public static final Map<String, Seat> allSeats = new HashMap<>();
    public static final Map<String, Train> allTrains = new HashMap<>();

    private final String API_KEY = Application.getApiKey();

    // Pass this to database perhaps, URLs without https://
    private final String [] trainCompanies = {"reliabletrains.com", "unreliabletrains.com"};

    @GetMapping(path = "/getTrains")
    public Collection<Train> getAllTrains() throws NullPointerException{

        ArrayList<Train> trains = new ArrayList<>();

        for (String i: trainCompanies) {
            try {
                trains.addAll(getAllTrainsFrom(i));
            } catch (Exception e) {
                System.out.println("Couldn't GET trains from " + i);
            }
        }

        return trains;
    }

    // AUX function for getAllTrains()
    private Collection<Train> getAllTrainsFrom(String trainCompany) throws NullPointerException{

        return webClientBuilder
                .baseUrl("https://" + trainCompany)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("trains")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Train>>(){})
                .block()
                .getContent();
    }

    @GetMapping(path = "/getTrain")
    public Train getTrain(@RequestParam String trainId, @RequestParam String trainCompany) throws NullPointerException{
        Train train = allTrains.get(trainId);

        if(train == null) {
            train = webClientBuilder
                .baseUrl("https://" + trainCompany)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("trains", trainId)
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(Train.class)
                .block();
            allTrains.put(trainId, train);
        }

        return train;
    }

    @GetMapping(path = "/getTrainTimes")
    public Collection<String> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId) throws NullPointerException{

        return webClientBuilder
                .baseUrl("https://" + trainCompany)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("trains")
                        .pathSegment(trainId)
                        .pathSegment("times")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>(){})
                .block()
                .getContent();
    }

    @GetMapping(path = "/getAvailableSeats")
    public Map<String, Collection<Seat>> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
        Mono<ResponseEntity<CollectionModel<Seat>>> responseEntityMono = webClientBuilder
                .baseUrl("https://" + trainCompany)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("trains", trainId, "seats")
                        .queryParam("time", time)
                        .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<CollectionModel<Seat>>(){});

        ResponseEntity<CollectionModel<Seat>> response = responseEntityMono.block();
        HttpStatusCode statusCode = response.getStatusCode();
        if(!statusCode.is2xxSuccessful()) return null;
        Collection<Seat> seats = response.getBody().getContent();

        Map<String, Collection<Seat>> map = new HashMap<>();

        ArrayList<Seat> secondClass = new ArrayList<>();
        ArrayList<Seat> firstClass = new ArrayList<>();
        for(Seat seat : seats)
            if(seat.getType().equals("2nd class"))
                secondClass.add(seat);
            else
                firstClass.add(seat);

        firstClass.sort(Seat.seatComparator);
        secondClass.sort(Seat.seatComparator);

        map.put("1st class", firstClass);
        map.put("2nd class", secondClass);

        return map;
    }

    @GetMapping(path = "/getSeat")
    public Seat getSeat(@RequestParam String trainId, @RequestParam String trainCompany, @RequestParam String seatId){
        // Should add the seat to the database if successful
        Seat seat = allSeats.get(seatId);
        if(seat == null) {
            seat = webClientBuilder
                    .baseUrl("https://" + trainCompany)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", trainId, "seats", seatId)
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(Seat.class)
                    .block();
            allSeats.put(seatId, seat);
        }
        return seat;
    }

    // TODO: Apply PUB/SUB to this
    @PostMapping(path = "/confirmQuotes")
    public ResponseEntity<?> confirmQuotes(@RequestBody Collection<Quote> quotes) throws ExecutionException, InterruptedException {
        String customer = SecurityFilter.getUser().getEmail();
        UUID bookingReference = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();
        List<Map<String, Object>> ticketMaps = new ArrayList<>();

        /// THESE ARE THE REQUESTS THAT ARE MADE TO THE SERVER
        for(Quote quote: quotes){
            Ticket ticket = null;

            Mono<ResponseEntity<Ticket>> responseMono = webClientBuilder // PUT request
                    .baseUrl("https://" + quote.getTrainCompany())
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", quote.getTrainId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("key", API_KEY)
                            .queryParam("customer", customer)
                            .queryParam("bookingReference", bookingReference.toString())
                            .build())
                    .retrieve()
                    .toEntity(Ticket.class);

            try {
                ResponseEntity<Ticket> response = responseMono.block();

                HttpStatusCode httpStatus = response.getStatusCode();
                if (httpStatus.is2xxSuccessful()) {
                    ticket = response.getBody();

                    tickets.add(ticket);
                    ticketMaps.add(ticket.toMap());
                }
            }catch (Exception e){
                System.out.println("Couldn't put the ticket");
            }
        }
        // remove all tickets if the number of booked tickets is lower than number of quotes in booking request
        if(tickets.size() != quotes.size()) {
            for (Ticket ticket : tickets) { // Should probably do a while loop that checks for a successful remove
                HttpStatusCode httpStatus = HttpStatus.NOT_FOUND;

                // this is in case unreliabletrains is not responding to a remove operation,
                // in which case we definitely want to spam the remove requests.
                while (!httpStatus.is2xxSuccessful()) {
                    try {
                        Mono<ResponseEntity<String>> responseEntity = webClientBuilder.baseUrl("https://" + ticket.getTrainCompany())
                                .build()
                                .delete()
                                .uri(uriBuilder -> uriBuilder
                                        .pathSegment("trains", ticket.getTrainId().toString(),
                                                "seats", ticket.getSeatId().toString(), "ticket", ticket.getTicketId().toString())
                                        .queryParam("key", API_KEY)
                                        .build())
                                .retrieve()
                                .toEntity(String.class); // This should return 204

                        httpStatus = Objects.requireNonNull(responseEntity.block()).getStatusCode();
                        System.out.println("Status was: " + httpStatus.toString());
                    }catch (Exception e){
                        System.out.println("Could remove the ticket: " + ticket.getSeatId());
                        httpStatus = HttpStatus.NOT_FOUND;
                    }
                }
            }
            return ResponseEntity.internalServerError().build();
        }

        DocumentReference docRef = db.collection("allbookings").document(customer)
                .collection("bookings").document(bookingReference.toString());

        // DocumentReference docRef = db.collection("allbookings").document(customer);
        // Add document data  with id "allbookings" using a hashmap
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", bookingReference.toString());
        bookingData.put("time", LocalDateTime.now().toString());
        bookingData.put("tickets", ticketMaps);
        bookingData.put("customer", customer);

        ApiFuture<WriteResult> future = docRef.set(bookingData);
        try {
            future.get(); // Wait until operation is completed.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        DocumentReference customerDocRef = db.collection("allbookings").document(customer);

        // Check if the document exists
        ApiFuture<DocumentSnapshot> customerFuture = customerDocRef.get();
        DocumentSnapshot customerDocument = customerFuture.get();

        if (customerDocument.exists()) {
            // If the document exists, get the current totalTickets value
            int totalTickets = customerDocument.getLong("totalTickets").intValue();

            // Increment the totalTickets value by the number of tickets bought in the current transaction
            totalTickets += tickets.size();

            // Update the document with the new totalTickets value
            customerDocRef.update("totalTickets", totalTickets);
        } else {
            // If the document doesn't exist, create a new one with totalTickets set to the number of tickets bought
            customerDocRef.set(Map.of("totalTickets", tickets.size()));
        }



        return ResponseEntity.ok().build();



        // PUBSUB TEST CODE HERE
        /*PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .putAttributes("user", SecurityFilter.getUser().getEmail())
                .build();

        ApiFuture<String> future = publisher.publish(pubsubMessage);

        System.out.println(publisher.getTopicNameString());*/
    }

    @PostMapping("/subscription")
    public static void handleConfirmQuotes(@RequestBody String body){

    }

    @GetMapping(path = "/getBookings")
    public Collection<Booking> getBookings(){
        String customer = SecurityFilter.getUser().getEmail();
        ArrayList<Booking> bookings = new ArrayList<>();

        // Create a query against the "bookings" subcollection.
        ApiFuture<QuerySnapshot> future = db.collection("allbookings")
                .document(customer)
                .collection("bookings")
                .get();

        try {
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot document : documents) {
                Booking booking = document.toObject(Booking.class);
                bookings.add(document.toObject(Booking.class));
            }

            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(path = "/getAllBookings")
    public Collection<Booking> getAllBookings(){
        List<String> roles = List.of(SecurityFilter.getUser().getRoles());

        // Check if the user has the "manager" role
        if (!roles.contains("manager")) return null;

        // Create a query against the "allbookings" collection
        ApiFuture<QuerySnapshot> future = db.collection("allbookings").get();

        try {
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Booking> bookings = new ArrayList<>();

            // Process each document in parallel
            for (DocumentSnapshot document : documents) {
                CollectionReference bookingsCollection = document.getReference().collection("bookings");
                ApiFuture<QuerySnapshot> bookingsFuture = bookingsCollection.get();

                try {
                    List<QueryDocumentSnapshot> bookingDocuments = bookingsFuture.get().getDocuments();
                    for (DocumentSnapshot bookingDocument : bookingDocuments) {
                        bookings.add(bookingDocument.toObject(Booking.class));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new CompletionException(e);
                }
            }

            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            throw new CompletionException(e);
        }
    }

    // This function seems kinda inefficient
    @GetMapping(path = "/getBestCustomers")
    public Collection<String> getBestCustomer(){
        List<String> roles = List.of(SecurityFilter.getUser().getRoles());
        if (!roles.contains("manager")) return null;

        ApiFuture<QuerySnapshot> future = db.collection("allbookings").get();
        List<QueryDocumentSnapshot> documents;
        ArrayList<String> bestCustomers = new ArrayList<>();
        long maxTickets = 0;

        try {
            documents = future.get().getDocuments();
            Map<String, Long> ticketCounts = new HashMap<>();

            for (DocumentSnapshot document : documents) {
                String customer = document.getId();
                long tickets = (Long) document.get("totalTickets");
                ticketCounts.put(customer, tickets);
            }

            for (Map.Entry<String, Long> entry : ticketCounts.entrySet()) {
                if (entry.getValue() > maxTickets) {
                    maxTickets = entry.getValue();
                    bestCustomers.clear();
                    bestCustomers.add(entry.getKey());
                } else if (entry.getValue() == maxTickets) {
                    bestCustomers.add(entry.getKey());
                }
            }

            return bestCustomers;
        } catch (InterruptedException | ExecutionException e) {
            throw new CompletionException(e);
        }
    }
}