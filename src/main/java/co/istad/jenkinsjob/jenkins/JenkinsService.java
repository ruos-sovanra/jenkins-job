package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.BuildRequest;
import co.istad.jenkinsjob.jenkins.dto.PiplineDto;

public interface JenkinsService {

    void createJob(PiplineDto piplineDto) throws Exception;

    int startBuild(BuildRequest buildRequest) throws Exception;

}
