package cz.jirikfi.monitoringsystembackend.models.metrics;

import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import cz.jirikfi.monitoringsystembackend.models.metrics.DataPointDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsHistoryDto {
    private MetricType type;
    private String label;
    private String unit;
    private List<DataPointDto> data;
}
