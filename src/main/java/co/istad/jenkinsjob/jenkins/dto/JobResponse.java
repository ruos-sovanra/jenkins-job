package co.istad.jenkinsjob.jenkins.dto;

import lombok.Builder;

@Builder
public record JobResponse(
        Long id,
        String name,
        String subdomain,
        String gitUrl,
        String branch
) {
}
