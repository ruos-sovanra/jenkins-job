package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.BuildRequest;
import co.istad.jenkinsjob.jenkins.dto.PiplineDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/jenkins")
@RequiredArgsConstructor
public class JenkinsController {

    private final JenkinsService jenkinsService;

    @PostMapping("/create-job")
    public ResponseEntity<String> createPipeline(@RequestBody PiplineDto pipelineRequest) {

        try {
            jenkinsService.createJob(pipelineRequest);
            return ResponseEntity.ok("Job created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create job");
        }
    }

    @PostMapping("/start-build")
    public ResponseEntity<String> startBuild(@RequestBody BuildRequest buildRequest) {

        try {
            int buildNumber = jenkinsService.startBuild(buildRequest);
            return ResponseEntity.ok("Build started successfully with build number: " + buildNumber);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to start build");
        }
    }

    @GetMapping("/stream-logs")
    public ResponseEntity<String> streamLogs(@RequestParam String jobName, @RequestParam int buildNumber) {
        try {
            jenkinsService.streamBuildLog(jobName, buildNumber);
            return ResponseEntity.ok("Log streaming started");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to stream logs");
        }
    }

    @GetMapping("/stream-build-log/{jobName}/{buildNumber}")
    public SseEmitter streamBuildLog(@PathVariable String jobName, @PathVariable int buildNumber) throws IOException, InterruptedException {
        return jenkinsService.streamLog(jobName, buildNumber);
    }

}
