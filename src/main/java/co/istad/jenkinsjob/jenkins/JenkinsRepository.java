package co.istad.jenkinsjob.jenkins;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Repository
public class JenkinsRepository {

    private final JenkinsServer jenkins;

    public JenkinsRepository(
            @Value("${jenkins.url}") String jenkinsUrl,
            @Value("${jenkins.username}") String jenkinsUsername,
            @Value("${jenkins.password}") String jenkinsPassword) throws URISyntaxException {
        JenkinsHttpClient client = new JenkinsHttpClient(new URI(jenkinsUrl), jenkinsUsername, jenkinsPassword);
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


}
