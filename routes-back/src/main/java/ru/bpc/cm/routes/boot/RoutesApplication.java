package ru.bpc.cm.routes.boot;

import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.TimeZone;

@SpringBootApplication
public class RoutesApplication implements WebMvcConfigurer {

    private Logger slf4jLog = LoggerFactory.getLogger(RoutesApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RoutesApplication.class, args);
    }

    @Bean
    public DataSource dataSource() throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setUser(RoutesBootConstants.DB_USERNAME);
        dataSource.setPassword(RoutesBootConstants.DB_PASSWORD);
        dataSource.setURL(RoutesBootConstants.DB_URL);
        dataSource.setImplicitCachingEnabled(true);
        dataSource.setFastConnectionFailoverEnabled(true);
        slf4jLog.info("Data Source successfully configured");
        return dataSource;
        //TODO find better way of datasource config
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        slf4jLog.info("TimeZone set as Default");
        return jacksonObjectMapperBuilder ->
                jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
        //TODO timezone can be set according to situation
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration corsRegistration = registry.addMapping("/**"); //what mappings are affected
        corsRegistration.allowedMethods("GET", "POST", "DELETE", "PATCH"); //add other methods if implemented
        corsRegistration.allowedOrigins("*"); //all origins allowed, MUST be changed without security
        corsRegistration.allowedHeaders("*"); //what headers can be exposed
        corsRegistration.maxAge(3600); //expiration time of CORS confirmation
    }

}
