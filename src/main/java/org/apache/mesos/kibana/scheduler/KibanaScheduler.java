package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos.*;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Scheduler for the KibanaFramework, in charge of starting tasks on the Mesos Slaves to fulfill requiredTasks.
 */
public class KibanaScheduler implements Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(KibanaScheduler.class);
    private SchedulerConfiguration configuration; // contains the scheduler's settings and tasks

    /**
     * Constructor for KibanaScheduler
     *
     * @param configuration the SchedulerConfiguration to use
     */
    public KibanaScheduler(SchedulerConfiguration configuration) {
        logger.info("constructing " + KibanaScheduler.class.getName());
        this.configuration = configuration;
    }

    /**
     * Checks whether the given offer has the tasks' required resources
     *
     * @param offer the offer whose resources to check
     * @return a boolean representing whether or not the offer has all the required resources
     */
    private boolean offerIsAcceptable(Offer offer) {
        boolean hasCpus = false;
        double offerCpus = 0;
        boolean hasMem = false;
        double offerMem = 0;
        boolean hasPorts = false;
        int offerPorts = 0;

        for (Resource resource : offer.getResourcesList()) {
            switch (resource.getName()) {
                case "cpus":
                    offerCpus = resource.getScalar().getValue();
                    hasCpus = true;
                    break;
                case "mem":
                    offerMem = resource.getScalar().getValue();
                    hasMem = true;
                    break;
                case "ports":
                    offerPorts = resource.getRanges().getRangeCount();
                    hasPorts = true;
            }
        }
        if (!hasCpus) {
            logger.info("Rejecting offer {} due to lack of cpus (required {}, got {})", offer.getId().getValue(), SchedulerConfiguration.getRequiredCpus(), offerCpus);
            return false;
        }
        if (!hasMem) {
            logger.info("Rejecting offer {} due to lack of mem (required {}, got {})", offer.getId().getValue(), SchedulerConfiguration.getRequiredMem(), offerMem);
            return false;
        }
        if (!hasPorts) {
            logger.info("Rejecting offer {} due to lack of ports (required {}, got {})", offer.getId().getValue(), SchedulerConfiguration.getRequiredPortCount(), offerPorts);
            return false;
        }

        logger.info("Accepting offer {} with {} cpus, {} memory and {} ports", offer.getId().getValue(), offerCpus, offerMem, offerPorts);
        return true;
    }

    /**
     * Launches a new Kibana task for the given elasticSearchUrl, using the offer
     *
     * @param requirement the requirement for the task
     * @param offer       the offer used to run the task
     * @param driver      the driver used to launch the task
     */
    private void launchNewTask(Map.Entry<String, Integer> requirement, Offer offer, SchedulerDriver driver) {
        TaskInfo task = TaskInfoFactory.buildTask(requirement, offer, configuration);
        configuration.registerTask(requirement.getKey(), task.getTaskId());

        Filters filters = Filters.newBuilder().setRefuseSeconds(1).build();
        driver.launchTasks(Arrays.asList(offer.getId()), Arrays.asList(task), filters);
    }


    @Override
    public void registered(SchedulerDriver schedulerDriver, FrameworkID frameworkID, MasterInfo masterInfo) {
        logger.info("registered() master={}:{}, framework={}", masterInfo.getIp(), masterInfo.getPort(), frameworkID);
    }

    @Override
    public void reregistered(SchedulerDriver schedulerDriver, MasterInfo masterInfo) {
        logger.info("reregistered()");
    }

    @Override
    public void resourceOffers(SchedulerDriver schedulerDriver, List<Offer> offers) {
        logger.info("resourceOffers() with {} offers", offers.size());

        List<Offer> acceptableOffers = new ArrayList<>();
        for (Offer offer : offers) {
            if (offerIsAcceptable(offer))
                acceptableOffers.add(offer);
            else
                schedulerDriver.declineOffer(offer.getId());
        }
        if (acceptableOffers.isEmpty()) return;

        for (Map.Entry<String, Integer> requirement : configuration.getRequirementDeltaMap().entrySet()) {
            int delta = requirement.getValue();
            if (delta > 0) {
                logger.info("ElasticSearch {} is missing {} tasks. Attempting to start tasks.", requirement.getKey(), delta);
                while (delta > 0) {
                    if (acceptableOffers.isEmpty()) return;
                    Offer pickedOffer = acceptableOffers.get(0);
                    launchNewTask(requirement, pickedOffer, schedulerDriver);
                    acceptableOffers.remove(pickedOffer);
                    delta--;
                }
            } else if (delta < 0) {
                logger.info("ElasticSearch {} has an excess of {} tasks. Killing excess tasks.", requirement.getKey(), delta);
                while (delta < 0) {
                    TaskID excessTask = configuration.getYoungestTask(requirement.getKey());
                    if (excessTask != null) {
                        configuration.unregisterTask(excessTask);
                        schedulerDriver.killTask(excessTask);
                        logger.info("Killed task {}.", excessTask.getValue());
                        delta++;
                    } else {
                        break;
                    }

                }
            }
        }

        for (Offer remainingOffer : acceptableOffers) {
            schedulerDriver.declineOffer(remainingOffer.getId());
        }
    }

    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, OfferID offerID) {
        logger.info("offerRescinded()");
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, TaskStatus taskStatus) {
        TaskID taskId = taskStatus.getTaskId();
        logger.info("statusUpdate() task {} is in state {}", taskId.getValue(), taskStatus.getState());

        switch (taskStatus.getState()) {
            case TASK_FAILED:
            case TASK_FINISHED:
                logger.info("Removing task {} due to state: {}", taskId.getValue(), taskStatus.getState());
                configuration.unregisterTask(taskId);
                break;
        }
    }

    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver, ExecutorID executorID, SlaveID slaveID, byte[] bytes) {
        logger.info("frameworkMessage()");
    }

    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        logger.info("disconnected()");
    }

    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, SlaveID slaveID) {
        logger.info("slaveLost()");
    }

    @Override
    public void executorLost(SchedulerDriver schedulerDriver, ExecutorID executorID, SlaveID slaveID, int i) {
        logger.info("executorLost()");
    }

    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        logger.error("error() {}", s);
    }
}