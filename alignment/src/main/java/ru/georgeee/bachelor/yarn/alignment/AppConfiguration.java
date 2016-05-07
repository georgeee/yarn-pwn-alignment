package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import ru.georgeee.bachelor.yarn.Yarn;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

@Configuration
@ImportResource({"${app.dictXml}"})
public class AppConfiguration {

    @Autowired
    private DataSourceProperties properties;

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSourceBuilder factory = DataSourceBuilder
                .create(this.properties.getClassLoader())
                .url(this.properties.getUrl())
                .username(this.properties.getUsername())
                .password(this.properties.getPassword());
        if (this.properties.getType() != null) {
            factory.type(this.properties.getType());
        }
        return factory.build();
    }

    @Value("${pwn.home}")
    private String pwnHomePath;

    @Value("${yarn.xml}")
    private String yarnXmlPath;

    @Bean
    public Yarn yarn() throws IOException, JAXBException {
        return Yarn.create(Paths.get(yarnXmlPath));
    }

    @Bean
    public IDictionary pwn() throws IOException {
        IDictionary pwnDict = new Dictionary(new URL("file", null, pwnHomePath));
        pwnDict.open();
        return pwnDict;
    }

}
