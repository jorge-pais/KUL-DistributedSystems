package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.PubsubContainer;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.ByteArray;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/push")
public class ConfirmQuotesAsync {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;
    @Resource(name = "db")
    private Firestore db;

    private final String API_KEY = Application.getApiKey();

    // TODO ASK ABOUT THIS
    @PostMapping("/confirmQuoteSub")
    public ResponseEntity<?> processQuotes(@RequestBody String payload) {
        try {
            //System.out.println("Received payload: " + payload);
            // Process the payload here
            ObjectMapper mapper = new ObjectMapper();
            PubsubContainer pubsub = mapper.readValue(payload, PubsubContainer.class);

            byte[] data = Base64.decode(pubsub.getMessage().getData());
            String customer = pubsub.getMessage().getAttributes().get("user");

            ObjectMapper objectMapper = new ObjectMapper();
            Collection<Quote> quotes = objectMapper.readValue(data, new TypeReference<Collection<Quote>>(){});

            UUID bookingReference = UUID.randomUUID();

            List<Ticket> tickets = new ArrayList<>();
            List<Map<String, Object>> ticketMaps = new ArrayList<>();

            /// THESE ARE THE REQUESTS THAT ARE MADE TO THE TRAIN SERVERS
            for(Quote quote: quotes){
                Ticket ticket = null;

                try {
                    if (quote.getTrainCompany().equals("InternalTrains")) {
                        ticket = new Ticket("InternalTrains", quote.getTrainId(), quote.getSeatId(), UUID.randomUUID(), customer, bookingReference.toString());

                        // Check if the seat/ticket is already taken
                        DocumentReference docRef= db.collection("takenSeats").document(quote.getSeatId().toString());
                        Ticket finalTicket = ticket;
                        db.runTransaction(transaction -> {
                            DocumentSnapshot snapshot = transaction.get(docRef).get();
                            if(snapshot.exists())
                                throw new Exception();
                            transaction.create(docRef, finalTicket.toMap());
                            return null;
                        });

                        tickets.add(ticket);
                        ticketMaps.add(ticket.toMap());
                    } else {
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

                        ResponseEntity<Ticket> response = responseMono.block();

                        HttpStatusCode httpStatus = response.getStatusCode();
                        if (httpStatus.is2xxSuccessful()) {
                            ticket = response.getBody();

                            tickets.add(ticket);
                            ticketMaps.add(ticket.toMap());
                        } else {
                            throw new Exception();
                        }
                    }
                }
                // Couldn't PUT all tickets now attempt to remove all tickets
                catch (Exception e) {
                    System.out.println("Some error while getting the tickets");
                    for (Ticket ticket_ : tickets) { // Should probably do a while loop that checks for a successful remove
                        HttpStatusCode httpStatus = HttpStatus.NOT_FOUND;

                        if(ticket_.getTrainCompany().equals("InternalTrains")){
                            DocumentReference docRef= db.collection("takenSeats").document(ticket_.getSeatId().toString());
                            db.runTransaction(transaction -> {
                                DocumentSnapshot snapshot = transaction.get(docRef).get();
                                if(snapshot.exists())
                                    transaction.delete(docRef);
                                return null;
                            });

                            continue;
                        }

                        // this is in case unreliabletrains is not responding to a remove operation,
                        // in which case we definitely want to spam the remove requests.
                        for (int i = 0; i < 25 && !httpStatus.is2xxSuccessful(); i++) {
                            try {
                                Mono<ResponseEntity<String>> responseEntity = webClientBuilder.baseUrl("https://" + ticket_.getTrainCompany())
                                        .build()
                                        .delete()
                                        .uri(uriBuilder -> uriBuilder
                                                .pathSegment("trains", ticket_.getTrainId().toString(),
                                                        "seats", ticket_.getSeatId().toString(), "ticket", ticket_.getTicketId().toString())
                                                .queryParam("key", API_KEY)
                                                .build())
                                        .retrieve()
                                        .toEntity(String.class); // This should return 204

                                httpStatus = responseEntity.block().getStatusCode();
                            } catch (Exception ee) {
                                httpStatus = HttpStatus.NOT_FOUND;
                            }
                        }
                    }
                    return ResponseEntity.status(500).build();
                }
            }

            //// We got the tickets, now put them on the database
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
            } catch (Exception e) {
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
            }
            else {
                // If the document doesn't exist, create a new one with totalTickets set to the number of tickets bought
                customerDocRef.set(Map.of("totalTickets", tickets.size()));
            }

        } catch (Exception e_) {
            // In case of an error, log it and return an appropriate response
            System.err.println("Error processing the message: " + e_.getMessage());
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }
}


