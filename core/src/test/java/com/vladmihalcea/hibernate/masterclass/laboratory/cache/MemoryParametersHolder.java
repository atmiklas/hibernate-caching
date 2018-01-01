package com.vladmihalcea.hibernate.masterclass.laboratory.cache;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Created by amik on 12/26/17.
 */
@Data
@Builder
@ToString
public class MemoryParametersHolder {
    private long freeMemoryBefore;
    private long freeMemoryAfter;
    private long maxMemory;
    private long totalMemory;

    public long getConsumedMemory() {
        return (freeMemoryBefore - freeMemoryAfter);
    }
}
