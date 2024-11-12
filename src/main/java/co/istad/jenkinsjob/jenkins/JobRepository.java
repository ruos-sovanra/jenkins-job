package co.istad.jenkinsjob.jenkins;

import co.istad.jenkinsjob.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job,Long> {

    Job findByName(String name);

    Job deleteJobByName(Job job);


}
