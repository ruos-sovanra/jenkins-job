package co.istad.jenkinsjob.jenkins.dto;

public record JobInfo(
        String name,
        String lastBuildStatus,
        String lastSuccessfulBuild
) {
}
