package com.buy01.order.order.stats;

import com.buy01.order.common.Status;

/**
 * Single source of truth for order analytics.
 *
 * <p>Only orders whose status matches {@link #REVENUE_STATUS} count as real
 * spending/revenue. Cancelled orders are excluded automatically because the
 * aggregation matches this exact status.</p>
 */
public final class OrderStatsConstants {

    /** Change this one value to make another status count towards stats. */
    public static final Status REVENUE_STATUS = Status.DELIVERED;

    /** Maximum number of products/categories returned in "top" lists. */
    public static final int TOP_LIMIT = 5;

    /** Number of trailing months returned by the seller revenue chart. */
    public static final int REVENUE_MONTHS = 6;

    private OrderStatsConstants() {
        // Utility class.
    }
}
