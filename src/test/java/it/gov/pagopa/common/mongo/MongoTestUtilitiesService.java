package it.gov.pagopa.common.mongo;

import com.mongodb.event.CommandStartedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import jakarta.annotation.PreDestroy;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.bson.BsonString;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilities when performing Mongo tests
 */
@Service
public class MongoTestUtilitiesService {

    @Autowired
    private MongoProperties mongoProperties;

    /**
     * It will return the actual mongo URL
     */
    public String getMongoUrl() {
        return mongoProperties.getUri().replaceFirst("(?<=//)[^@]+@", "");
    }

    //region retrieve mongo operations
    @Value
    @EqualsAndHashCode(of = "command", callSuper = false)
    public static class MongoCommand {
        String type;
        String collection;
        LocalTime firstOccurrence;
        String command;
        String sample;
    }

    protected static Queue<MongoCommand> mongoCommands;
    private static String mongoCommandsListenerDesc;
    static {
        startMongoCommandListener();
    }

    /**
     * To be called before each test in order to perform the asserts on {@link #stopAndGetMongoCommands()}
     */
    public static void startMongoCommandListener() {
        startMongoCommandListener("");
    }
    public static void startMongoCommandListener(String desc) {
        mongoCommands = new ConcurrentLinkedQueue<>();
        mongoCommandsListenerDesc = desc;
        if(mongoCommandsListenerDesc.length()>0){
            mongoCommandsListenerDesc = ": " + mongoCommandsListenerDesc;
        }
    }

    /**
     * It returned all the mongo commands sends to database server from {@link #startMongoCommandListener()} until now, stopping the listener (call again the start method if you want to continue)
     */
    public static List<Map.Entry<MongoCommand, Long>> stopAndGetMongoCommands() {
        List<Map.Entry<MongoCommand, Long>> out = mongoCommands.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .toList();
        mongoCommands = null;
        return out;
    }

    public static void stopAndPrintMongoCommands() {
        printMongoCommands(stopAndGetMongoCommands());
    }

    public static void printMongoCommands(List<Map.Entry<MongoCommand, Long>> commands) {
        if (commands != null) {
            System.out.printf("""
                                
                    ********** MONGO COMMANDS%s **********
                    %s
                    ************************************%s
                                
                    """,
                    mongoCommandsListenerDesc,
                    commands.stream()
                            .map(c -> "Times %d: %s".formatted(c.getValue(), c.getKey()))
                            .collect(Collectors.joining("\n")),
                    IntStream.range(0, mongoCommandsListenerDesc.length()).mapToObj(x->"*").collect(Collectors.joining()));
        }
    }

    @Configuration
    public static class TestMongoConfiguration {

        @Primary
        @Bean
        public MongoMetricsCommandListener mongoMetricsCommandListener(MeterRegistry registry) {
            return new MongoMetricsCommandListener(registry) {

                @PreDestroy
                void printIfNotEmpty() {
                    printMongoCommands(stopAndGetMongoCommands());
                }

                @Override
                public void commandStarted(@NonNull CommandStartedEvent event) {
                    if (mongoCommands != null) {
                        Document clone = Document.parse(event.getCommand().toJson());
                        clone.remove("lsid");

                        cleanFind(clone);
                        cleanUpsert(clone);
                        cleanFindAndModify(clone);
                        cleanInsert(clone);
                        cleanAggregate(clone);

                        mongoCommands.add(new MongoCommand(
                                event.getCommandName(),
                                event.getCommand().get(event.getCommandName()).toString(),
                                LocalTime.now(),
                                clone.toJson(),
                                event.getCommand().toJson()
                        ));
                    }
                    super.commandStarted(event);
                }

                private void cleanFind(Document clone) {
                    clearDocumentValues(clone, "filter");
                }

                private void cleanUpsert(Document clone) {
                    @SuppressWarnings("unchecked")
                    List<Document> updatesNode = (List<Document>) clone.get("updates");
                    if (updatesNode != null) {
                        updatesNode.forEach(u -> {
                            clearDocumentValues(u, "q");

                            clearDocumentValues(u, "u");
                        });
                    }
                }

                private void cleanFindAndModify(Document clone) {
                    clearDocumentValues(clone, "query");
                    if(clone.get("findAndModify") != null){
                        clearDocumentValues(clone, "update");
                    }
                }

                private void cleanInsert(Document clone) {
                    if(clone.get("insert")!=null){
                        clearDocumentValues(clone, "documents");
                    }
                }

                private void cleanAggregate(Document clone) {
                    if(clone.get("aggregate")!=null){
                        clearDocumentValues(clone, "pipeline");
                    }
                }

                public static final BsonString BSON_DUMMY_VALUE = new BsonString("VALUE");
                private void clearDocumentValues(Document document, String key) {
                    Object value = document.get(key);
                    if(value instanceof Document node){
                        node.keySet().forEach(k -> {
                            if(!(node.get(k) instanceof Document nested) || nested.keySet().stream()
                                    .filter(k2->k2.startsWith("$"))
                                    .peek(k2 -> clearDocumentValues(nested, k2))
                                    .count()==0){
                                node.put(k, BSON_DUMMY_VALUE);
                            }
                        });
                    } else if(value != null){
                        document.put(key, BSON_DUMMY_VALUE);
                    }
                }
            };
        }
    }
//endregion
}
