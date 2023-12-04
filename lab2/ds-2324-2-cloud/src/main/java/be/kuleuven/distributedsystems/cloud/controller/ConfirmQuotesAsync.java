package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
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
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.memory.UserAttribute;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/push")
public class ConfirmQuotesAsync {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;
    @Resource(name = "db")
    private Firestore db;

    private final int MAX_TRIES = 25; // coded hard as fuck

    private final String API_KEY = Application.getApiKey();

    @PostMapping("/confirmQuoteSub")
    public ResponseEntity<?> processQuotes(@RequestBody String payload){
        // Process the pubsub message and get the payload

        Collection<Quote> quotes; String customer;
        try{
            ObjectMapper mapper = new ObjectMapper();
            PubsubContainer pubsub = mapper.readValue(payload, PubsubContainer.class);

            byte[] data = Base64.decode(pubsub.getMessage().getData());
            quotes = mapper.readValue(data, new TypeReference<Collection<Quote>>(){});
            customer = pubsub.getMessage().getAttributes().get("user");
        }
        catch (Exception e){
            System.out.println("[ConfirmQuotes]: Error reading/decoding pubsubmessage!");

            return ResponseEntity.status(500).build();
        }

        // Generate booking reference
        UUID bookingReference = UUID.randomUUID();

        ArrayList<Ticket> tickets = new ArrayList<>();

        // First try to read all tickets from external provider, we skip all internal trains
        for(Quote quote: quotes){
            if(quote.getTrainCompany().equals("InternalTrains")) continue;
            Ticket ticket;

            // Try to get the ticket!
            Mono<ResponseEntity<Ticket>> responseMono = webClientBuilder
                    .baseUrl("https://" + quote.getTrainCompany())
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", quote.getTrainId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("key", API_KEY)
                            .queryParam("customer", customer)
                            .queryParam("bookingReference", bookingReference)
                            .build())
                    .retrieve()
                    .toEntity(Ticket.class);

            HttpStatusCode httpStatusCode;
            try {
                ResponseEntity<Ticket> response = responseMono.block();
                //assert response != null;

                httpStatusCode = response.getStatusCode();
                if (httpStatusCode.is2xxSuccessful()) {
                    ticket = response.getBody();
                    tickets.add(ticket);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("[Confirm Quotes] Error occured while getting external tickets! Removing!");
                removeExternal(tickets);

                // SEND THE BOOKING UNSUCCESSFUL EMAIL HERE!
                return ResponseEntity.ok().build();
            }
        }

        // Then create all the tickets for the local trains within the firestore database
        for(Quote quote: quotes){
            Ticket ticket;
            if(!quote.getTrainCompany().equals("InternalTrains")) continue;
            //DocumentReference ticketRef = db.collection("takenSeats").document();
            ticket = new Ticket("InternalTrains",
                    quote.getTrainId(),
                    quote.getSeatId(),
                    UUID.randomUUID(),
                    customer,
                    bookingReference.toString());
            try{
                //DocumentReference takenRef = db.collection("takenSeats").document(quote.getSeatId().toString());
                DocumentReference seatRef = db.collection("storedSeats").document(quote.getSeatId().toString());

                /* Check if unavailable
                * i srly tried to make this with only the transaction part
                * mas fdss caputa de merda isto ser tudo assincrono não para fazer nada de jeito*/
                if(!seatRef.get().get().get("available", boolean.class))
                    throw new RuntimeException();

                db.runTransaction(transaction -> {
                    DocumentSnapshot seatSnap = transaction.get(seatRef).get();
                    transaction.update(seatRef, "available", false);
                    return null;
                });
            }
            catch(Exception e){
                System.out.println("[Confirm Quotes] Error occured while getting internal tickets! Removing!");
                removeExternal(tickets);
                removeInternal(tickets);

                // SEND THE BOOKING UNSUCCESSFUL EMAIL HERE!
                return ResponseEntity.ok().build();
            }

            tickets.add(ticket);
        }

        // Now place the booking!
        DocumentReference docRef = db.collection("allbookings")
                .document(customer)
                .collection("bookings")
                .document(bookingReference.toString());

        // Map of the tickets
        List<Map<String, Object>> ticketMaps = new ArrayList<>();
        for (Ticket ticket : tickets) ticketMaps.add(ticket.toMap());

        // Create the booking
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", bookingReference.toString());
        bookingData.put("time", LocalDateTime.now().toString());
        bookingData.put("tickets", ticketMaps);
        bookingData.put("customer", customer);

        ApiFuture<WriteResult> future = docRef.set(bookingData);
        try{
            future.get();
            DocumentReference customerDocRef = db.collection("allbookings").document(customer);

            // Check if the document exists
            ApiFuture<DocumentSnapshot> customerFuture = customerDocRef.get();
            DocumentSnapshot customerDocument = customerFuture.get();

            if (customerDocument.exists()) {
                int totalTickets = customerDocument.getLong("totalTickets").intValue();
                // Increment the totalTickets value by the number of tickets bought in the current transaction
                totalTickets += tickets.size();
                customerDocRef.update("totalTickets", totalTickets);
            }
            else {
                customerDocRef.set(Map.of("totalTickets", tickets.size()));
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }

        // Great success!
        // Send an email confirming the

        //EmailService.sendBookingConfirmation(customer, true);

        return ResponseEntity.ok().build();
    }

    /*These could be remote workers whose job is just to remove tickets
    * but idk if calling web workers from other webworkers should be done
    * or not. also they'll be making a ton of async requests*/
    public void removeExternal(Collection<Ticket> tickets){
        String customer;

        for(Ticket ticket : tickets) {
            if(ticket.getTrainCompany().equals("InternalTrains")) continue;
            // Dummy 4xx code, not 404
            // 404 is returned when trying to delete an already non-existent ticket
            HttpStatusCode httpStatusCode = HttpStatus.BAD_REQUEST;
            try {
                int i;
                for (i = 0; i < MAX_TRIES && (!httpStatusCode.is2xxSuccessful() || httpStatusCode != HttpStatus.NOT_FOUND); i++) {
                    Mono<ResponseEntity<String>> responseEntity = webClientBuilder.baseUrl("https://" + ticket.getTrainCompany())
                            .build()
                            .delete()
                            .uri(uriBuilder -> uriBuilder
                                    .pathSegment("trains", ticket.getTrainId().toString(),
                                            "seats", ticket.getSeatId().toString(), "ticket", ticket.getTicketId().toString())
                                    .queryParam("key", API_KEY)
                                    .build())
                            .retrieve()
                            .toEntity(String.class);

                    httpStatusCode = responseEntity.block().getStatusCode();
                }
            }
            catch (Exception ee){
                continue;
            }
        }
    }

    public void removeInternal(Collection<Ticket> tickets){

        for(Ticket ticket: tickets){
            if(!ticket.getTrainCompany().equals("InternalTrains")) continue;

            DocumentReference takenRef = db.collection("takenSeats").document(ticket.getSeatId().toString());
            DocumentReference seatRef = db.collection("storedSeats").document(ticket.getSeatId().toString());
            db.runTransaction(transaction -> {
                DocumentSnapshot takenSnap = transaction.get(takenRef).get();
                DocumentSnapshot seatSnap = transaction.get(seatRef).get();
                if(takenSnap.exists())
                    transaction.delete(takenRef);
                transaction.update(seatRef, "available", true);
                return null;
            });
        }

    }
}




