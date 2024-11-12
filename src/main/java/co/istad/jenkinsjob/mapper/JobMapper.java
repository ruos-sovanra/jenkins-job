package co.istad.jenkinsjob.mapper;

import co.istad.jenkinsjob.domain.Job;
import co.istad.jenkinsjob.jenkins.dto.JobInfo;
import co.istad.jenkinsjob.jenkins.dto.JobResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobMapper {
    JobResponse toJobInfo(Job job);
}
