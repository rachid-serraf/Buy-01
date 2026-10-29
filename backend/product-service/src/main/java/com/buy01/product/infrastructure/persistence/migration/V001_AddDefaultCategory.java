package com.buy01.product.infrastructure.persistence.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "product-add-default-category", order = "001", author = "rachid")
public class V001_AddDefaultCategory {

    private static final String COLLECTION = "products";

    @Execution
    public void execute(MongoTemplate mongo) {
        Query missingCategory = Query.query(new Criteria().orOperator(
                Criteria.where("category").exists(false),
                Criteria.where("category").is(null)));
        mongo.updateMulti(missingCategory, Update.update("category", "OTHER"), COLLECTION);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.updateMulti(
                Query.query(Criteria.where("category").is("OTHER")),
                new Update().unset("category"),
                COLLECTION);
    }
}
