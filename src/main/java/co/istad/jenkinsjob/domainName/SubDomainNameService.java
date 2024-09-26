package co.istad.jenkinsjob.domainName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class SubDomainNameService {

    @Value("${name.com.api.token}")
    private String apiToken;

    @Value("${name.com.api.username}")
    private String apiUsername;

    private final String domain = "psa-khmer.world";

    public String createSubdomain(String subdomainName, String ipAddress) {
        RestTemplate restTemplate = new RestTemplate();

        // Use production API endpoint instead of dev endpoint
        String url = "https://api.name.com/v4/domains/" + domain + "/records";
        System.out.println("URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(apiUsername, apiToken); // Basic Auth with externalized username and token

        // Request body for creating subdomain
        String requestBody = String.format(
                "{\"host\":\"%s\",\"type\":\"A\",\"answer\":\"%s\",\"ttl\":300}",
                subdomainName, ipAddress
        );

        // Debugging: Print the request body
        System.out.println("Request body for subdomain creation: " + requestBody);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // Make the POST request
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                return "Subdomain created successfully.";
            } else {
                return "Error creating subdomain: " + response.getBody();
            }

        } catch (HttpClientErrorException e) {
            // Handle specific client errors (e.g., 4xx errors)
            return "Client error while creating subdomain: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            // Handle other exceptions (e.g., connection issues)
            return "Error creating subdomain: " + e.getMessage();
        }
    }

    // Method to delete subdomain by its name
    public String deleteSubdomainByName(String subdomainName) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON parsing

        // Step 1: Get all DNS records for the domain
        String getUrl = "https://api.name.com/v4/domains/" + domain + "/records";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(apiUsername, apiToken);
        HttpEntity<String> getRequestEntity = new HttpEntity<>(headers);

        try {
            // Fetch all records for the domain
            ResponseEntity<String> getResponse = restTemplate.exchange(getUrl, HttpMethod.GET, getRequestEntity, String.class);

            if (getResponse.getStatusCode().is2xxSuccessful()) {
                String responseBody = getResponse.getBody();
                System.out.println("All DNS records: " + responseBody);

                // Step 2: Parse the response to find the record for the specific subdomain
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode records = jsonResponse.get("records");

                // Ensure "records" node is not null
                if (records == null || !records.isArray()) {
                    return "No DNS records found for the domain.";
                }

                String recordId = null;

                for (JsonNode record : records) {
                    // Add null check before accessing "host"
                    JsonNode hostNode = record.get("host");
                    if (hostNode != null && hostNode.asText().equals(subdomainName)) {
                        JsonNode idNode = record.get("id");

                        // Check if "id" is not null before using it
                        if (idNode != null) {
                            recordId = idNode.asText();
                            break;
                        }
                    }
                }

                if (recordId == null) {
                    return "Subdomain not found: " + subdomainName;
                }

                // Step 3: Delete the specific record by ID
                String deleteUrl = "https://api.name.com/v4/domains/" + domain + "/records/" + recordId;
                HttpEntity<String> deleteRequestEntity = new HttpEntity<>(headers);

                ResponseEntity<String> deleteResponse = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteRequestEntity, String.class);

                if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                    return "Subdomain deleted successfully: " + subdomainName;
                } else {
                    return "Error deleting subdomain: " + deleteResponse.getBody();
                }
            } else {
                return "Error fetching records: " + getResponse.getBody();
            }

        } catch (HttpClientErrorException e) {
            // Handle client error (e.g., 4xx status)
            return "Client error: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            // Handle other exceptions
            return "Error: " + e.getMessage();
        }
    }

    public String getAllDNSRecords() {
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON parsing

    // Step 1: Get all DNS records for the domain
    String getUrl = "https://api.name.com/v4/domains/" + domain + "/records";
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(apiUsername, apiToken);
    HttpEntity<String> getRequestEntity = new HttpEntity<>(headers);

    try {
        // Fetch all records for the domain
        ResponseEntity<String> getResponse = restTemplate.exchange(getUrl, HttpMethod.GET, getRequestEntity, String.class);

        if (getResponse.getStatusCode().is2xxSuccessful()) {
            String responseBody = getResponse.getBody();
            System.out.println("All DNS records: " + responseBody);

            // Pretty-print the JSON response
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } else {
            return "Error fetching records: " + getResponse.getBody();
        }

    } catch (HttpClientErrorException e) {
        // Handle client error (e.g., 4xx status)
        return "Client error: " + e.getResponseBodyAsString();
    } catch (Exception e) {
        // Handle other exceptions
        return "Error: " + e.getMessage();
    }
}
}
