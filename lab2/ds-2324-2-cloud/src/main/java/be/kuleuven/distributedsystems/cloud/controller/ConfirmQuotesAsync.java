package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ConfirmQuotesAsync implements MessageReceiver {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

    private final String API_KEY = Application.getApiKey();

    public ResponseEntity<?> confirmQuotes(Collection<Quote> quotes){
        String customer = SecurityFilter.getUser().getEmail();
        UUID bookingReference = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();

        for(Quote quote: quotes){
            webClientBuilder // PUT request
                    .baseUrl("https://" + quote.getTrainCompany())
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", quote.getTrainId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("key", API_KEY)
                            .queryParam("customer", customer)
                            .queryParam("bookingReference", bookingReference.toString())
                            .build())
                    .retrieve().bodyToMono(String.class).block();

            Ticket ticket = webClientBuilder  // GET request
                    .baseUrl("https://" + quote.getTrainCompany())
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("trains", quote.getTrainId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("key", API_KEY)
                            .build())
                    .retrieve().bodyToMono(Ticket.class).block();

            tickets.add(ticket);
        }

        // Create the booking
        Booking booking = new Booking(bookingReference, LocalDateTime.now(), tickets, customer);

        // For now just the local implementation of this
        // Check if the customer is already in the HashMap
        if(!TrainRestController.allBookings.containsKey(customer)){
            ArrayList<Booking> userBookings = new ArrayList<>();
            userBookings.add(booking);
            TrainRestController.allBookings.put(customer, userBookings);
        }
        else{
            TrainRestController.allBookings.get(customer).add(booking);
        }

        return ResponseEntity.ok().build();
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer consumer){
        System.out.println("Received message with ID:" + pubsubMessage.getMessageId());

        try{
            ByteString data = pubsubMessage.getData();

            System.out.println("Read the following string: " + data.toString());
            
        }catch (Exception e){
            System.out.println("Error while reading message data");
        }
    }
}
