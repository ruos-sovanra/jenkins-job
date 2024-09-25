package co.istad.jenkinsjob.jenkins.dto;

import lombok.Builder;

@Builder
public record PiplineDto(
        String name,
        String gitUrl,
        String branch,
        String token
) {
}
