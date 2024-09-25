package co.istad.jenkinsjob.jenkins.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record BuildRequest(
        String jobName,
        Map<String,String> params
) {
}
