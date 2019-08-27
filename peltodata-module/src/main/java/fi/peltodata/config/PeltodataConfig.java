package fi.peltodata.config;

import fi.nls.oskari.service.ServiceException;
import fi.peltodata.service.PeltodataService;
import fi.peltodata.service.PeltodataServiceMybatisImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PeltodataConfig {

    @Bean
    public PeltodataService peltodataService() throws ServiceException {
        return new PeltodataServiceMybatisImpl();
    }
}
