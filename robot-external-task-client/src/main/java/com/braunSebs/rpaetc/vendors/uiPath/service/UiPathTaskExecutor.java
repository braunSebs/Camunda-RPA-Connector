package com.braunSebs.rpaetc.vendors.uiPath.service;

import java.time.Instant;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.braunSebs.rpaetc.vendors.uiPath.config.UiPathAuthenticator;
import com.braunSebs.rpaetc.vendors.uiPath.model.JobTask;

@Component
public class UiPathTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathTaskExecutor.class);

    @Autowired
    private UiPathAuthenticator uiPathAuthenticator;

    @Autowired
    private UiPathApiService uiPathApiService;

    @Autowired
    private UiPathWebhookService uiPathWebhookService;

    @Autowired
    private UiPathPollingService uiPathPollingService;

    @Value("${bridge.uipath.cloud.webhook}")
    private boolean webhook;

    /**
     * Starts a UiPath job by creating a JobTask and setting up the necessary
     * parameters.
     *
     * @param externalTaskService Camunda service for external tasks.
     * @param externalTask        The external task to execute.
     * @param inputArguments      The input arguments for the job.
     */
    public void startUiPathJob(ExternalTaskService externalTaskService, ExternalTask externalTask,
            Map<String, Object> inputArguments) {

        JobTask jobTask = new JobTask(externalTaskService, externalTask, Instant.now(), inputArguments);

        uiPathAuthenticator.refreshTokenIfExpired();
        jobTask.setHeader(uiPathAuthenticator.getHeaders());

        int folderId = uiPathApiService
                .getFolderByName(jobTask.getProcessVariables().get("bridge_folderName").toString(), jobTask);
        jobTask.addHeaderItem("X-UIPATH-OrganizationUnitId", String.valueOf(folderId));

        String jobType = jobTask.getProcessVariables().get("bridge_jobType").toString();

        switch (jobType) {
            case "startRobot":
                startRobot(jobTask);
                break;
            case "addQueueItem":
                addQueueItem(inputArguments, jobTask);
                break;
            case "checkQueueItemStatus":
                checkQueueItemStatus(jobTask);
                break;
            default:
                LOGGER.error("Unsupported job type '{}' for external task '{}'", jobType, externalTask.getId());
                throw new IllegalArgumentException("Unsupported job type '" + jobType + "'");
        }
    }

    /**
     * Starts a UiPath robot with the given job task.
     *
     * @param jobTask The JobTask object associated with the external task.
     */
    public void startRobot(JobTask jobTask) {

        String robotName = jobTask.getProcessVariables().get("bridge_robotName").toString();
        String releaseKey = uiPathApiService.getReleaseKeyByName(robotName, jobTask);

        int jobId = uiPathApiService.startRobot(releaseKey, jobTask);
        jobTask.setJobId(jobId);

        if (webhook) {
            uiPathWebhookService.registerJobId(jobId, jobTask);
        } else {
            uiPathPollingService.registerJobId(jobId, jobTask);
        }

        LOGGER.info("Successfully started robot for external task '{}' with job ID '{}' and robot name '{}'",
                jobTask.getExternalTask().getId(),
                jobId,
                robotName);
    }

    /**
     * Adds a new queue item to the UiPath Orchestrator with the specified input
     * arguments and job task.
     *
     * @param inputArguments a map of input arguments for the queue
     * @param jobTask        the job task to be associated with the queue
     * 
     */
    public void addQueueItem(Map<String, Object> inputArguments, JobTask jobTask) {
        uiPathAuthenticator.refreshTokenIfExpired();

        // Log the action
        LOGGER.debug("Adding new queue item for external task '{}'", jobTask.getExternalTask().getId());

        // Add the queue Item to the UiPath Orchestrator
        uiPathApiService.addQueueItem(jobTask);

        LOGGER.info("Queue item successfully added for external task '{}'", jobTask.getExternalTask().getId());
    }

    /**
     * Checks the status of a queue item in the UiPath Orchestrator associated with
     * the job task.
     *
     * @param jobTask The job task associated with the queue item.
     */
    public void checkQueueItemStatus(JobTask jobTask) {
        uiPathAuthenticator.refreshTokenIfExpired();

        // Log the action
        LOGGER.debug("Checking status of queue item for external task '{}'", jobTask.getExternalTask().getId());

        // Check the Status of the queue Item at the UiPath Orchestrator
        uiPathApiService.checkQueueItemStatus(jobTask);
    }

    /**
     * Completes an external task in the Camunda workflow engine.
     *
     * @param jobTask The job task associated with the external task.
     */
    public void completeExternalTask(JobTask jobTask) {
        ExternalTaskService externalTaskService = jobTask.getExternalTaskService();
        ExternalTask externalTask = jobTask.getExternalTask();

        // Log the action
        LOGGER.debug("Completing external task with ID '{}'", externalTask.getId());

        externalTaskService.complete(externalTask, jobTask.getProcessVariables());

        LOGGER.info("External task '{}' completed successfully", externalTask.getId());
    }

}