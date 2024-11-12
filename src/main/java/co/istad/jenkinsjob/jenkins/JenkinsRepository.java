package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.jenkins.dto.BuildInfo;
import co.istad.jenkinsjob.jenkins.dto.JobInfo;
import co.istad.jenkinsjob.jenkins.dto.PipelineInfo;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.model.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class JenkinsRepository {

    private final JenkinsServer jenkins;
    private final JenkinsHttpClient client;

    public JenkinsRepository(
            @Value("${jenkins.url}") String jenkinsUrl,
            @Value("${jenkins.username}") String jenkinsUsername,
            @Value("${jenkins.password}") String jenkinsPassword) throws URISyntaxException {
        this.client = new JenkinsHttpClient(new URI(jenkinsUrl), jenkinsUsername, jenkinsPassword);
        this.jenkins = new JenkinsServer(client);
    }

    public void createJob(String jobName, String jobConfig) throws IOException {
        jenkins.createJob(jobName, jobConfig);
    }

    public int startBuild(String jobName, Map<String,String> params) throws IOException,InterruptedException {
        JobWithDetails job = jenkins.getJob(jobName);
        QueueReference queueReference;

        if(params != null && !params.isEmpty()) {
            queueReference = job.build(params);
        } else {
            queueReference = job.build();
        }

        return getBuildNumberFromQueue(queueReference);
    }

    private int getBuildNumberFromQueue(QueueReference queueReference) throws IOException,InterruptedException {
        QueueItem queueItem = jenkins.getQueueItem(queueReference);
        while (queueItem.getExecutable() == null) {
            Thread.sleep(100);
            queueItem = jenkins.getQueueItem(queueReference);
        }

        return queueItem.getExecutable().getNumber().intValue();
    }

    public String getBuildLog(String jobName, int buildNumber) throws IOException {
        JobWithDetails job = jenkins.getJob(jobName);
        return job.getBuildByNumber(buildNumber).details().getConsoleOutputText();
    }

    public Build getBuild(String jobName, int buildNumber) throws IOException {
        JobWithDetails job = jenkins.getJob(jobName);
        return job.getBuildByNumber(buildNumber);
    }

    public void deleteJob(String jobName) throws IOException {
        jenkins.deleteJob(jobName, true);
    }

    public List<JobInfo> getJobs() throws IOException {
        List<JobInfo> jobInfos = new ArrayList<>();
        for (Job job : jenkins.getJobs().values()) {
            JobWithDetails jobDetails = job.details();
            Build lastBuild = jobDetails.getLastBuild();
            Build lastSuccessfulBuild = jobDetails.getLastSuccessfulBuild();

            String lastBuildStatus = (lastBuild != null) ? lastBuild.details().getResult().toString() : "N/A";
            String lastSuccessfulBuildNumber = (lastSuccessfulBuild != null) ? String.valueOf(lastSuccessfulBuild.getNumber()) : "N/A";

            jobInfos.add(new JobInfo(job.getName(), lastBuildStatus, lastSuccessfulBuildNumber));
        }
        return jobInfos;
    }

    public JobWithDetails getJob(String jobName) throws IOException {
        return jenkins.getJob(jobName);
    }

    public List<BuildInfo> getBuildsInfo(String jobName) throws IOException {
        JobWithDetails job = jenkins.getJob(jobName);
        List<BuildInfo> buildInfos = new ArrayList<>();

        for (Build build : job.getBuilds()) {
            BuildWithDetails buildDetails = build.details();
            String status = buildDetails.getResult() != null ? buildDetails.getResult().toString() : "BUILDING";
            String log = buildDetails.getConsoleOutputText();
            buildInfos.add(new BuildInfo(build.getNumber(), status,log));
        }

        return buildInfos;
    }

    public PipelineInfo getPipelineInfo(String jobName) throws IOException {
        JobWithDetails jobDetails = jenkins.getJob(jobName);

        if (jobDetails == null) {
            throw new IOException("Job not found");
        }

        String jobConfigXml = getJobConfigXml(jobName);
        String pipelineScript = extractPipelineScriptFromConfig(jobConfigXml);

        return new PipelineInfo(pipelineScript);
    }

    private String getJobConfigXml(String jobName) throws IOException {
        try {
            String path = "/job/" + jobName + "/config.xml";
            String response = client.get(path);
            return response;
        } catch (HttpResponseException e) {
            throw new IOException("Failed to fetch job configuration", e);
        }
    }

    private String extractPipelineScriptFromConfig(String jobConfigXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(jobConfigXml.getBytes()));

            NodeList scriptNodes = document.getElementsByTagName("script");
            if (scriptNodes.getLength() > 0) {
                Element scriptElement = (Element) scriptNodes.item(0);
                return scriptElement.getTextContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
