package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.app.AppUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Component("taskA_Settings")
@Getter @Setter
public class Settings {
    @Value("${cs.tasks.a.aggr.tags:}")
    private String aggrTagsRaw;
    @Value("${cs.tasks.a.aggr.threshold:0.4}")
    private double aggrThreshold;
    @Value("${cs.tasks.a.dMax:5}")
    private int dMax;
    @Value("${cs.tasks.a.nMax:15}")
    private int nMax;
    @Value("${cs.tasks.a.dir}")
    private String dir;
    @Value("${cs.tasks.a.maxImages:4}")
    private int maxImages;

    private Set<String> aggrTags;


    @PostConstruct
    private void init(){
        aggrTags = AppUtils.splitParam(aggrTagsRaw, Function.identity(), HashSet::new);
    }
}
