package com.buy01.product.infrastructure.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.buy01.product.infrastructure.persistence.migration.V001_AddDefaultCategory;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class V001_AddDefaultCategoryTest {

    @Mock
    private MongoTemplate mongo;

    private final V001_AddDefaultCategory migration = new V001_AddDefaultCategory();

    @Test
    void executeAssignsOtherToProductsWithoutCategory() {
        migration.execute(mongo);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq("products"));

        assertThat(queryCaptor.getValue().getQueryObject()).containsKey("$or");
        Document update = updateCaptor.getValue().getUpdateObject();
        assertThat(update.get("$set", Document.class)).containsEntry("category", "OTHER");
    }

    @Test
    void rollbackRemovesCategoryFromMigratedProducts() {
        migration.rollback(mongo);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq("products"));

        assertThat(queryCaptor.getValue().getQueryObject()).containsEntry("category", "OTHER");
        Document update = updateCaptor.getValue().getUpdateObject();
        assertThat(update.get("$unset", Document.class)).containsKey("category");
    }
}
