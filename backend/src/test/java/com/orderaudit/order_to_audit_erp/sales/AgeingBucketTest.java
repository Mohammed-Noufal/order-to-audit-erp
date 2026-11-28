package com.orderaudit.order_to_audit_erp.sales;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgeingBucketTest {

    @Test
    void firstThirtyDaysAreTheCurrentBucket() {
        assertThat(AgeingBucket.forDays(0)).isEqualTo(AgeingBucket.DAYS_0_30);
        assertThat(AgeingBucket.forDays(30)).isEqualTo(AgeingBucket.DAYS_0_30);
    }

    @Test
    void dayThirtyOneStartsTheSecondBucket() {
        assertThat(AgeingBucket.forDays(31)).isEqualTo(AgeingBucket.DAYS_31_60);
        assertThat(AgeingBucket.forDays(60)).isEqualTo(AgeingBucket.DAYS_31_60);
    }

    @Test
    void daySixtyOneStartsTheThirdBucket() {
        assertThat(AgeingBucket.forDays(61)).isEqualTo(AgeingBucket.DAYS_61_90);
        assertThat(AgeingBucket.forDays(90)).isEqualTo(AgeingBucket.DAYS_61_90);
    }

    @Test
    void moreThanNinetyDaysIsTheOldestBucket() {
        assertThat(AgeingBucket.forDays(91)).isEqualTo(AgeingBucket.DAYS_90_PLUS);
        assertThat(AgeingBucket.forDays(400)).isEqualTo(AgeingBucket.DAYS_90_PLUS);
    }

    @Test
    void labelsMatchTheReportColumns() {
        assertThat(AgeingBucket.DAYS_0_30.label()).isEqualTo("0-30");
        assertThat(AgeingBucket.DAYS_31_60.label()).isEqualTo("31-60");
        assertThat(AgeingBucket.DAYS_61_90.label()).isEqualTo("61-90");
        assertThat(AgeingBucket.DAYS_90_PLUS.label()).isEqualTo("90+");
    }
}
