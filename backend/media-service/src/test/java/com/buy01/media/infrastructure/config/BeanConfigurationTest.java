package com.buy01.media.infrastructure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoDatabase;

class BeanConfigurationTest {

    @Test
    void checkMongoPingsDatabaseWhenRunnerExecutes() throws Exception {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoDatabase database = mock(MongoDatabase.class);
        when(mongoTemplate.getDb()).thenReturn(database);

        CommandLineRunner runner = new BeanConfiguration().checkMongo(mongoTemplate);
        runner.run();

        verify(database).runCommand(new Document("ping", 1));
    }
}
