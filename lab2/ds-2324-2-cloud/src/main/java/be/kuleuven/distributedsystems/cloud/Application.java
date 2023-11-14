package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.controller.ConfirmQuotesAsync;
import be.kuleuven.distributedsystems.cloud.controller.TrainRestController;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));

        ApplicationContext context = SpringApplication.run(Application.class, args);

        // TODO: (level 2) load this data into Firestore
        String data = new String(new ClassPathResource("data.json").getInputStream().readAllBytes());
    }

    @Bean
    public boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public static String projectId() {
        return "demo-distributed-systems-kul";
    }

    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public Publisher publisher() throws IOException{
        TopicName topicName = TopicName.of(projectId(), "confirmQuotes");

        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                        ManagedChannelBuilder.forTarget("localhost:8083")
                                .usePlaintext().build()));

        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        // check if the topic already exists
        // and create it if not
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                        .setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build())) {
            topicAdminClient.createTopic(topicName);
        }catch (Exception e){
            System.out.println("Topic already exists!");
        }

        return Publisher.newBuilder(topicName)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public void subscriber() throws IOException{
        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                        ManagedChannelBuilder.forTarget("localhost:8083")
                                .usePlaintext().build()));

        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .setTransportChannelProvider(channelProvider)
                .build();

        Subscription subscription = null;

        SubscriptionName subscriptionName = SubscriptionName.of(projectId(), "confirmQuotesSubscription");

        // Make the pubsub metadata part of the HTTP header,
        // instead of sending it in the body
        //PushConfig.NoWrapper noWrapper = PushConfig.NoWrapper.newBuilder().setWriteMetadata(true).build();

        PushConfig pushConfig = PushConfig.newBuilder()
                .setPushEndpoint("http://localhost:8080/push/confirmQuoteSub")
                //.setNoWrapper(noWrapper)
                .build();

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            subscription = subscriptionAdminClient.getSubscription(subscriptionName);
            subscriptionAdminClient.deleteSubscription(subscriptionName);
            throw new Exception();
        }catch (Exception e){ // Let's try that again but this time good - D.Lynch
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
                subscription = subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(projectId(), "confirmQuotes"), pushConfig, 60);
                System.out.println("Subscription created!");
            }
        }

    }

    @Bean
    public Firestore db(){
        return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId())
                .setCredentials(new FirestoreOptions.EmulatorCredentials())
                .setEmulatorHost("localhost:8084")
                .build().getService();
    }

    public static String getApiKey() { // Hidden external API key!
        try {
            File file = new ClassPathResource("API_KEY").getFile();

            String key = new String(Files.readAllBytes(file.toPath()));
            //System.out.println("API_KEY = " + key);
            return key;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
}
