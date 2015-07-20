package org.apache.mesos.kibana.web;

import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.kibana.scheduler.SchedulerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequestController {
    public static final Logger logger = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    SchedulerConfiguration configuration;

    @Autowired
    SchedulerDriver schedulerDriver;

    @RequestMapping(method = RequestMethod.POST, value = "/request")
    public Request requestKibana(@RequestBody Request request) {
        logger.info("New request passed in through API for ES {}, delta {}", request.getElasticSearchUrl(), request.getDelta());
        configuration.registerRequirement(request.getElasticSearchUrl(), request.getDelta());
        return request;
    }
}
