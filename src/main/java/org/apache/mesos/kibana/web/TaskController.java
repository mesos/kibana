package org.apache.mesos.kibana.web;

import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring controller for managing tasks
 */
@RestController
@RequestMapping("/task")
public class TaskController {
    public static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    SchedulerConfiguration configuration; //Reference to the framework's scheduler's configuration

    /**
     * Handles requests, registering new requirements with the scheduler's configuration
     *
     * @param request the request to handle
     * @return the handled request
     */
    @RequestMapping(method = RequestMethod.POST, value = "/request")
    public TaskRequest request(@RequestBody TaskRequest request) {
        LOGGER.info("New request for elasticSearch {} (delta {})", request.getElasticSearchUrl(), request.getDelta());
        configuration.registerRequirement(request.getElasticSearchUrl(), request.getDelta());
        return request;
    }
}
