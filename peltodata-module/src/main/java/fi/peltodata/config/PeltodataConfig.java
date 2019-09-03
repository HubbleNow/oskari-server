package fi.peltodata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.nls.oskari.service.ServiceException;
import fi.peltodata.service.PeltodataService;
import fi.peltodata.service.PeltodataServiceMybatisImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class PeltodataConfig {

    public static String PROP_UPLOAD_ROOT_DIR_PATH = "peltodata.upload.root.dir";

    @Bean
    public PeltodataService peltodataService() throws ServiceException {
        return new PeltodataServiceMybatisImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(1000000000000L);
        resolver.setMaxUploadSizePerFile(1000000000000L);
        return resolver;
    }
}
