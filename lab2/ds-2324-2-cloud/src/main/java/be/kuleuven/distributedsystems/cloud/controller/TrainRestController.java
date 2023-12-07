package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/*      TODO!!!!!
 *   1. Guardar os tempos dos trains locais
 *   2. Guardar os availableSeats locais (num documentos), remover no booking
 *   3. Ler o json e escrever da db
 *   4. Validação dos security tokens
 *   5. arranjar maneira de ligar o SendGrid ao pub/sub
 * */

@RestController
@RequestMapping("/api")
public class TrainRestController {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;
    @Resource(name= "publisher")
    private Publisher publisher;
    @Resource(name = "db")
    private Firestore db;

    public static final Map<String, List<Booking>> allBookings = new HashMap<>();
    public static final Map<String, Seat> allSeats = new HashMap<>();
    public static final Map<String, Train> allTrains = new HashMap<>();

    private final String API_KEY = Application.getApiKey();

    // For now this is hard-coded, eventually this could be passed to the database
    private final String [] trainCompanies = {"reliabletrains.com", "unreliabletrains.com"};

    /**
     * Retrieves all trains from external train companies and internal Firestore database.
     * @return Collection<Train> A collection of Train objects.
     * @throws NullPointerException if an error occurs during data retrieval.
     */
    @GetMapping(path = "/getTrains")
    public Collection<Train> getAllTrains() throws NullPointerException{

        ArrayList<Train> trains = new ArrayList<>();

        for (String i: trainCompanies) {
            try {
                Collection<Train> results = getAllTrainsFrom(i);

                trains.addAll(results);
            } catch (Exception e) {
                System.out.println("Couldn't GET trains from " + i);
                //return null;
            }
        }

        // Now, get local trains from Firestore
        CollectionReference trainsCollection = db.collection("storedTrains");

        // Create a query to retrieve trains with the specified company name
        Query query = trainsCollection.whereEqualTo("trainCompany", "InternalTrains");
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        try {
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                // Convert Firestore document to Train object
                Train train = document.toObject(Train.class);
                trains.add(train);
            }
        } catch (Exception e) {
            // Handle the exception appropriately
            e.printStackTrace();
            return null;
        }

