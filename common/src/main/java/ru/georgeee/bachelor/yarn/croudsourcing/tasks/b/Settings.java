package ru.georgeee.bachelor.yarn.croudsourcing.tasks.b;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("taskB_Settings")
@Getter @Setter
public class Settings {
    @Value("${cs.tasks.b.nMax:15}")
    private int nMax;
    @Value("${cs.tasks.b.dir}")
    private String dir;
    @Value("${cs.tasks.b.maxImages:4}")
    private int maxImages;
}
