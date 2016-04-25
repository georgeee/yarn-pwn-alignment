package ru.georgeee.bachelor.yarn.alignment;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"${app.dictXml}"})
public class AppConfiguration {
}
