package co.istad.jenkinsjob.jenkins.dto;

import lombok.Builder;

@Builder
public record BuildInfo(
        int buildNumber,
        String status,
        String log
) {
}
