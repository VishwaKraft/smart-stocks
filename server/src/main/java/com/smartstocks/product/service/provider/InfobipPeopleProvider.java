package com.smartstocks.product.service.provider;

import com.smartstocks.product.models.CampaignSegmentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates or updates person records in Infobip's People CDP via the REST API.
 *
 * API reference: https://www.infobip.com/docs/people/manage-data/create-person
 *
 * Example curl:
 * curl --location 'https://m91qm6.api.infobip.com/people/2/persons' \
 *   --header 'Authorization: App <key>' \
 *   --data-raw '{ "firstName": "...", "lastName": "...",
 *                 "contactInformation": { "email": [...], "phone": [...] } }'
 */
@Slf4j
public class InfobipPeopleProvider {

    private final String apiKey;
    private final String peopleBaseUrl;
    private final RestTemplate restTemplate;

    public InfobipPeopleProvider(String apiKey, String peopleBaseUrl) {
        this.apiKey = apiKey;
        this.peopleBaseUrl = peopleBaseUrl.endsWith("/")
                ? peopleBaseUrl.substring(0, peopleBaseUrl.length() - 1)
                : peopleBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Creates a person record in Infobip for the given campaign segment user.
     * Extracts firstName / lastName from the user's data map if available.
     *
     * @param segmentUser the pre-generated campaign segment user record
     * @return the Infobip person ID on success, or null on failure
     */
    public String createPerson(CampaignSegmentUser segmentUser) {
        String url = peopleBaseUrl + "/people/2/persons";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "App " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        Map<String, Object> body = buildPersonPayload(segmentUser);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            log.info("[InfobipPeopleProvider] Person created for [{}]: status={}",
                    segmentUser.getEmailId(), response.getStatusCode());

            // Extract person ID from response
            if (response.getBody() != null) {
                Object personId = response.getBody().get("id");
                return personId != null ? personId.toString() : null;
            }
            return null;

        } catch (HttpClientErrorException ex) {
            // 409 Conflict means the person already exists — not an error
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("[InfobipPeopleProvider] Person already exists for [{}] — skipping creation.",
                        segmentUser.getEmailId());
                return "existing";
            }
            log.error("[InfobipPeopleProvider] HTTP error creating person for [{}]: status={}, body={}",
                    segmentUser.getEmailId(), ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return null;
        } catch (Exception ex) {
            log.error("[InfobipPeopleProvider] Unexpected error creating person for [{}]: {}",
                    segmentUser.getEmailId(), ex.getMessage(), ex);
            return null;
        }
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPersonPayload(CampaignSegmentUser segmentUser) {
        Map<String, Object> body = new HashMap<>();

        // Extract name fields from data map if not explicitly set
        String firstName = segmentUser.getFirstName();
        String lastName = segmentUser.getLastName();
        if ((firstName == null || firstName.isBlank()) && segmentUser.getData() != null) {
            Object fn = segmentUser.getData().get("firstName");
            if (fn != null) firstName = fn.toString();
        }
        if ((lastName == null || lastName.isBlank()) && segmentUser.getData() != null) {
            Object ln = segmentUser.getData().get("lastName");
            if (ln != null) lastName = ln.toString();
        }

        if (firstName != null && !firstName.isBlank()) body.put("firstName", firstName);
        if (lastName != null && !lastName.isBlank()) body.put("lastName", lastName);

        // Custom attributes from data map (excluding standard fields)
        if (segmentUser.getData() != null) {
            Map<String, Object> custom = new HashMap<>();
            for (Map.Entry<String, Object> entry : segmentUser.getData().entrySet()) {
                String key = entry.getKey();
                if (!key.equalsIgnoreCase("firstName") && !key.equalsIgnoreCase("lastName")
                        && !key.equalsIgnoreCase("email") && !key.equalsIgnoreCase("phone")) {
                    custom.put(key, entry.getValue());
                }
            }
            if (!custom.isEmpty()) {
                body.put("customAttributes", custom);
            }
        }

        // Contact information
        Map<String, Object> contactInfo = new HashMap<>();

        if (segmentUser.getEmailId() != null && !segmentUser.getEmailId().isBlank()) {
            Map<String, String> emailEntry = new HashMap<>();
            emailEntry.put("address", segmentUser.getEmailId());
            contactInfo.put("email", List.of(emailEntry));
        }

        if (segmentUser.getPhoneNumber() != null && !segmentUser.getPhoneNumber().isBlank()) {
            Map<String, String> phoneEntry = new HashMap<>();
            phoneEntry.put("number", segmentUser.getPhoneNumber());
            contactInfo.put("phone", List.of(phoneEntry));
        }

        if (!contactInfo.isEmpty()) {
            body.put("contactInformation", contactInfo);
        }

        return body;
    }
}
