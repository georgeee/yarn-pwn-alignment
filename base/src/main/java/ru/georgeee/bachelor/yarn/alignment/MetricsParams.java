package ru.georgeee.bachelor.yarn.alignment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MetricsParams {
    @Value("${metrics.p1.mean:0.5}")
    private double p1Mean;
    @Value("${metrics.p1.sd:0.2}")
    private double p1Sd;
    @Value("${metrics.p2.sd:0.2}")
    private double p2Sd;
}
