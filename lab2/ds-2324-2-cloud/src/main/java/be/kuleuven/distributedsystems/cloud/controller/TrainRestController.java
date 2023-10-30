package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.entities.*;
import org.bouncycastle.util.StringList;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

@RestController
@RequestMapping("/api")
public class TrainRestController {

    private final String API_KEY = Application.getApiKey();

    // Pass this to database, URLs without https://
    private final String [] trainCompanies = {"reliabletrains.com", "unreliabletrains.com"};

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

    @GetMapping(path = "/getTrains")
    public Collection<Train> getAllTrains() throws NullPointerException{

        ArrayList<Train> trains = new ArrayList<Train>();

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

    /*@GetMapping(path = "/getSeat")
    public Seat getSeat(@RequestParam String trainId, @RequestParam String trainCompany, @RequestParam String seatId) throws NullPointerException{
        return webClientBuilder
                .baseUrl("https://" + trainCompany)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder.pathSegment("train", trainId)
                        .pathSegment("seats").queryParam("key", API_KEY))
        )
    }*/

    // TODO
    @GetMapping(path = "/getAvailableSeats")
    public Collection<Collection<Seat>> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time) throws NullPointerException{

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

        // HARD CODED, get all seats from second and first class in separate arrays
        ArrayList<Seat> secondClass = new ArrayList<>();
        for(Seat seat : seats)
            if(seat.getType().equals("2nd class"))
                secondClass.add(seat);

        ArrayList<Seat> firstClass = new ArrayList<>();
        for(Seat seat : seats)
            if(seat.getType().equals("1st class"))
                firstClass.add(seat);

        firstClass.sort(Seat.seatComparator);
        secondClass.sort(Seat.seatComparator);

        Collection<Collection<Seat>> allSeatsGrouped = new ArrayList<Collection<Seat>>();
        allSeatsGrouped.add(secondClass);
        allSeatsGrouped.add(firstClass);

        return allSeatsGrouped;
    }
}