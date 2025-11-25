package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import cz.jirikfi.monitoringsystembackend.Services.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/devices/{id}/metrics}")
public class MetricsController {
    @Autowired
    private MetricsService metricsService;

    // FIXME: Probably need to change to only call devices that have metrics not individual metrics database!!!!!!!!!!!!!!
    // FIXME: Need to JOIN between Device and Metrics

    // POST /api/devices/{id}/latestMetrics
    // most used endpoint
    // check if values are below threshold

//    @PostMapping
//    public ResponseEntity<List<Metrics>> getMetrics(@PathVariable UUID id) {
//
//    } // TODO

    /// GETs

    // GET /api/devices/{id}/allMetrics
    // get all metrics by device id
    // -> for longterm graphs for all of properties

    // GET /api/devices/{id}/getAll/cpu
    // GET /api/devices/{id}/getAll/{RAM|CPU|GPU|NETWORK|DISK|BATTERY}
    // GET only RAM | CPU | GPU | NETWORK | DISK | BATTERY metrics by device id
    // -> for single longterm graphs

    // USER can choose from modes like (AVERAGE OF DAY, WEEK, OF MONTH) to make average of old records



}
