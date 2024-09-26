package co.istad.jenkinsjob.domainName;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class SubDomainNameController {
    private final SubDomainNameService subdomainService;
    @PostMapping("/create-subdomain")
    public String createSubdomain( @RequestParam String subdomain,
                                  @RequestParam String ipAddress) {
        return subdomainService.createSubdomain(subdomain, ipAddress);
    }
    @DeleteMapping("/delete-subdomain")
    public String deleteSubdomain(@RequestParam String subdomain) {
        return subdomainService.deleteSubdomainByName(subdomain);
    }

    @GetMapping("/get-records")
    public String getRecords() {
        return subdomainService.getAllDNSRecords();
    }

}
