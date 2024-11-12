package co.istad.jenkinsjob;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
//        servers = {
//                @Server(url = "https://jen-api.psa-khmer.world", description = "Deploy API Server"),
//        },
        info = @Info(
                title = "Jenkins(DevOps Class)",
                version = "1.0",
                description = "DevOps Project"
        )
)
public class JenkinsJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(JenkinsJobApplication.class, args);
    }

}
