package cz.jirikfi.monitoringsystembackend.models.thresholds;

import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThresholdResponseDto {

    private UUID id;
    private MetricType metricType;
    private ThresholdOperator operator;
    private Double thresholdValue;
    private AlertSeverity severity;
}
