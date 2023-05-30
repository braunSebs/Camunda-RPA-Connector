package com.braunSebs.rpaetc.vendors.uiPath.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.braunSebs.rpaetc.vendors.uiPath.model.JobTask;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service that handles UiPath webhook payloads.
 */
@Service
public class UiPathWebhookService {
    @Autowired
    private UiPathTaskExecutor uiPathTaskExecutor;

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathWebhookService.class);

    // Map to store registered job tasks by job ID
    private final ConcurrentMap<Integer, JobTask> registeredJobTasks = new ConcurrentHashMap<>();

    /**
     * Registers a job ID and its corresponding jobTask.
     *
     * @param jobId   the job ID to register
     * @param jobTask the job task to associate with the job ID
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

    /**
     * Handles a UiPath webhook payload.
     *
     * @param payload the payload to handle
     * @throws JSONException if the payload is not a valid JSON object
     */
    public void handlePayload(String payload) throws JSONException {
        LOGGER.debug("Received payload: {}", payload);

        JSONObject payloadAsJson;

        try {
            payloadAsJson = new JSONObject(payload);
        } catch (JSONException e) {
            LOGGER.error("Payload is not a valid JSON object: {}", e.getMessage());
            throw e;
        }

        String type = payloadAsJson.getString("Type");

        if (payloadAsJson.isEmpty()) {
            LOGGER.warn("Received empty payload");
            return;
        }

        if (type.equals("job.created")) {
            JSONArray jobsArray = payloadAsJson.getJSONArray("Jobs");

            for (int i = 0; i < jobsArray.length(); i++) {
                int jobId = jobsArray.getJSONObject(i).getInt("Id");

                try {
                    if (isValidJobId(jobId)) {
                        handleJobCreated(jobId);
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid jobId: {}", e.getMessage());
                    throw e;
                }
            }
        } else {
            int jobId = payloadAsJson.getJSONObject("Job").getInt("Id");

            try {
                if (isValidJobId(jobId)) {
                    switch (type) {
                        case "job.started":
                            handleJobStarted(jobId);
                            break;

                        case "job.faulted":
                        case "job.stopped":
                        case "job.suspended":
                            handleJobFailed(jobId, type);
                            break;

                        case "job.completed":
                            handleJobCompleted(payloadAsJson, jobId, registeredJobTasks.get(jobId));
                            break;

                        default:
                            LOGGER.error("Unknown payload type: {}", type);
                            break;
                    }
                } else {
                    LOGGER.warn("Payload received for unregistered jobId: {}", jobId);
                }
            } catch (IllegalArgumentException e) {
                registeredJobTasks.remove(jobId);
                LOGGER.error("Invalid jobId: {}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Handles a job failure.
     *
     * @param jobId the ID of the failed job
     * @param type  the type of failure
     */
    private void handleJobFailed(int jobId, String type) {
        LOGGER.error("Job {} failed with status {}", jobId, type);
        registeredJobTasks.remove(jobId);
        throw new RuntimeException("UiPath job failed: " + type);
    }

    /**
     * Checks if a job ID is valid.
     *
     * @param jobId the job ID to check
     * @return true if the job ID is valid, false otherwise
     */
    private boolean isValidJobId(int jobId) {
        return registeredJobTasks.containsKey(jobId);
    }

    /**
     * Handles a job creation event.
     *
     * @param jobId the ID of the created job
     */
    private void handleJobCreated(int jobId) {
        LOGGER.info("UiPath job {} is created", jobId);
    }

    /**
     * Handles a job start event.
     *
     * @param jobId the ID of the created job
     */
    private void handleJobStarted(int jobId) {
        LOGGER.info("UiPath job {} is started", jobId);
    }

    /**
     * Handles a job completion event.
     *
     * @param payloadAsJson the job completion payload as a JSONObject
     * @param jobId         the ID of the completed job
     * @throws JSONException    if there is an error processing the JSON payload
     * @throws RuntimeException if there is an error completing the external task
     */
    private void handleJobCompleted(JSONObject payloadAsJson, int jobId, JobTask jobTask)
            throws JSONException, RuntimeException {
        LOGGER.info("UiPath job {} completed", jobId);
        try {
            // Extract output arguments from the job completion payload
            JSONObject jobObject = payloadAsJson.getJSONObject("Job");
            Map<String, Object> outputArguments = new HashMap<>();
            JSONObject outputArgumentsJson = jobObject.getJSONObject("OutputArguments");
            for (String key : outputArgumentsJson.keySet()) {
                Object value = outputArgumentsJson.get(key);
                jobTask.addProcessVariable(key, value);
                outputArguments.put(key, value);
            }

            LOGGER.debug("Output arguments for jobId {}: {}", jobId, outputArguments);

            // Complete the external task with the output arguments from the completed job

            uiPathTaskExecutor.completeExternalTask(jobTask);

            registeredJobTasks.remove(jobId);
        } catch (JSONException e) {
            LOGGER.error("Error processing job completion payload: {}", e.getMessage());
            registeredJobTasks.remove(jobId);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("Error completing external task: {}", e.getMessage());
            registeredJobTasks.remove(jobId);
            throw e;
        }
    }

}