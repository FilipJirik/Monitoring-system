package cz.jirikfi.monitoringsystembackend.models.thresholds;

import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateThresholdRequestDto {

    @NotNull(message = "Metric type cannot be null")
    private MetricType metricType;

    @NotNull(message = "Operator cannot be null")
    private ThresholdOperator operator;

    @NotNull(message = "Threshold value cannot be null")
    private Double thresholdValue;

    @NotNull(message = "Severity cannot be null")
    private AlertSeverity severity;
}
