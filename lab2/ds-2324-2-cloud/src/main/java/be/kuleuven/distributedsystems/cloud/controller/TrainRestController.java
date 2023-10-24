package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import org.bouncycastle.util.StringList;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api")
public class TrainRestController {

    private final String API_KEY = Application.getApiKey();

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

    @GetMapping(path = "/getTrains")
    public Collection<Train> getAllTrains() throws NullPointerException{
        return webClientBuilder
                .baseUrl("https://reliabletrains.com")
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
        Collection<Train> trains = getAllTrains();

        for(Train train: trains) {
            if (train.getTrainId().toString().equals(trainId))
                return train;
        }

        return null;
    }

    @GetMapping(path = "/getTrainTimes")
    public Collection<String> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId) throws NullPointerException{

        return webClientBuilder
                .baseUrl("https://reliabletrains.com")
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

}
