package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import org.bouncycastle.util.StringList;
import org.checkerframework.checker.units.qual.A;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.awt.desktop.SystemEventListener;
import java.awt.print.Book;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private Map<String, List<Booking>> allBookings = new HashMap<>();

    private final String API_KEY = Application.getApiKey();

    // Pass this to database perhaps, URLs without https://
    private final String [] trainCompanies = {"reliabletrains.com", "unreliabletrains.com"};

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

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
    public Map<String, Collection<Seat>> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time) throws NullPointerException{
        Collection<Seat> seats = webClientBuilder
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
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>(){})
                .block()
                .getContent();

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
        return webClientBuilder
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
    }

    // TODO: Apply PUB/SUB to this
    @PostMapping(path = "/confirmQuotes")
    public ResponseEntity<?> confirmQuotes(@RequestBody Collection<Quote> quotes){
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

        Booking booking = new Booking(bookingReference, LocalDateTime.now(), tickets, customer);

        // For now just the local implementation of this
        // Check if the customer is already in the HashMap
        if(!allBookings.containsKey(customer)){
            ArrayList<Booking> userBookings = new ArrayList<>();
            userBookings.add(booking);
            allBookings.put(customer, userBookings);
        }
        else{
            allBookings.get(customer).add(booking);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/getBookings")
    public Collection<Booking> getBookings(){
        String customer = SecurityFilter.getUser().getEmail();

        //System.out.println("get bookings called!!!!");

        List<Booking> bookings = allBookings.get(customer);
        if(bookings == null) return null; // why assert no work

        return bookings;
    }

    @GetMapping(path = "/getAllBookings")
    public Collection<Booking> getAllBookings(){
        List<String> roles = List.of(SecurityFilter.getUser().getRoles());
        if(!roles.contains("manager")) return null;

        ArrayList<Booking> bookings = new ArrayList<>();
        for (List<Booking> bookingList : allBookings.values())
            bookings.addAll(bookingList);

        return bookings;
    }

    // This function seems kinda inefficient
    @GetMapping(path = "/getBestCustomers")
    public Collection<String> getBestCustomer(){
        List<String> roles = List.of(SecurityFilter.getUser().getRoles());
        if(!roles.contains("manager")) return null;

        ArrayList<String> users = new ArrayList<>();
        int maxTickets = 0; int tickets;
        for(String user : allBookings.keySet()){
            tickets = 0;
            for(Booking booking : allBookings.get(user))
                tickets += booking.getTickets().size();

            if (tickets > maxTickets){
                users = new ArrayList<>();
                users.add(user);
                maxTickets = tickets;
            } else if (tickets == maxTickets) {
                users.add(user);
            }
        }

        return users;
    }
}