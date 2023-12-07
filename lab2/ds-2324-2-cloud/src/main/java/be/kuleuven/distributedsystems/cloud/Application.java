package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.controller.ConfirmQuotesAsync;
import be.kuleuven.distributedsystems.cloud.controller.TrainRestController;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.*;
import com.google.protobuf.Api;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
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
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {

    /**
     * The main method to start the Spring application.
     * Sets the server port and initializes the application context.
     * It also loads data into Firestore from a JSON resource.
     * @param args The command line arguments.
     * @throws IOException if an error occurs during reading the data file.
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));

        ApplicationContext context = SpringApplication.run(Application.class, args);

        // TODO: (level 2) load this data into Firestore
        String data = new String(new ClassPathResource("data.json").getInputStream().readAllBytes());

        loadLocalDBData(data);
    }

    /**
     * Loads local database data from a JSON string into Firestore.
     * Parses the JSON data to create LocalTrain objects, then converts these to Train objects.
     * For each LocalTrain, it stores the train, its seats, and times in Firestore.
     * The method also counts the total number of stored seats and times, storing these counts in Firestore.
     * @param data The JSON string representing the local train data.
     * @throws RuntimeException for various errors including IOException, database access issues, and data processing errors.
     */
    public static void loadLocalDBData(String data){
        Firestore db = db();

        // Convert JSON data to LocalTrain Object
        ObjectMapper mapper = new ObjectMapper();
        LocalTrainsWrapper trainsWrapper = null;
        try {
            trainsWrapper = mapper.readValue(data, LocalTrainsWrapper.class);
            //System.out.println("Java object after parsing JSON using Jackson");
            System.out.println(trainsWrapper);
            System.out.println(trainsWrapper.getTrains());

        } catch (IOException e) {
            e.printStackTrace();
        }

        int totalStoredSeats = 0;
        int totalStoredTimes = 0;

        for (LocalTrain localTrain : trainsWrapper.getTrains()) {

            //System.out.println("Train Name:" + localTrain.getName());

            // Convert LocalTrain Object to Train Object
            Train train = new Train(localTrain);
            //System.out.println("Train company:" + train.getTrainCompany());

            // Check if the train is already stored in Firestore database
            Query query = db.collection("storedTrains")
                    .whereEqualTo("name", train.getName()); // Use whereEqualTo for the specific field

            try {
                QuerySnapshot querySnapshot = query.get().get();

                if (!querySnapshot.isEmpty()) {
                    // If the train is found in Firestore, no need to initialize data!
                    System.out.println("Train data has already been initialized! No need to do it again!");
                } else {
                    // If the train is not found, then put the train and respective data on the database
                    System.out.println("No train data recorded. Initializing Local Trains data on Firestore!");
                    Map<String, Object> trainData = train.toMap();  // Use the toMap method

                    ApiFuture<WriteResult> storeTrainFuture = db.collection("storedTrains").document(train.getTrainId().toString()).set(trainData);

                    // Wait until the operation is completed.
                    storeTrainFuture.get();

                    Set<String> storedSeatTimes = new HashSet<>(); // Keep track of stored seat times

                    // Now, after storing the train, store the respective seats and times:
                    for (LocalTrain.LocalSeat localSeat : localTrain.getSeats()) {

                        // ------------ Store Seats -------------- //

                        // Convert LocalSeat Object to Seat Object
                        SeatDBWrapper seat = new SeatDBWrapper(localSeat, train);

                        // Put the seat and respective data on the database
                        //System.out.println("Initializing Local Seats data on Firestore!");

                        Map<String, Object> seatData = seat.toMap();  // Use the toMap method

                        ApiFuture<WriteResult> storeSeatFuture = db.collection("storedSeats")
                                .document(seat.getSeatId().toString())
                                .set(seatData);

                        // Wait until the operation is completed.
                        storeSeatFuture.get();

                        // Increment the count of stored seats
                        totalStoredSeats++;

                        // ---------- Store Times ---------- //

                        String seatTime = localSeat.getTime().toString();
                        String seatIdentifier = localTrain.getName() + seatTime;

                        // Check if the seat time has already been stored for the same train
                        if (storedSeatTimes.contains(seatIdentifier)) {
                            //System.out.println("Train time has already been stored! Skipping...");
                            continue;
                        }

                        // If the seat time is not found, then put the time on the database
                        //System.out.println("Storing " + localTrain.getName() + "time in Firestore!");

                        // Store the seat time in the storedSeatTimes set
                        storedSeatTimes.add(seatIdentifier);

                        // Now, we can store the seat time in the storedTimes collection
                        Map<String, Object> seatTimeData = new HashMap<>();
                        seatTimeData.put("trainId", train.getTrainId().toString());
                        seatTimeData.put("trainCompany", train.getTrainCompany());
                        seatTimeData.put("trainName", train.getName());
                        seatTimeData.put("time", seatTime);

                        ApiFuture<WriteResult> future = db.collection("storedSeatTimes").document(seatIdentifier).set(seatTimeData);


                        // Wait until the operation is completed.
                        future.get();

                        // Increment the count of stored times
                        totalStoredTimes++;
                    }
                }
            } catch (Exception e) {
                // Handle other exceptions appropriately
                throw new RuntimeException(e);
            }
        }

        // Create a document with the total counts
        Map<String, Object> totalCounts = new HashMap<>();
        totalCounts.put("totalStoredSeats", totalStoredSeats);
        totalCounts.put("totalStoredTimes", totalStoredTimes);

        // Store the document in Firestore
        DocumentReference totalCountsRef = db.collection("totalCounts").document("counts");

        ApiFuture<WriteResult> storeTotalCountsFuture = totalCountsRef.set(totalCounts);

        // Wait until the operation is completed.
        try {
            storeTotalCountsFuture.get();
            System.out.println("Total counts document created!");
        } catch (Exception e) {
            // Handle exceptions appropriately
            e.printStackTrace();
        }
    }

    /**
     * Checks if the application is running in a production environment.
     * It determines the environment by checking the system environment variable 'GAE_ENV'.
     * @return boolean True if running in production, false otherwise.
     */
    @Bean
    public static boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    /**
     * Returns the project ID based on the environment.
     * Chooses between a production and a demo project ID.
     * @return String The project ID.
     */
    @Bean
    public static String projectId() {
        if(isProduction())
            return "distributedsystems-tmaiajpais";

        return "demo-distributed-systems-kul";
    }

    /**
     * Configures and provides a WebClient.Builder.
     * Sets up the client with specific configurations like connector and codecs.
     * @param configurer HypermediaWebClientConfigurer for hypermedia support.
     * @return WebClient.Builder A configured WebClient.Builder instance.
     */
    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    /**
     * Configures and provides an HttpFirewall instance.
     * Allows URL encoded slashes in URLs.
     * @return HttpFirewall A configured HttpFirewall instance.
     */
    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    /**
     * Configures and provides a Publisher instance for Pub/Sub messaging.
     * Sets up the publisher for a specific topic based on the environment.
     * @return Publisher The configured Publisher instance.
     * @throws IOException if an error occurs during publisher setup.
     */
    @Bean
    public Publisher publisher() throws IOException{
        TopicName topicName = TopicName.of(projectId(), "confirmQuotes");
        TransportChannelProvider channelProvider = null;
        if(!Application.isProduction()) {

            publisherChannel = ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();
            channelProvider = FixedTransportChannelProvider.create(
                    GrpcTransportChannel.create(publisherChannel));

            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
                    TopicAdminSettings.newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build())) {

                // Check if the topic already exists
                try {
                    Topic topic = topicAdminClient.getTopic(topicName);
                    System.out.println("Topic already exists!");
                } catch (ApiException e) {
                    if (e.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
                        // Topic does not exist, create it
                        topicAdminClient.createTopic(topicName);
                        System.out.println("Topic created!");
                    } else {
                        // Other ApiException, print error and stack trace
                        System.out.println("Either an error occurred or topic already exists!");
                        e.printStackTrace();
                    }
                }

                return Publisher.newBuilder(topicName)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
            }
        }

        // return the cloud one
        return Publisher.newBuilder(topicName)
                .build();
    }

    /**
     * Configures and initializes a Pub/Sub subscriber.
     * Sets up the subscriber based on the environment, with specific push configurations.
     * @throws IOException if an error occurs during subscriber setup.
     */
    @Bean
    public void subscriber() throws IOException{

        SubscriptionAdminSettings subscriptionAdminSettings;

        PushConfig pushConfig;
        if(!Application.isProduction()) { // Running in firebase emulator suite!

            subscriberChannel = ManagedChannelBuilder.forTarget("localhost:8083")
                    .usePlaintext().build();

            TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
                    GrpcTransportChannel.create(subscriberChannel));

            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .setTransportChannelProvider(channelProvider)
                    .build();

            pushConfig = PushConfig.newBuilder()
                    .setPushEndpoint("http://localhost:8080/push/confirmQuoteSub")
                    //.setNoWrapper(noWrapper)
                    .build();

            SubscriptionName subscriptionName = SubscriptionName.of(projectId(), "confirmQuotesSubscription");

            // Create subscription
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
                Subscription subscription = subscriptionAdminClient.getSubscription(subscriptionName);
                subscriptionAdminClient.deleteSubscription(subscriptionName);
                throw new Exception();
            }
            catch (Exception e){
                // Let's try that again but this time good - D.Lynch
                try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
                    Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(projectId(), "confirmQuotes"), pushConfig, 60);
                    System.out.println("Subscription created!");
                }
                catch (Exception e_){
                    e_.printStackTrace();
                    System.out.println("error creating subscriber");
                }
            }
        }
        else { // in the cloud you clown!
            subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder().build();
            pushConfig = PushConfig.newBuilder()
                    .setPushEndpoint("https://distributedsystems-tmaiajpais.ew.r.appspot.com/push/confirmQuoteSub")
                    .build();
        }
    }

    /**
     * Cleanup method called before the application is destroyed.
     * Shuts down the ManagedChannels used by the publisher and subscriber.
     */
    @PreDestroy
    public void preDestroy() {
        assert publisherChannel != null;
        assert subscriberChannel != null;
        // shutdown your channels here
        shutdownManagedChannel(publisherChannel);
        shutdownManagedChannel(subscriberChannel);
    }

    private ManagedChannel publisherChannel = null;
    private ManagedChannel subscriberChannel = null;

    private void shutdownManagedChannel(ManagedChannel channel) {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Channel did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Provides a Firestore instance.
     * Configures Firestore based on whether the application is in production or not.
     * @return Firestore The Firestore service instance.
     */
    @Bean
    public static Firestore db(){
        if(!Application.isProduction())
            return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId())
                .setCredentials(new FirestoreOptions.EmulatorCredentials())
                .setEmulatorHost("localhost:8084")
                .build().getService();

        return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId())
                .build().getService();
    }

    /**
     * Retrieves the external API key from a resource file.
     * Reads the API key from the 'API_KEY' resource file.
     * @return String The API key, or null if an error occurs.
     */
    public static String getApiKey() { // Hidden external API key!
        try {
            InputStream is = new ClassPathResource("API_KEY").getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));

            //String key = bf.readLine();
            //System.out.println("API_KEY = " + key);
            return bf.readLine(); // return key;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the Sendgrid API key.
     * Currently, the method returns a placeholder string.
     * @return String The Sendgrid API key.
     */
    public static String getSendgridApiKey(){
        return "API_KEY";
    }
}
