package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.domain.Job;
import co.istad.jenkinsjob.domainName.SubDomainNameService;
import co.istad.jenkinsjob.jenkins.dto.*;
import co.istad.jenkinsjob.mapper.JobMapper;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JenkinsServiceImpl implements JenkinsService{

    private final JenkinsRepository jenkinsRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SubDomainNameService subDomainNameService;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Override
    public void createJob(PiplineDto piplineDto) throws Exception {
        try {
            // Log the start of the job creation process
            System.out.println("Starting job creation for: " + piplineDto.name());

            // Create subdomain
            subDomainNameService.createSubdomain(piplineDto.subdomain(), "146.190.89.57");
            System.out.println("Subdomain created: " + piplineDto.subdomain());

            // Create job configuration
            String jobConfig = createJobConfig(piplineDto);
            System.out.println("Job configuration created for: " + piplineDto.name());

            // Create job in Jenkins
            jenkinsRepository.createJob(piplineDto.name(), jobConfig);
            System.out.println("Job created in Jenkins: " + piplineDto.name());

            // Save job details to the repository
            Job job = new Job();
            job.setName(piplineDto.name());
            job.setSubdomain(piplineDto.subdomain());
            job.setGitUrl(piplineDto.gitUrl());
            job.setBranch(piplineDto.branch());
            jobRepository.save(job);
            System.out.println("Job details saved to repository: " + piplineDto.name());
        } catch (Exception e) {
            // Log the exception
            System.err.println("Failed to create job: " + e.getMessage());
            throw new Exception("Failed to create job", e);
        }
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

    @Override
    public SseEmitter streamLog(String jobName, int buildNumber) throws IOException, InterruptedException {
        // Set the timeout to a longer duration, e.g., 30 minutes (1800000 milliseconds)
        SseEmitter emitter = new SseEmitter(1800000L);
        executor.execute(() -> {
            try {
                Build build = jenkinsRepository.getBuild(jobName, buildNumber);
                int lastReadPosition = 0;

                while (true) {
                    String newLogs = getNewLogs(build, lastReadPosition);
                    if (!newLogs.isEmpty()) {
                        emitter.send(SseEmitter.event().data(newLogs));
                        lastReadPosition += newLogs.length();
                    }
                    if (!build.details().isBuilding()) {
                        break; // Exit loop if build is complete
                    }
                    Thread.sleep(1000);
                }
                // Send remaining logs and complete the emitter
                String remainingLogs = getNewLogs(build, lastReadPosition);
                if (!remainingLogs.isEmpty()) {
                    emitter.send(SseEmitter.event().data(remainingLogs));
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @Override
    public void deleteJob(String name) throws IOException {
        try {
            Job job = jobRepository.findByName(name);
            if (job == null) {
                throw new IOException("Job not found: " + name);
            }
            jobRepository.delete(job);
            jenkinsRepository.deleteJob(name);
            System.out.println("Job deleted successfully: " + name);
        } catch (Exception e) {
            System.err.println("Failed to delete job: " + e.getMessage());
            throw new IOException("Failed to delete job: " + name, e);
        }
    }

    @Override
    public List<JobResponse> getJobs() throws IOException {
        List<Job> jobs = jobRepository.findAll();

        return jobs.stream().map(jobMapper::toJobInfo).collect(Collectors.toList());
    }

    @Override
    public ArrayList<Integer> getAllBuildNumbersByJobName(String jobName) throws IOException {
        JobWithDetails job = jenkinsRepository.getJob(jobName);

        if (job == null) {
            throw new IOException("Job not found: " + jobName);
        }

        return job.getBuilds().stream()
                .map(Build::getNumber)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public JobResponse getJobsByJobName(String jobName) throws IOException {
        Job job = jobRepository.findByName(jobName);

        if (job == null) {
            throw new IOException("Job not found: " + jobName);
        }

        return jobMapper.toJobInfo(job);
    }

    @Override
    public List<BuildInfo> getBuildsInfo(String jobName) throws IOException {

       return jenkinsRepository.getBuildsInfo(jobName);
    }

    @Override
    public PipelineInfo getPiplineInfo(String jobName) throws IOException {
        return jenkinsRepository.getPipelineInfo(jobName);
    }

    private String getNewLogs(Build build, int lastReadPosition) throws IOException {
        String fullLog = build.details().getConsoleOutputText();
        if (lastReadPosition < fullLog.length()) {
            return fullLog.substring(lastReadPosition);
        }
        return "";
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
                        "      <triggers>\n" +
                        "        <com.cloudbees.jenkins.GitHubPushTrigger plugin=\"github@1.29.4\">\n" +
                        "          <spec></spec>\n" +
                        "        </com.cloudbees.jenkins.GitHubPushTrigger>\n" +
                        "      </triggers>\n" +
                        "    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>\n" +
                        "  </properties>\n" +
                        "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@2.90\">\n" +
                        "    <script>%s</script>\n" +
                        "    <sandbox>true</sandbox>\n" +
                        "  </definition>\n" +
                        "  <triggers>\n" +
                        "    <com.cloudbees.jenkins.GitHubPushTrigger plugin=\"github@1.29.4\">\n" +
                        "      <spec></spec>\n" +
                        "    </com.cloudbees.jenkins.GitHubPushTrigger>\n" +
                        "  </triggers>\n" +
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
        String subDomain = pipelineRequest.subdomain().toLowerCase().replace(" ", "-");
        String token = pipelineRequest.token();

        System.out.println("Subdomain: " + subDomain);

        return String.format(
                "@Library('project-type-detect') _\n" +
                        "pipeline {\n" +
                        "    agent any\n" +
                        "    environment {\n" +
                        "        GIT_REPO_URL = '%s'\n" +
                        "        GIT_BRANCH = '%s'\n" +
                        "        DOCKER_IMAGE_NAME = '%s'\n" +
                        "        DOCKER_IMAGE_TAG = '${BUILD_NUMBER}'\n" +
                        "        DEPLOY_PORT = '%d'\n" +
                        "        CONTAINER_NAME = '%s'\n" +
                        "        SUBDOMAIN = '%s'\n" +
                        "        DOMAIN = 'psa-khmer.world'\n" +
                        "        GITHUB_TOKEN = '%s'\n" +
                        "        WEBHOOK_URL = 'https://jenkins.psa-khmer.world/github-webhook/'\n" +
                        "    }\n" +
                        "    stages {\n" +
                        "        stage('Checkout') {\n" +
                        "            steps {\n" +
                        "                git branch: env.GIT_BRANCH, url: env.GIT_REPO_URL\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Generate Dockerfile') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    generateDockerfile(\"${env.WORKSPACE}\")\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Build') {\n" +
                        "            steps {\n" +
                        "                sh \"docker build -t ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG} .\"\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Deploy') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    deployContainer(env.CONTAINER_NAME, env.DOCKER_IMAGE_NAME, env.DOCKER_IMAGE_TAG, env.DEPLOY_PORT)\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Configure Nginx and Certbot') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    configureNginxAndCertbot(env.SUBDOMAIN, env.DOMAIN, env.DEPLOY_PORT)\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "        stage('Configure GitHub Webhook') {\n" +
                        "            steps {\n" +
                        "                script {\n" +
                        "                    createGitHubWebhook(env.GIT_REPO_URL, env.WEBHOOK_URL, env.GITHUB_TOKEN)\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                pipelineRequest.gitUrl(),
                pipelineRequest.branch(),
                pipelineRequest.name(),
                randomPort,
                containerName,
                subDomain,
                token
        );
    }
}
