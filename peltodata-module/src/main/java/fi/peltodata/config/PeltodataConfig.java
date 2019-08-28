package fi.peltodata.config;

import fi.nls.oskari.service.ServiceException;
import fi.peltodata.service.PeltodataService;
import fi.peltodata.service.PeltodataServiceMybatisImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PeltodataConfig {

    public static String PROP_UPLOAD_ROOT_DIR_PATH = "peltodata.upload.root.dir";

    @Bean
    public PeltodataService peltodataService() throws ServiceException {
        return new PeltodataServiceMybatisImpl();
    }
}
