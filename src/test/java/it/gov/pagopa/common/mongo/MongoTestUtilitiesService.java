package it.gov.pagopa.common.mongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.stereotype.Service;

/** Utilities when performing Mongo tests */
@Service
public class MongoTestUtilitiesService {

    @Autowired
    private MongoProperties mongoProperties;

    /** It will return the actual mongo URL */
    public String getMongoUrl() {
        return mongoProperties.getUri().replaceFirst("(?<=//)[^@]+@", "");
    }
}
