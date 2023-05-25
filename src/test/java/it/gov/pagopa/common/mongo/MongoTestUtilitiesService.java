package it.gov.pagopa.common.mongo;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.Objects;

/** Utilities when performing Mongo tests */
@Service
public class MongoTestUtilitiesService {

    @Autowired(required = false)
    private MongodExecutable embeddedMongoServer;

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    /** It will return the actual mongo URL */
    public String getMongoUrl() {
        try {
            String mongoUrl;
            if (embeddedMongoServer != null) {
                Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
                mongoEmbeddedServerConfigField.setAccessible(true);
                MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
                Net mongodNet = Objects.requireNonNull(mongodConfig).net();

                mongoUrl = "mongodb://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort());
            } else {
                mongoUrl = mongodbUri.replaceFirst(":[^:]+(?=:[0-9]+)", "");
            }
            return mongoUrl;
        } catch (NoSuchFieldException | UnknownHostException e){
            throw new IllegalStateException("Something went wrong while retrieving MongoURL", e);
        }
    }
}
