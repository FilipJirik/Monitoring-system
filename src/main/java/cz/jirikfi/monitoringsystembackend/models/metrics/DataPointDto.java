package cz.jirikfi.monitoringsystembackend.models.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class DataPointDto {
    private Instant timestamp;
    private Double value;
}
