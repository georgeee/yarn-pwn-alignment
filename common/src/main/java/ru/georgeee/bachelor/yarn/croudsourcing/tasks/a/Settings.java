package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("taskA_Settings")
@Getter @Setter
public class Settings {
    @Value("${cs.tasks.a.wMax:2}")
    private int wMax;
    @Value("${cs.tasks.a.dMax:5}")
    private int dMax;
    @Value("${cs.tasks.a.nMax:15}")
    private int nMax;
    @Value("${cs.tasks.a.dir}")
    private String dir;
    @Value("${cs.tasks.a.maxImages:4}")
    private int maxImages;
}
