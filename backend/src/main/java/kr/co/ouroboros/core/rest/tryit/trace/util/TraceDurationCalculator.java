package kr.co.ouroboros.core.rest.tryit.trace.util;

import kr.co.ouroboros.core.rest.tryit.trace.dto.TraceSpanInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Utility class for calculating trace duration.
 * <p>
 * Provides methods for calculating total duration of traces from span timestamps.
 *
 * @author Ouroboros Team
 * @since 0.0.1
 */
@Slf4j
public class TraceDurationCalculator {
    
    /**
     * Calculates total duration of the trace from span timestamps.
     * <p>
     * Finds the earliest start time and latest end time across all spans,
     * then calculates the difference in milliseconds.
     *
     * @param spans List of trace span information
     * @return Total duration in milliseconds, or 0 if spans are empty or invalid
     */
    public static long calculateTotalDuration(List<TraceSpanInfo> spans) {
        if (spans == null || spans.isEmpty()) {
            return 0;
        }
        
        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        
        for (TraceSpanInfo span : spans) {
            if (span.getStartTimeNanos() != null && span.getEndTimeNanos() != null) {
                minStart = Math.min(minStart, span.getStartTimeNanos());
                maxEnd = Math.max(maxEnd, span.getEndTimeNanos());
            }
        }
        
        if (minStart == Long.MAX_VALUE || maxEnd == Long.MIN_VALUE) {
            return 0;
        }
        
        return (maxEnd - minStart) / 1_000_000; // Convert nanoseconds to milliseconds
    }
}

