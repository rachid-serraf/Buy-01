package com.buy01.product.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

class MongoConversionConfigTest {

    private final MongoCustomConversions conversions = new MongoConversionConfig().mongoCustomConversions();

    @Test
    void storesBigDecimalAsDecimal128() {
        assertThat(conversions.getCustomWriteTarget(BigDecimal.class)).contains(Decimal128.class);
    }

    @Test
    void readsDecimal128AndLegacyStringPricesAsBigDecimal() {
        assertThat(conversions.hasCustomReadTarget(Decimal128.class, BigDecimal.class)).isTrue();
        assertThat(conversions.hasCustomReadTarget(String.class, BigDecimal.class)).isTrue();
    }
}
