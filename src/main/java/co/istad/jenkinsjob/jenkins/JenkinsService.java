package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface JenkinsService {

    void createJob(PiplineDto piplineDto) throws Exception;

    int startBuild(BuildRequest buildRequest) throws Exception;

    void streamBuildLog(String jobName, int buildNumber) throws IOException, InterruptedException;

    SseEmitter streamLog(String jobName, int buildNumber) throws IOException, InterruptedException;

    void deleteJob(String jobName) throws IOException;

    List<JobResponse> getJobs() throws IOException;

    ArrayList<Integer> getAllBuildNumbersByJobName(String jobName) throws IOException;

    JobResponse getJobsByJobName(String jobName) throws IOException;


    List<BuildInfo> getBuildsInfo(String jobName) throws IOException;

    PipelineInfo getPiplineInfo(String jobName) throws IOException;
}