        return trains;
    }

    /**
     * Auxiliary method to get all trains from a specific train company.
     * @param trainCompany The name of the train company.
     * @return Collection<Train> A collection of Train objects from the specified company.
     * @throws NullPointerException if an error occurs during data retrieval.
     */
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

    /**
     * Retrieves a specific train by its ID and company, either from Firestore or an external API.
     * @param trainId The ID of the train.
     * @param trainCompany The company of the train.
     * @return Train The requested train object.
     * @throws RuntimeException for various errors, including API and database access issues.
     */
    @GetMapping(path = "/getTrain")
    public Train getTrain(@RequestParam String trainId, @RequestParam String trainCompany){
        // OLD implementation
        /*Train train = allTrains.get(trainId);

        if(train == null) {
            Mono<ResponseEntity<Train>> responseEntityMono =
                    webClientBuilder
                    .baseUrl("https://" + trainCompany)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", trainId)
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve()
                    .toEntity(Train.class);
            try{
                ResponseEntity<Train> response = responseEntityMono.block();
                HttpStatusCode statusCode = response.getStatusCode();

                if(statusCode.is2xxSuccessful()) {
                    train = response.getBody();
                    allTrains.put(trainId, train);
                }
            }catch (Exception e){
                return null;
            }
        }

        return train;*/

        // Check if the train is already stored in Firestore database
        DocumentReference trainRef = db.collection("storedTrains").document(trainId);

        //System.out.println(trainId);

        try {
            DocumentSnapshot document = trainRef.get().get();

            if (document.exists()) {
                // If the train is found in Firestore, return it
                return document.toObject(Train.class);
            } else {
                // If the train is not found in Firestore, make the HTTP request to the Train Company
                try {
                    Train train = webClientBuilder
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

                    // Store the train in Firestore
                    Map<String, Object> trainData = train.toMap();  // Use the toMap method

                    ApiFuture<WriteResult> storeTrainFuture = trainRef.set(trainData);

                    storeTrainFuture.get(); // Wait until operation is completed.

                    return train;
                } catch (WebClientResponseException e) {
                    // Handle WebClientResponseException appropriately (e.g., log the error, throw an exception)
                    throw e;
                } catch (Exception e) {
                    // Handle other exceptions appropriately
                    throw new RuntimeException(e);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the schedule times for a specific train.
     * @param trainCompany The company of the train.
     * @param trainId The ID of the train.
     * @return Collection<String> A collection of times for the train.
     * @throws NullPointerException if an error occurs during data retrieval.
     */
    @GetMapping(path = "/getTrainTimes")
    public Collection<String> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId) throws NullPointerException{

        // Check if the Train times are already stored in Firestore database
        Query query = db.collection("storedSeatTimes")
                .whereEqualTo("trainCompany", trainCompany).whereEqualTo("trainId", trainId);

        //System.out.println(trainCompany);

        try {
            QuerySnapshot querySnapshot = query.get().get();

            if (!querySnapshot.isEmpty()) {
                List<String> trainTimes = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    // Assuming "time" is the field where train times are stored
                    trainTimes.add((String) document.get("time"));
                }
                return trainTimes;
            } else {
                Mono<ResponseEntity<CollectionModel<String>>> responseEntityMono =
                        webClientBuilder
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
                                .toEntity(new ParameterizedTypeReference<CollectionModel<String>>(){});

                try{
                    ResponseEntity<CollectionModel<String>> response = responseEntityMono.block();
                    HttpStatusCode statusCode = response.getStatusCode();
                    if(statusCode.is2xxSuccessful())
                        return response.getBody().getContent();
                    return null;
                }catch (Exception e){
                    return null;
                }
            }
        } catch (Exception e) {
            // Handle exceptions appropriately
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves available seats for a specific train at a given time.
     * @param trainCompany The company of the train.
     * @param trainId The ID of the train.
     * @param time The time for which to retrieve seats.
     * @return Map<String, Collection<Seat>> A map categorizing seats into classes.
     * @throws Exception for various exceptions including API and database access issues.
     */
    @GetMapping(path = "/getAvailableSeats")
    public Map<String, Collection<Seat>> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time)  {

        try {
            if (trainCompany.equals("InternalTrains")) {
                Query query = db.collection("storedSeats")
                        .whereEqualTo("trainCompany", trainCompany)
                        .whereEqualTo("trainId", trainId)
                        .whereEqualTo("time", time)
                        .whereEqualTo("available", true);

                ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                QuerySnapshot querySnapshot = querySnapshotFuture.get();

                //System.out.println(querySnapshot.isEmpty());

                // Process and return seats found in Firestore
                Collection<Seat> seats = new ArrayList<>();

                for (QueryDocumentSnapshot document : querySnapshot) {
                    Seat seat = new Seat(document.toObject(SeatDBWrapper.class));
                    //System.out.println(seat.getTime());
                    seats.add(seat);
                }
                // Group seats by class
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
            } else {
                // Seats not found in Firestore, make an external API call
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
                Collection<Seat> seats = null;
                try {
                    ResponseEntity<CollectionModel<Seat>> response = responseEntityMono.block();
                    HttpStatusCode statusCode = response.getStatusCode();
                    if (statusCode.is2xxSuccessful())
                        seats = response.getBody().getContent();

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
                }catch (Exception e){
                    return null;
                }
            }
        }
        catch (Exception e) {
            // Handle exceptions appropriately
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a specific seat from Firestore or an external API.
     * @param trainId The ID of the train.
     * @param trainCompany The company of the train.
     * @param seatId The ID of the seat.
     * @return Seat The requested seat object.
     * @throws RuntimeException for various errors, including API and database access issues.
     */
    @GetMapping(path = "/getSeat")
    public Seat getSeat(@RequestParam String trainId, @RequestParam String trainCompany, @RequestParam String seatId){

        // Check if the seat is already stored in Firestore database
        DocumentReference seatRef = db.collection("storedSeats").document(seatId);

        ApiFuture<DocumentSnapshot> checkSeatFuture = seatRef.get();

        try {
            DocumentSnapshot document = checkSeatFuture.get();

            if (document.exists()) {
                // If the seat is found in Firestore, return it
                SeatDBWrapper wrappedSeat = document.toObject(SeatDBWrapper.class);
                return new Seat(wrappedSeat);
            } else {
                // If the seat is not found in Firestore, make the HTTP request to the Train Company
                try {
                    Seat seat = webClientBuilder
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

                    // Store the seat in Firestore
                    Map<String, Object> seatData = (new SeatDBWrapper(seat, true)).toMap();  // Use the toMap method

                    ApiFuture<WriteResult> storeSeatFuture = seatRef.set(seatData);

                    storeSeatFuture.get(); // Wait until operation is completed.

                    return seat;
                } catch (WebClientResponseException e) {
                    // Handle WebClientResponseException appropriately (e.g., log the error, throw an exception)
                    throw e;
                } catch (Exception e) {
                    // Handle other exceptions appropriately
                    throw new RuntimeException(e);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Confirms quotes for a customer and sends data to a publisher.
     * @param quotes A collection of quotes to be confirmed.
     * @return ResponseEntity<?> A response entity indicating the outcome.
     * @throws ExecutionException, InterruptedException for asynchronous operation failures.
     */
    @PostMapping(path = "/confirmQuotes")
    public ResponseEntity<?> confirmQuotes(@RequestBody Collection<Quote> quotes) throws ExecutionException, InterruptedException {
        String customer = SecurityFilter.getUser().getEmail();

        ObjectMapper objectMapper = new ObjectMapper();
        try{
            // Serialize quote array as json and send it to publisher
            byte[] quoteJson = objectMapper.writeValueAsBytes(quotes);

            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFrom(quoteJson))
                    .putAttributes("user", customer)
                    .build();

            publisher.publish(message);

            return ResponseEntity.ok().build();
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all bookings for the current customer.
     * @return Collection<Booking> A collection of booking objects.
     * @throws RuntimeException for database access issues.
     */
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

    /**
     * Retrieves all bookings for all customers, accessible only by users with "manager" role.
     * @return Collection<Booking> A collection of all booking objects.
     * @throws CompletionException for parallel processing issues.
     */
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

    /**
     * Identifies the best customers based on the number of tickets purchased, accessible only by "manager" role.
     * @return Collection<String> A collection of the best customers' identifiers.
     * @throws CompletionException for asynchronous operation failures.
     */
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