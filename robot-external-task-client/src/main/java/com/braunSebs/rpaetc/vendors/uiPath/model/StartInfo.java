package com.braunSebs.rpaetc.vendors.uiPath.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Represents the information needed to start a UiPath job.
 */
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName("startInfo")
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class StartInfo {

    private String releaseKey;
    private String strategy;
    private int jobsCount;
    private String inputArguments;

    /**
     * Constructor with parameters.
     *
     * @param releaseKey      The release key of the UiPath process.
     * @param strategy        The strategy to use for starting the job.
     * @param jobsCount       The number of jobs to start.
     * @param inputArguments  The input arguments for the UiPath process.
     */
    public StartInfo(String releaseKey, String strategy, int jobsCount, String inputArguments) {
        this.releaseKey = releaseKey;
        this.strategy = strategy;
        this.jobsCount = jobsCount;
        this.inputArguments = inputArguments;
    }

    /**
     * Default constructor.
     */
    public StartInfo() {
    }

    // Getter and setter methods.

    /**
     * Returns the release key of the UiPath process.
     *
     * @return The release key of the UiPath process.
     */
    public String getReleaseKey() {
        return releaseKey;
    }

    /**
     * Sets the release key of the UiPath process.
     *
     * @param releaseKey The release key of the UiPath process.
     */
    public void setReleaseKey(String releaseKey) {
        this.releaseKey = releaseKey;
    }

    /**
     * Returns the strategy to use for starting the job.
     *
     * @return The strategy to use for starting the job.
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Sets the strategy to use for starting the job.
     *
     * @param strategy The strategy to use for starting the job.
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the number of jobs to start.
     *
     * @return The number of jobs to start.
     */
    public int getJobsCount() {
        return jobsCount;
    }

    /**
     * Sets the number of jobs to start.
     *
     * @param jobsCount The number of jobs to start.
     */
    public void setJobsCount(int jobsCount) {
        this.jobsCount = jobsCount;
    }

    /**
     * Returns the input arguments for the UiPath process.
     *
     * @return The input arguments for the UiPath process.
     */
    public String getInputArguments() {
        return inputArguments;
    }

    /**
     * Sets the input arguments for the UiPath process.
     *
     * @param inputArguments The input arguments for the UiPath process.
     */
    public void setInputArguments(String inputArguments) {
        this.inputArguments = inputArguments;
    }

}