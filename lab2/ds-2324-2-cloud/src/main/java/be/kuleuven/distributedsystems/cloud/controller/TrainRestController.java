package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import org.bouncycastle.util.StringList;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.awt.print.Book;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class TrainRestController {

    private Map<String, List<Booking>> allBookings = new HashMap<>();

    private final String API_KEY = Application.getApiKey();

    // Pass this to database, URLs without https://
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
        return webClientBuilder
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
    public Seat getSeat(@RequestParam String trainId, @RequestParam String trainCompany, @RequestParam String seatId) throws NullPointerException{

        Collection<String> times = getTrainTimes(trainCompany, trainId);
        Collection<Seat> seats;
        for(String time : times) {
            try{
                seats = webClientBuilder
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
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {
                    })
                    .block()
                    .getContent();

                for(Seat seat : seats)
                    if (seat.getSeatId().toString().equals(seatId))
                        return seat;
            }catch (Exception e){
                System.out.println("Couldn't GET from "+ trainCompany);
            }
        }

        return null;
    }

    @PostMapping(path = "/confirmQuotes")
    public ResponseEntity<?> confirmQuotes(@RequestBody Collection<Quote> quotes){
        String customer = SecurityFilter.getUser().getEmail();
        UUID bookingReference = UUID.randomUUID();

        List<Ticket> tickets = new ArrayList<>();

        for(Quote quote: quotes){
            System.out.println(quote.getSeatId());

            // PUT request
            webClientBuilder
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

            // GET request
            Ticket ticket = webClientBuilder
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
            System.out.println("user not in database!");
            ArrayList<Booking> userBookings = new ArrayList<>();
            userBookings.add(booking);
            allBookings.put(customer, userBookings);
        }else{
            System.out.println("user in database already!");
            allBookings.get(customer).add(booking);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/getBookings")
    public Collection<Booking> getBookings(){
        String customer = SecurityFilter.getUser().getEmail();

        System.out.println("get bookings called!!!!");

        for(Booking booking : allBookings.get(customer))
            for(Ticket ticket : booking.getTickets())
                System.out.println(ticket.getCustomer() + "---" + ticket.getTicketId());

        return allBookings.get(customer);
    }

}