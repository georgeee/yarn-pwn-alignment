package ru.georgeee.bachelor.yarn;

import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"ru.georgeee.bachelor.yarn.db.repo"})
@EntityScan(basePackages = {"ru.georgeee.bachelor.yarn.db.entity"})
@ImportResource({"ru/georgeee/bachelor/yarn/common-springContext.xml"})
public class CommonConfiguration {

}
