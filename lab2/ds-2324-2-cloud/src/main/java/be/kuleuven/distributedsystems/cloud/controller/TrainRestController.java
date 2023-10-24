package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.entities.Train;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api")
public class TrainRestController {

    private final String API_KEY = "JViZPgNadspVcHsMbDFrdGg0XXxyiE";

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


}
