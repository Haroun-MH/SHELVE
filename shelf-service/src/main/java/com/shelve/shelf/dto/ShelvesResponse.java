package com.shelve.shelf.dto;

import com.shelve.shelf.entity.ShelfType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelvesResponse {
    private List<ShelfItemResponse> reading;
    private List<ShelfItemResponse> read;
    private List<ShelfItemResponse> toRead;
    private ShelfStats stats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShelfStats {
        private long readingCount;
        private long readCount;
        private long toReadCount;
    }
}
