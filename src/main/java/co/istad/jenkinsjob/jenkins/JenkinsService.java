package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.BuildRequest;
import co.istad.jenkinsjob.jenkins.dto.PiplineDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface JenkinsService {

    void createJob(PiplineDto piplineDto) throws Exception;

    int startBuild(BuildRequest buildRequest) throws Exception;

    void streamBuildLog(String jobName, int buildNumber) throws IOException, InterruptedException;

    SseEmitter streamLog(String jobName, int buildNumber) throws IOException, InterruptedException;

}
