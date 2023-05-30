package com.braunSebs.rpaetc.vendors.uiPath.model;

import java.time.Instant;
import java.util.Map;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.HttpHeaders;

/**
 * Represents a task to be executed in a job, carrying all the required
 * contextual information such as process variables, HTTP headers, and job
 * status.
 */
public class JobTask {

    private int jobId;
    private Map<String, Object> processVariables;
    private HttpHeaders header;
    private final ExternalTaskService externalTaskService;
    private final ExternalTask externalTask;
    private final Instant creationDateTime;

    /**
     * Constructs a new job task instance with the specified process variables.
     *
     * @param externalTaskService the external task service
     * @param externalTask        the external task
     * @param creationDateTime    the creation date and time
     * @param processVariables    the process variables
     */
    public JobTask(ExternalTaskService externalTaskService, ExternalTask externalTask,
            Instant creationDateTime, Map<String, Object> processVariables) {
        this.externalTaskService = externalTaskService;
        this.externalTask = externalTask;
        this.creationDateTime = creationDateTime;
        this.processVariables = processVariables;
        this.header = new HttpHeaders(); // Initialize the header
    }

    /**
     * Adds a process variable with the specified key and value to the job task.
     *
     * @param key   the key of the process variable
     * @param value the value of the process variable
     */
    public void addProcessVariable(String key, Object value) {
        processVariables.put(key, value);
    }

    /**
     * Sets the Bearer Authorization header using the provided access token.
     *
     * @param accessToken the access token to be used for authorization
     */
    public void setBearerAuth(String accessToken) {
        header.setBearerAuth(accessToken);
    }

    /**
     * Adds a key-value pair to the HttpHeaders.
     *
     * @param key   The key to be added to the HttpHeaders.
     * @param value The value to be associated with the key in the HttpHeaders.
     */
    public void addHeaderItem(String key, String value) {
        header.set(key, value);
    }

    /**
     * Retrieves the unique identifier of this job task.
     *
     * @return jobId, the unique identifier of this job task
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * Assigns a unique identifier to this job task.
     *
     * @param jobId the unique identifier to be assigned
     */
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    /**
     * Retrieves the external task service that manages this job task.
     *
     * @return externalTaskService, the service managing this job task
     */
    public ExternalTaskService getExternalTaskService() {
        return externalTaskService;
    }

    /**
     * Retrieves the specific external task that this job task represents.
     *
     * @return externalTask, the specific task that this job task represents
     */
    public ExternalTask getExternalTask() {
        return externalTask;
    }

    /**
     * Retrieves the timestamp indicating when this job task was created.
     *
     * @return creationDateTime, the timestamp of job task creation
     */
    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    /**
     * Retrieves the map of process variables associated with this job task.
     *
     * @return processVariables, the map of process variables associated with this
     *         job task
     */
    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    /**
     * Assigns a new map of process variables to this job task.
     *
     * @param processVariables the new map of process variables to be assigned
     */
    public void setProcessVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }

    /**
     * Retrieves the HTTP headers associated with this job task.
     * 
     * @return header, the HTTP headers associated with this job task
     */
    public HttpHeaders getHeader() {
        return header;
    }

    /**
     * Assigns a new set of HTTP headers to this job task.
     * 
     * @param header - The new set of HTTP headers to be assigned
     */
    public void setHeader(HttpHeaders header) {
        this.header = header;
    }
}