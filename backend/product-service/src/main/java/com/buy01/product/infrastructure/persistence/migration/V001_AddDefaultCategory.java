package com.buy01.product.infrastructure.persistence.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

//@ChangeUnit(id = "product-add-default-category", order = "001", author = "rachid")
public class V001_AddDefaultCategory {

//    @Execution
//    public void execute(MongoTemplate mongo) {
//        mongo.updateMulti(
//                Query.query(Criteria.where("category").exists(false)),
//                Update.update("category", "OTHER"),
//                "products");   // use your real collection name
//    }
//
//    @RollbackExecution
//    public void rollback(MongoTemplate mongo) {
//        mongo.updateMulti(
//                Query.query(Criteria.where("category").is("OTHER")),
//                new Update().unset("category"),
//                "products");
//    }
}