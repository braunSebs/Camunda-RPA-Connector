package com.braunSebs.rpaetc;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.braunSebs.rpaetc.vendors.uiPath.service.UiPathTaskExecutor;

@Component
@ExternalTaskSubscription("callRpaBridge")
public class CallToRpaTaskHandler implements ExternalTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallToRpaTaskHandler.class);

    @Value("${bridge.uipath.cloud.webhook}")
    private boolean webhook;

    @Autowired
    private UiPathTaskExecutor uiPathTaskExecutor;

    /**
     * Handles the execution of an external task by processing its input arguments
     * and invoking the appropriate RPA service.
     *
     * @param externalTask        The external task to be executed.
     * @param externalTaskService Service providing operations for the external
     *                            task.
     */
    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        // Fetch all variables associated with the task
        Map<String, Object> inputArguments = new HashMap<>(externalTask.getAllVariables());

        // Log the initiation of task processing
        LOGGER.info("Starting execution of external task '{}' with vendor '{}' and job type '{}'",
                externalTask.getId(),
                inputArguments.get("bridge_vendor"),
                inputArguments.get("bridge_jobType"));

        try {
            // Extract vendor name
            String vendor = inputArguments.get("bridge_vendor").toString();

            // Execute task based on vendor
            switch (vendor) {
                case "uipath":
                    uiPathTaskExecutor.startUiPathJob(externalTaskService, externalTask, inputArguments);
                    break;
                default:
                    LOGGER.error("Unsupported vendor '{}' for external task '{}'", vendor, externalTask.getId());
                    throw new IllegalArgumentException("Unsupported vendor '" + vendor + "'");
            }
        } catch (Exception e) {
            // Handle and log errors during task execution
            String errorMessage = "Error during the execution of external task: " + e.getMessage();
            long retryDelay = 0L; // There will be no retry delay as we are not retrying the task

            // Register a failure for the external task, with no retries in case of errors
            externalTaskService.handleFailure(externalTask, errorMessage, e.getMessage(), 0, retryDelay);

            LOGGER.error("Error during the execution of external task '{}' for robot named '{}' in folder '{}': {}",
                    externalTask.getId(), externalTask.getVariable("robotName"), externalTask.getVariable("folderName"),
                    errorMessage, e);
        }
    }
}