package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.BuildRequest;
import co.istad.jenkinsjob.jenkins.dto.PiplineDto;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class JenkinsServiceImpl implements JenkinsService{

    private final JenkinsRepository jenkinsRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger logger = LoggerFactory.getLogger(JenkinsService.class);

    @Override
    public void createJob(PiplineDto piplineDto) throws Exception {

        String jobConfig = createJobConfig(piplineDto);
        jenkinsRepository.createJob(piplineDto.name(), jobConfig);
    }

    @Override
    public int startBuild(BuildRequest buildRequest) throws Exception {
       try {
           return jenkinsRepository.startBuild(buildRequest.jobName(), buildRequest.params());
       } catch (Exception e) {
           throw new Exception("Failed to start build", e);
       }
    }

    @Override
    public void streamBuildLog(String jobName, int buildNumber) throws IOException, InterruptedException {
        Build build = jenkinsRepository.getBuild(jobName, buildNumber);
        BuildWithDetails buildWithDetails = build.details();

        while (buildWithDetails.isBuilding()) {
            System.out.print(buildWithDetails.getConsoleOutputText());
            Thread.sleep(2000); // Sleep for 2 seconds before fetching the log again
            buildWithDetails = build.details(); // Refresh build details
        }

        // Print remaining log after build is complete
        System.out.print(buildWithDetails.getConsoleOutputText());
    }


    private String createJobConfig(PiplineDto piplineDto) {
        String pipelineScript = createPipelineScript(piplineDto);
        return String.format(
                "<?xml version='1.1' encoding='UTF-8'?>\n" +
                        "<flow-definition plugin=\"workflow-job@2.40\">\n" +
                        "  <description>%s</description>\n" +
                        "  <keepDependencies>false</keepDependencies>\n" +
                        "  <properties>\n" +
                        "    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>\n" +
                        "      <triggers/>\n" +
                        "    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>\n" +
                        "  </properties>\n" +
                        "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@2.90\">\n" +
                        "    <script>%s</script>\n" +
                        "    <sandbox>true</sandbox>\n" +
                        "  </definition>\n" +
                        "  <triggers/>\n" +
                        "  <disabled>false</disabled>\n" +
                        "</flow-definition>",
                piplineDto.name(),
                pipelineScript
        );
    }

    private String createPipelineScript(PiplineDto pipelineRequest) {
        Random random = new Random();
        int randomPort = 3000 + random.nextInt(1000); // Random port between 3000 and 3999
        String containerName = pipelineRequest.name() + "-container";

        return String.format(
                "pipeline {\n" +
                        "    agent any\n" +
                        "    environment {\n" +
                        "        GIT_REPO_URL = '%s'\n" +
                        "        GIT_BRANCH = '%s'\n" +
                        "        DOCKER_IMAGE_NAME = '%s'\n" +
                        "        DOCKER_IMAGE_TAG = '${BUILD_NUMBER}'\n" +
                        "        DEPLOY_PORT = '%d'\n" +
                        "        CONTAINER_NAME = '%s'\n" +
                        "    }\n" +
                        "    stages {\n" +
                        "        stage('Checkout') {\n" +
                        "            steps {\n" +
                        "                git branch: env.GIT_BRANCH, url: env.GIT_REPO_URL\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Build') {\n" +
                        "            steps {\n" +
                        "                sh 'docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} .'\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Deploy') {\n" +
                        "            steps {\n" +
                        "                sh '''\n" +
                        "                    docker stop ${CONTAINER_NAME} || true\n" +
                        "                    docker rm ${CONTAINER_NAME} || true\n" +
                        "                    docker run -d --name ${CONTAINER_NAME} -p ${DEPLOY_PORT}:3000 ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}\n" +
                        "                '''\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                pipelineRequest.gitUrl(),
                pipelineRequest.branch(),
                pipelineRequest.name(),
                randomPort,
                containerName
        );
    }
}
