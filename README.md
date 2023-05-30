# Camunda RPA External Task Client
This project provides a functional Spring Boot application, which makes an External Task Client available that can delegate tasks to an RPA bot. With this External Client, the following actions can be triggered in UiPath:

* Start a robot
* Create a queue item
* Check the status of a queue item

This project is a proof of concept (POC) that demonstrates specific functionalities and capabilities. To provide a detailed explanation of the project and its functions, here is a video that explains the features (youtube.com).

The POC project showcases the core ideas and feasibility of the proposed solution. It serves as a prototype to validate the concept and gather feedback before proceeding with full-scale development.

The BPMN model for the POC can be found in the repository. It represents the visual representation of the business process implemented in the POC.

## Getting Started
To use the RPA External Task Client, follow these steps:
1. Clone the repository to your local machine.
2. Import the project into your preferred IDE.
3. Setup the application.properties and the uiPath.properties
4. Build the project using Maven.
5. Run the project using the RobotExternalTaskClientApplication class

## How to configure your application
To configure the application, there are two properties files. The application.properties is used for your Springboot application and Camunda settings, while the UiPath.properties is used for the UiPath specific configurations. 

### application.properties

This file contains the configurations for your Springboot application and for Camunda. These are the configurations I recommend. You can find more information about the configurations for your Springboot application here (https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) and for Camunda here (https://docs.camunda.org/manual/latest/user-guide/spring-boot-integration/configuration/#camunda-engine-properties).

* server.port=8081: This value is the server port for your Tomcat server. The application is accessible under this port. The default value is 8081. This can be adjusted as needed.

* logging.level.org.springframework=INFO: This value sets the logging level of the Springboot application. For more information on the logging values, visit (https://www.baeldung.com/spring-boot-logging).

* logging.level.org.apache=INFO: This value sets the logging level for the Apache server. The default is set to INFO. This logging level is suitable for operation. For more information on logging, visit (https://logging.apache.org/log4j/2.x/manual/customloglevels.html).

* logging.logback.rollingpolicy.max-file-size=100KB: Maximum size of the logging file. The default is 100KB. This value can be adjusted as needed.

* logging.logback.rollingpolicy.max-history=0: The maximum number of archived log files. The default setting is 0. This value can be adjusted as needed.

* debug=false: Enable or disable the debug mode. The default value is false.

* spring.banner.location=classpath:banner.txt: Specify the path to the Spring Boot banner that is displayed when the application starts.

* camunda.bpm.client.base-url=http://localhost:8080/engine-rest: This is the URL to your Camunda process application. By default, a locally hosted Camunda application with port 8080 is specified. You need to adjust this value to the URL where your Camunda process application is hosted.

* camunda.bpm.client.lock-duration=3600000; This value specifies the duration for which a task is locked by the application. If the task is not completed within this time, it will be released for fetching again. Please note that a UiPath robot may have longer processing time.

* camunda.bpm.client.max-tasks=3: This value represents the maximum number of tasks that the worker can fetch at a time. By default, it is set to 3, but you can adjust this value as needed.

* camunda.bpm.client.worker-id=robot-external-task-client-1: This is the unique identifier that identifies your worker. In Camunda, this identifier is used to track which worker executed a specific task, allowing for traceability and monitoring.

* logging.level.org.camunda.bpm.client=DEBUG: The logging.level.org.camunda.bpm.client property determines the logging level for your Camunda BPMN client. By setting the value to DEBUG, you enable detailed logging for the client.

* spring.config.import=classpath:uiPath.properties: This value refers to the uiPath.properties file. It is a configuration file for UiPath, and typically, it cannot be modified directly.

## uiPath.properties
This file is used to create configurations specific to UiPath.

bridge.uipath.cloud.url=https://cloud.uipath.com: This is the URL to the Cloud Orchestrator. In most cases, this URL does not need to be changed.

* bridge.uipath.cloud.org=YourOrganisation: Here, you need to enter your UiPath organization. The name can be found in the settings of the Orchestrator.

* bridge.uipath.cloud.tenant=DefaultTenant: Here, you need to specify the tenant you want to connect to. If you haven't explicitly configured a tenant in the Orchestrator, you can use the value "DefaultTenant".

* bridge.uipath.app-id=YourAppId: To send tasks from Camunda to the Orchestrator, you need to create an external application in the Orchestrator. The generated App ID needs to be configured here. Refer to the UiPath documentation for managing external applications (https://docs.uipath.com/de/automation-cloud/automation-cloud/latest/admin-guide/managing-external-applications).

* bridge.uipath.secret-app-key=YourappKey: Here, you need to enter the generated key for the external application created in the Orchestrator.

* bridge.uipath.cloud.access-token-url=https://cloud.uipath.com/identity_/connect/token: This is the URL where the application resolves an access token. The default value can be used for this.

* bridge.uipath.cloud.webhook=true: If you want to receive the response from the Orchestrator via a webhook, you can leave this value as "true". If the value is set to "false", polling for the response will be done every 5 seconds. Refer to the UiPath documentation for managing webhooks (https://docs.uipath.com/de/orchestrator/standalone/2023.4/user-guide/managing-webhooks)

bridge.uipath.cloud.webhook-secret=YourWebhookSecret: If you have set the webhook to "true," you need to enter the webhook secret that you specified in the Orchestrator. The webhook secret is required to receive the webhook successfully.

## How to configure your External Task in Camunda Modeler

This section demonstrates how you can configure the task in the Camunda Modeler so that the External Task Client can delegate a task to the UiPath Orchestrator. There are three possible operations: start a robot synchronously, create a Queue Item, and check the status of a Queue Item.

### Start a robot
If you want to start a robot synchronously, these are the requirements for the task in Camunda. During execution, all process variables are passed to the UiPath Orchestrator. To use these, you can define them as input variables for your bot

    <bpmn:serviceTask id="StartRobotTask" name="Start Robot" camunda:type="external" camunda:topic="YourTopic">
      <bpmn:extensionElements>
        <camunda:inputOutput>
          <camunda:inputParameter name="bridge_vendor">uipath</camunda:inputParameter>
          <camunda:inputParameter name="bridge_jobType">startRobot</camunda:inputParameter>
          <camunda:inputParameter name="bridge_folderName">YourFolderName</camunda:inputParameter>
          <camunda:inputParameter name="bridge_robotName">YourRobotName</camunda:inputParameter>
        </camunda:inputOutput>
      </bpmn:extensionElements>
    </bpmn:serviceTask>

* bridge_vendor: Currently, UiPath is the only provider. You can leave the default value "uipath".

* bridge_jobType: This is the instruction for the task that the application should perform. If you want to start a robot, you should leave the default value "startRobot".

* bridge_folderName: Here, you can enter the folder name in which your robot is deployed. The application takes care of the folder ID.

* bridge_robotName: Here, you can enter the name of the robot as it was specified in the UiPath Orchestrator. The application takes care of the release key.


### Add a queue item
For asynchronous processing, you can use the application to create a queue item. These are the requirements to create a queue item. Once a queue item has been created, the ID of the entry is carried along as a variable. In order to track the status of the queue item in the later course of the process, the ID has been specified as an output parameter in this example.

    <bpmn:serviceTask id="AdQueueTask" name="Add Queue Item" camunda:type="external" camunda:topic="YourTopic">
      <bpmn:extensionElements>
        <camunda:inputOutput>
          <camunda:inputParameter name="bridge_vendor">uipath</camunda:inputParameter>
          <camunda:inputParameter name="bridge_jobType">addQueueItem</camunda:inputParameter>
          <camunda:inputParameter name="bridge_folderName">YourFolderName</camunda:inputParameter>
          <camunda:inputParameter name="bridge_queueName">YourQueueName</camunda:inputParameter>       
          <camunda:outputParameter name="bridge_queueItemId">${bridge_queueItemId}</camunda:outputParameter>
        </camunda:inputOutput>
      </bpmn:extensionElements>
    </bpmn:serviceTask>

* bridge_vendor: Currently, UiPath is the only provider. You can leave the default value "uipath".

* bridge_jobType: This is the instruction for the task that the application should perform. If you want to add a queue item, you should leave the default value "addQueueItem".

* bridge_folderName: Here, you can enter the folder name in which your queue is deployed. The application takes care of the folder ID.

* bridge_queueItemId: This variable is returned by the application.

### Check queue item Status

To check the status of a queue item, this task can be created. In this example, the ID from the previous task is passed on to the application. To allow the status to be processed in the further course of the process, it is specified as an output parameter in this example. You can find the various statuses in the UiPath documentation (https://docs.uipath.com/orchestrator/standalone/2023.4/user-guide/queue-item-statuses).

    <bpmn:serviceTask id="CheckQueueItemStatusTask" name="Check Queue Item Status" camunda:type="external" camunda:topic="YourTopic">
      <bpmn:extensionElements>
        <camunda:inputOutput>
          <camunda:inputParameter name="bridge_vendor">uipath</camunda:inputParameter>
          <camunda:inputParameter name="bridge_jobType">checkQueueItemStatus</camunda:inputParameter>       
          <camunda:inputParameter name="bridge_folderName">YourFolderName</camunda:inputParameter>
          <camunda:inputParameter name="bridge_queueItemId">${bridge_queueItemId}</camunda:inputParameter>
          <camunda:outputParameter name="bridge_queueItemStatus">${bridge_queueItemStatus}</camunda:outputParameter>
        </camunda:inputOutput>
      </bpmn:extensionElements>
    </bpmn:serviceTask>

* bridge_vendor: Currently, UiPath is the only provider. You can leave the default value "uipath".

* bridge_jobType: This is the instruction for the task that the application should perform. If you want to check a queue item status, you should leave the default value "checkQueueItemStatus".

* bridge_folderName: Here, you can enter the folder name in which your queue is deployed. The application takes care of the folder ID.

* bridge_queueItemId: The ID of the preceding queue item. Alternatively, the ID of another queue entry can also be stored.

* bridge_queueItemStatus: The status that was queried is returned by the application.

## Developer instrurctions
This is a Camunda external task that allows you to perform actions in UiPath. The application is designed to be extensible, enabling the addition of more providers. Feel free to clone the project and customize it to fit your needs. Here is a video providing a tour of the code (youtube.com).

### Found a Bug? 
If you have come across a bug or issue, please leave a comment in the "Issues" section and help me make the project better.
