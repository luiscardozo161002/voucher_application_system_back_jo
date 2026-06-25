package mx.juarezdeoriente.solicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import mx.juarezdeoriente.solicitudes.config.CompanyProperties;

@SpringBootApplication
@EnableConfigurationProperties(CompanyProperties.class)
public class SolicitudesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolicitudesApplication.class, args);
    }
}
