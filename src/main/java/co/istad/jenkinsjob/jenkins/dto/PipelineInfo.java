package co.istad.jenkinsjob.jenkins.dto;

import lombok.Builder;

@Builder
public record PipelineInfo(
        String pipeline
) {
}
