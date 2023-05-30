package com.braunSebs.rpaetc.vendors.uiPath.service;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.braunSebs.rpaetc.vendors.uiPath.config.UiPathAuthenticator;
import com.braunSebs.rpaetc.vendors.uiPath.model.JobTask;
import com.braunSebs.rpaetc.vendors.uiPath.model.StartInfo;

/**
 * Service that handles UiPath api Services.
 */
@Service
public class UiPathApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathApiService.class);

    @Autowired
    private RestTemplate uiPathDirectoryRestClient;

    @Autowired
    private UiPathTaskExecutor uiPathTaskExecutor;

    @Autowired
    private UiPathAuthenticator uiPathAuthenticator;

    @Value("${bridge.uipath.cloud.url}")
    private String cloudUrl;

    @Value("${bridge.uipath.cloud.org}")
    private String cloudOrg;

    @Value("${bridge.uipath.cloud.tenant}")
    private String cloudTenant;

    /**
     * Fetches the ID of a specific folder by its name.
     *
     * @param folderName The name of the folder whose ID is to be fetched.
     * @param jobTask    The job task instance, which includes additional
     *                   information necessary for the API call.
     * @return The ID of the specified folder.
     * @throws RuntimeException if the request to fetch the folder ID fails or if
     *                          there is an error in parsing the JSON response.
     */
    public int getFolderByName(String folderName, JobTask jobTask) {
        LOGGER.debug("Initiating request to fetch ID for the folder '{}'", folderName);
        uiPathAuthenticator.refreshTokenIfExpired();
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(jobTask.getHeader());

            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    "https://cloud.uipath.com/{cloudOrg}/{cloudTenant}/odata/Folders?$Filter=DisplayName%20eq%20'{folderName}'",
                    HttpMethod.GET,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant,
                    folderName);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("Request to fetch folder ID failed. Response body: {}", responseEntity.getBody());
                throw new RuntimeException("Failed to fetch folder ID. Response body: " + responseEntity.getBody());
            }

            JSONObject responseEntityGetFolderAsJson = new JSONObject(responseEntity.getBody());
            Integer folderId = responseEntityGetFolderAsJson.getJSONArray("value").getJSONObject(0).getInt("Id");

            LOGGER.info("Successfully fetched ID '{}' for the folder '{}'", folderId, folderName);
            return folderId;
        } catch (RestClientException e) {
            LOGGER.error("Exception occurred while fetching the folder ID for '{}': {}", folderName, e.getMessage(), e);
            throw new RuntimeException("Exception occurred while fetching the folder ID for " + folderName, e);
        } catch (JSONException e) {
            LOGGER.error("Exception occurred while parsing JSON response for the folder '{}': {}", folderName,
                    e.getMessage(), e);
            throw new RuntimeException("Exception occurred while parsing JSON response for the folder " + folderName,
                    e);
        }
    }

    /**
     * Fetches the release key of a specific robot using its name.
     *
     * @param robotName The name of the robot whose release key is to be fetched.
     * @param jobTask   The job task instance, which includes additional information
     *                  necessary for the API call.
     * @return The release key of the specified robot.
     * @throws RuntimeException if the request to fetch the release key fails or if
     *                          there is an error in parsing the JSON response.
     */
    public String getReleaseKeyByName(String robotName, JobTask jobTask) {
        // Log the initiation of the request
        LOGGER.debug("Fetching release key for robot: '{}'", robotName);

        // Refresh the token if it's expired
        uiPathAuthenticator.refreshTokenIfExpired();

        // Set the bearer authorization token
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        try {
            // Prepare the request entity with appropriate headers
            HttpEntity<String> requestEntity = new HttpEntity<>(jobTask.getHeader());

            // Perform the API call and get the response
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    cloudUrl + "/{cloudOrg}/{cloudTenant}/odata/Releases?$filter=Name%20eq%20'{robotName}'",
                    HttpMethod.GET,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant,
                    robotName);

            // If the API call was unsuccessful, log the error and throw an exception
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                LOGGER.error("Failed to fetch release key for robot: '{}'. Response body: {}", robotName,
                        responseEntity.getBody());
                throw new RuntimeException("Failed to fetch release key for robot: " + robotName + ". Response body: "
                        + responseEntity.getBody());
            }

            // Parse the response body to JSON
            JSONObject responseEntityAsJson = new JSONObject(responseEntity.getBody());

            // Extract the release key from the JSON response
            String releaseKey = responseEntityAsJson.getJSONArray("value").getJSONObject(0).getString("Key");

            // Log the success and return the release key
            LOGGER.info("Fetched release key for robot '{}': {}", robotName, releaseKey);
            return releaseKey;
        } catch (RestClientException e) {
            // Log and throw an exception if there was a problem with the API call
            LOGGER.error("Error occurred while fetching the release key for robot '{}': {}", robotName, e.getMessage(),
                    e);
            throw new RuntimeException("Error occurred while fetching the release key for robot: " + robotName, e);
        } catch (JSONException e) {
            // Log and throw an exception if there was a problem parsing the JSON response
            LOGGER.error("Error occurred while parsing JSON response for robot '{}': {}", robotName, e.getMessage(), e);
            throw new RuntimeException("Error occurred while parsing JSON response for robot: " + robotName, e);
        }
    }

    /**
     * Initiates the UiPath robot.
     *
     * @param releaseKey The release key associated with the robot to be initiated.
     * @param jobTask    An instance of JobTask containing necessary information for
     *                   the API call.
     * @return The ID of the job initiated for the robot.
     * @throws RuntimeException if there's an issue initiating the robot or
     *                          processing the response.
     */
    public int startRobot(String releaseKey, JobTask jobTask) {
        LOGGER.debug("Starting robot with release key {} and input arguments {}", releaseKey,
                jobTask.getProcessVariables());
        uiPathAuthenticator.refreshTokenIfExpired();
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Convert input arguments to JSON string
            String inputArgumentsString = objectMapper.writeValueAsString(jobTask.getProcessVariables());

            // Create start info object
            StartInfo startInfo = new StartInfo(releaseKey, "ModernJobsCount", 1, inputArgumentsString);

            // Convert start info object to JSON string
            String startInfoString = objectMapper.writeValueAsString(startInfo);
            LOGGER.debug("Start info: {}", startInfoString);

            // Send request to start robot
            HttpEntity<String> requestEntity = new HttpEntity<>(startInfoString, jobTask.getHeader());
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    "https://cloud.uipath.com/{cloudOrg}/{cloudTenant}/odata/Jobs/UiPath.Server.Configuration.OData.StartJobs",
                    HttpMethod.POST,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant);

            // Check if the request was successful
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // Parse response and get job ID
                JSONObject responseEntityStartAsJson = new JSONObject(responseEntity.getBody());
                int jobId = responseEntityStartAsJson.getJSONArray("value").getJSONObject(0).getInt("Id");
                LOGGER.info("Started robot with job ID {}", jobId);
                return jobId;
            } else {
                // Handle error if the request was unsuccessful
                String responseBody = responseEntity.getBody();
                LOGGER.error("Failed to start robot. Response body: {}", responseBody);
                throw new RuntimeException("Failed to start robot. Response body: " + responseBody);
            }

        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing JSON for start info", e);
            throw new RuntimeException("Error processing JSON for start info", e);
        } catch (HttpClientErrorException e) {
            // Handle HTTP client errors
            LOGGER.error("Failed to make HTTP request", e);
            throw new RuntimeException("Failed to make HTTP request: " + e.getMessage());
        } catch (Exception e) {
            // Handle unknown errors
            LOGGER.error("An unknown error occurred", e);
            throw new RuntimeException("An unknown error occurred: " + e.getMessage());
        }
    }

    /**
     * Adds an item to a UiPath queue.
     *
     * @param name           the name of the queue to add the item to
     * @param inputArguments the input arguments to be passed along with the queue
     *                       item
     * @param jobTask        the current job task
     * @throws RuntimeException if an error occurs while adding the queue item
     */
    public void addQueueItem(JobTask jobTask) {
        String name = jobTask.getProcessVariables().get("bridge_queueName").toString();
        LOGGER.debug("Adding queue item to queue '{}' with input arguments: {}", jobTask.getProcessVariables(),
                jobTask.getProcessVariables());
        uiPathAuthenticator.refreshTokenIfExpired();
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        try {
            // Prepare queue item data
            JSONObject specificContentJson = new JSONObject(jobTask.getProcessVariables());
            JSONObject itemData = new JSONObject()
                    .put("Priority", "Normal")
                    .put("Name", name)
                    .put("SpecificContent", specificContentJson);
            JSONObject itemDataWrapped = new JSONObject().put("itemData", itemData);

            // Send request to add queue item
            HttpEntity<String> requestEntity = new HttpEntity<>(itemDataWrapped.toString(),
                    jobTask.getHeader());
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    "https://cloud.uipath.com/{cloudOrg}/{cloudTenant}/odata/Queues/UiPathODataSvc.AddQueueItem",
                    HttpMethod.POST,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant);

            // Check if the request was successful
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JSONObject responseEntityAsJson = new JSONObject(responseEntity.getBody());
                int queueItemId = responseEntityAsJson.getInt("QueueDefinitionId");
                LOGGER.info("Queue item added successfully. Queue item ID: {}", queueItemId);

                // Add the Key of the Queue Item to the process variables
                int itemId = responseEntityAsJson.getInt("Id");
                jobTask.addProcessVariable("bridge_queueItemId", itemId);

                // Complete the current job task's external task

                uiPathTaskExecutor.completeExternalTask(jobTask);

            } else {
                // Handle error if the request was unsuccessful
                String responseBody = responseEntity.getBody();
                LOGGER.error("Failed to add queue item. Response body: {}", responseBody);
                throw new RuntimeException("Failed to add queue item. Response body: " + responseBody);
            }
        } catch (JSONException e) {
            // Handle JSON parsing errors
            LOGGER.error("Failed to parse JSON", e);
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage());
        } catch (HttpClientErrorException e) {
            // Handle HTTP client errors
            LOGGER.error("Failed to make HTTP request", e);
            throw new RuntimeException("Failed to make HTTP request: " + e.getMessage());
        } catch (Exception e) {
            // Handle unknown errors
            LOGGER.error("An unknown error occurred", e);
            throw new RuntimeException("An unknown error occurred: " + e.getMessage());
        }
    }

    /**
     * Checks the status of a specific UiPath queue item.
     *
     * @param jobTask An instance of JobTask containing necessary information for
     *                the API call.
     * @throws RuntimeException if an error occurs while checking the queue item
     *                          status or processing the response.
     */
    public void checkQueueItemStatus(JobTask jobTask) {
        // Extract the queue item ID from the process variables
        int queueItemId = (int) jobTask.getProcessVariables().get("bridge_queueItemId");

        // Refresh the authentication token if it's expired
        uiPathAuthenticator.refreshTokenIfExpired();

        // Update the bearer authorization token in the job task
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        try {
            // Prepare the request entity with the headers
            HttpEntity<String> requestEntity = new HttpEntity<>(null, jobTask.getHeader());

            // Perform the API call to get the status of the queue item and receive the
            // response
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    "https://cloud.uipath.com/{cloudOrg}/{cloudTenant}/odata/QueueItems({queueItemId})/",
                    HttpMethod.GET,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant,
                    queueItemId);

            // If the API call was unsuccessful, log the error and throw an exception
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                LOGGER.error("Failed to check the status of queue item. Response body: {}", responseBody);
                throw new RuntimeException("Failed to check the status of queue item. Response body: " + responseBody);
            }

            // Parse the response body into JSON and extract the queue item status
            JSONObject responseEntityAsJson = new JSONObject(responseEntity.getBody());
            String queueItemStatus = responseEntityAsJson.getString("Status");

            // Log the queue item status
            LOGGER.info("The status of queue item ID '{}' is: {}", queueItemId, queueItemStatus);

            // Add the queue item status to the process variables
            jobTask.addProcessVariable("bridge_queueItemStatus", queueItemStatus);

            // If the queue item status is successful, extract and add output variables to
            // the process variables
            if (queueItemStatus.equals("Successful")) {
                JSONObject objectAsJson = responseEntityAsJson.getJSONObject("Output");

                for (String key : objectAsJson.keySet()) {
                    jobTask.addProcessVariable(key, objectAsJson.getString(key));
                }
            }

            // Complete the external task associated with the current job task
            uiPathTaskExecutor.completeExternalTask(jobTask);
        } catch (HttpClientErrorException e) {
            // Log and throw an exception if there's an issue with the HTTP request
            LOGGER.error("Failed to make HTTP request to check the status of queue item", e);
            throw new RuntimeException(
                    "Failed to make HTTP request to check the status of queue item: " + e.getMessage(), e);
        } catch (Exception e) {
            // Log and throw an exception for any other unknown errors
            LOGGER.error("An unknown error occurred while checking the status of queue item", e);
            throw new RuntimeException(
                    "An unknown error occurred while checking the status of queue item: " + e.getMessage(), e);
        }
    }

}