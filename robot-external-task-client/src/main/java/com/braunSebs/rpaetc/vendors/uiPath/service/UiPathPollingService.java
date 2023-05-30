package com.braunSebs.rpaetc.vendors.uiPath.service;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.braunSebs.rpaetc.vendors.uiPath.config.UiPathAuthenticator;
import com.braunSebs.rpaetc.vendors.uiPath.model.JobTask;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

/**
 * Service that handles UiPath polling payloads.
 */
@Service
public class UiPathPollingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathPollingService.class);

    @Autowired
    private RestTemplate uiPathDirectoryRestClient;

    @Autowired
    private UiPathAuthenticator uiPathAuthenticator;

    @Autowired
    private UiPathTaskExecutor uiPathTaskExecutor;

    @Value("${bridge.uipath.cloud.url}")
    private String cloudUrl;

    @Value("${bridge.uipath.cloud.org}")
    private String cloudOrg;

    @Value("${bridge.uipath.cloud.tenant}")
    private String cloudTenant;

    @Value("${bridge.uipath.cloud.webhook}")
    private boolean webhook;

    // Map to store registered job tasks by job ID
    private final ConcurrentMap<Integer, JobTask> registeredJobTasks = new ConcurrentHashMap<>();

    /**
     * Registers a job ID and its corresponding JobTask.
     * 
     * @param jobId   The ID of the job to register.
     * @param jobTask The JobTask object to register.
     */
    public void registerJobId(int jobId, JobTask jobTask) {
        cleanJobTask();
        registeredJobTasks.put(jobId, jobTask);
        LOGGER.debug("Job {} registered", jobId);
    }

    /**
     * Removes expired job tasks from the registered job tasks map.
     * A job task is considered expired if its creation date-time is more than 24
     * hours ago.
     */
    public void cleanJobTask() {
        Instant currentDateTime = Instant.now();
        // Set the threshold date-time to 24 hours ago
        Instant thresholdDateTime = currentDateTime.minus(Duration.ofHours(24));

        Iterator<Map.Entry<Integer, JobTask>> iterator = registeredJobTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, JobTask> entry = iterator.next();
            JobTask jobTask = entry.getValue();

            if (jobTask.getCreationDateTime().isBefore(thresholdDateTime)) {
                // If the job task is expired, remove it from the map
                iterator.remove();
                LOGGER.info("Removed expired job {} from registered job tasks", entry.getKey());
            }
        }
    }

    @PostConstruct
    public void init() {
        if (!webhook) {
            pollJobs();
        }
    }

    /**
     * Polls UiPath Orchestrator for job status updates.
     */
    @Scheduled(fixedDelay = 5000)
    public void pollJobs() {
        LOGGER.debug("Polling UiPath jobs...");
        try {
            Iterator<Map.Entry<Integer, JobTask>> iterator = registeredJobTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, JobTask> entry = iterator.next();
                int jobId = entry.getKey();

                // Retrieve the job response entity from UiPath Orchestrator.
                ResponseEntity<String> responseEntity = getJobResponseEntity(jobId, entry.getValue());
                JSONObject responseEntityJson = new JSONObject(responseEntity.getBody());
                String state = responseEntityJson.getString("State");

                // Handle the job based on its current state.
                switch (state) {
                    case "Successful":
                        handleJobSuccessful(jobId, responseEntityJson, entry.getValue());
                        iterator.remove();
                        break;

                    case "Running":
                        handleJobRunning(jobId, state);
                        break;

                    case "Pending":
                        handleJobPending(jobId, state);
                        break;

                    case "Faulted":
                        handleJobFailed(jobId, state);
                        iterator.remove();
                        break;

                    case "Canceled":
                        handleJobFailed(jobId, state);
                        iterator.remove();
                        break;

                    case "Terminated":
                        handleJobFailed(jobId, state);
                        iterator.remove();
                        break;

                    default:
                        LOGGER.warn("Unknown job state: {}", state);
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while polling UiPath jobs: {}", e.getMessage());
        }
    }

    /**
     * Returns the response entity for a given job ID.
     *
     * @param jobId The ID of the job to retrieve.
     * @return The response entity containing the job details.
     * @throws RuntimeException If an error occurs while retrieving the job details.
     */
    private ResponseEntity<String> getJobResponseEntity(int jobId, JobTask jobTask) {
        LOGGER.debug("Getting job response entity for job ID {}", jobId);
        uiPathAuthenticator.refreshTokenIfExpired();
        jobTask.setBearerAuth(uiPathAuthenticator.getAccessToken());

        try {
            // Prepare HTTP request with headers.
            HttpEntity<String> requestEntity = new HttpEntity<>(jobTask.getHeader());

            // Make the request.
            ResponseEntity<String> responseEntity = uiPathDirectoryRestClient.exchange(
                    cloudUrl + "/{cloudOrg}/{cloudTenant}/orchestrator_/odata/Jobs({jobId})",
                    HttpMethod.GET,
                    requestEntity,
                    String.class,
                    cloudOrg,
                    cloudTenant,
                    jobId);

            // Check if the request was successful
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                LOGGER.debug("Successfully retrieved job response entity for job ID {}", jobId);
                return responseEntity;
            } else {
                // Handle error if the request was unsuccessful
                String responseBody = responseEntity.getBody();
                LOGGER.error("Failed to get job response entity for job ID {}. Response body: {}", jobId, responseBody);
                registeredJobTasks.remove(jobId);
                throw new RuntimeException(
                        "Failed to get job response entity for job ID " + jobId + ". Response body: " + responseBody);
            }
        } catch (RestClientException e) {
            LOGGER.error("Error getting job response entity for job ID {}: {}", jobId, e.getMessage(), e);
            registeredJobTasks.remove(jobId);
            throw new RuntimeException("Error getting job response entity for job ID " + jobId, e);
        }
    }

    /**
     * Handles a successful job completion.
     *
     * @param jobId              the ID of the UiPath job that has completed
     *                           successfully
     * @param responseEntityJson the JSON object representing the response entity of
     *                           the job
     * @throws JSONException    if there is an error processing the JSON payload
     * @throws RuntimeException if there is an error completing the external task
     */
    private void handleJobSuccessful(int jobId, JSONObject responseEntityJson, JobTask jobTask) {
        try {
            LOGGER.info("UiPath job {} completed", jobId);
            Map<String, Object> outputArguments = new HashMap<>();
            String outputArgumentsString = responseEntityJson.getString("OutputArguments");
            // Extract output arguments from the job completion payload
            JSONObject outputArgumentsJson = new JSONObject(outputArgumentsString);
            for (String key : outputArgumentsJson.keySet()) {
                Object value = outputArgumentsJson.get(key);
                jobTask.addProcessVariable(key, value);
            }
            LOGGER.debug("Output arguments received for job {}: {}", jobId, outputArguments);

            uiPathTaskExecutor.completeExternalTask(jobTask);

            registeredJobTasks.remove(jobId);
        } catch (JSONException e) {
            LOGGER.error("Error processing job completion payload: {}", e.getMessage());
            registeredJobTasks.remove(jobId);

        } catch (RuntimeException e) {
            LOGGER.error("Error completing external task: {}", e.getMessage());
            registeredJobTasks.remove(jobId);

        }
    }

    /**
     * 
     * Handles a job that is currently running.
     * 
     * @param jobId the ID of the UiPath job that is currently running
     */
    private void handleJobRunning(int jobId, String state) {
        LOGGER.info("UiPath job {} is {}.", jobId, state);
    }

    /**
     * 
     * Handles a job that has failed.
     * 
     * @param jobId the ID of the UiPath job that has failed
     * 
     * @param state the current state of the job
     */
    private void handleJobFailed(int jobId, String state) {
        LOGGER.warn("UiPath job {} failed with state {}.", jobId, state);
        ExternalTaskService externalTaskService = registeredJobTasks.get(jobId).getExternalTaskService();
        ExternalTask externalTask = registeredJobTasks.get(jobId).getExternalTask();
        registeredJobTasks.remove(jobId);

        externalTaskService.handleBpmnError(externalTask, "uiPathJobFailed", "UiPath job failed");
    }

    /**
     * 
     * Handles a job that is currently pending.
     * 
     * @param jobId the ID of the UiPath job that is currently pending
     */
    private void handleJobPending(Integer jobId, String state) {
        LOGGER.info("UiPath job {} is {}.", jobId, state);
    }

}