package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains the configuration for a KibanaScheduler.
 * Used to manage task settings and required/running tasks.
 */
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static final String dockerImageName = "kibana"; // the name of Kibana Docker image to use when starting a task
    private static final double CPU = 0.1D;                 // the amount of CPUs a task needs
    private static final double MEM = 128D;                 // the amount of memory a task needs
    private static final double PORTS = 1D;                 // the amount of ports a task needs
    protected Map<String, Integer> requiredInstances = new HashMap<>();             // a map containing the required instances: <elasticSearchUrl, numberOfInstances>
    protected Map<String, List<Protos.TaskID>> runningInstances = new HashMap<>();  // a map containing the currently running instances: <elasticSearchUrl, listOfTaskIds>
    private String masterAddress;                           // the address of the Mesos master

    /**
     * Returns the name of the Kibana Docker image
     *
     * @return the name of the Kibana Docker image
     */
    public static String getDockerImageName() {
        return dockerImageName;
    }

    /**
     * Returns the amount of CPU a task needs
     *
     * @return the amount of CPU a task needs
     */
    public static double getCPU() {
        return CPU;
    }

    /**
     * Returns the amount of memory a task needs
     *
     * @return the amount of memory a task needs
     */
    public static double getMEM() {
        return MEM;
    }

    /**
     * Returns the amount of ports a task needs
     *
     * @return the amount of ports a task needs
     */
    public static double getPORTS() {
        return PORTS;
    }

    /**
     * Returns the address of the Mesos master
     *
     * @return the address of the Mesos master
     */
    public String getMasterAddress() {
        return masterAddress;
    }

    /**
     * Sets the address of the Mesos master
     *
     * @param masterAddress the address of the mesos master
     */
    public void setMasterAddress(String masterAddress) {
        logger.info("Setting Mesos master address to {}", masterAddress);
        this.masterAddress = masterAddress;
    }

    /**
     * Increases the required number of instances for the given elasticSearchUrl by the given amount.
     * If the resulting amount of required instances is equal to or lower than 0, the elasticSearchUrl entry is removed from the requirements.
     *
     * @param elasticSearchUrl the elasticSearchUrl to change the required amount of instances for
     * @param amount           the amount by which to change the required amount of instances
     */
    public void putRequiredInstances(String elasticSearchUrl, int amount) {
        if (requiredInstances.containsKey(elasticSearchUrl)) {
            int newAmount = amount + requiredInstances.get(elasticSearchUrl).intValue();
            if (newAmount <= 0) {
                requiredInstances.remove(elasticSearchUrl);
                logger.info("RequiredInstances: No more instances are required for ElasticSearch {}", elasticSearchUrl);
            } else {
                requiredInstances.put(elasticSearchUrl, newAmount);
                logger.info("RequiredInstances: Now requiring {} instances for ElasticSearch {}", newAmount, elasticSearchUrl);
            }
        } else if (amount > 0) {
            requiredInstances.put(elasticSearchUrl, amount);
            logger.info("RequiredInstances: Now requiring {} instances for ElasticSearch {}", amount, elasticSearchUrl);
        }
    }

    /**
     * Returns a Map with all known elasticSearchUrls and the delta between the required and running number of instances.
     *
     * @return a Map with all known elasticSearchUrls and the delta between the required and running number of instances
     */
    public Map<String, Integer> getRequirementDeltaMap() {
        //TODO Find a nicer way to merge these lists
        List<String> elasticSearchUrls = new ArrayList<>();
        elasticSearchUrls.addAll(requiredInstances.keySet());
        elasticSearchUrls.removeAll(runningInstances.keySet());
        elasticSearchUrls.addAll(runningInstances.keySet());

        Map<String, Integer> requirementDeltaMap = new HashMap<>();
        for (String elasticSearchUrl : elasticSearchUrls) {
            requirementDeltaMap.put(elasticSearchUrl, getRequirementDelta(elasticSearchUrl));
        }
        return requirementDeltaMap;
    }


    /**
     * Calculates the delta between the required amount and the running amount of instances for the given elasticSearchUrl
     *
     * @param elasticSearchUrl the elasticSearchUrl to calculate the delta for
     * @return the delta between the required amount and the running amount of instances for the given elasticSearchUrl
     */
    private int getRequirementDelta(String elasticSearchUrl) {
        if (requiredInstances.containsKey(elasticSearchUrl)) {
            int requiredAmount = requiredInstances.get(elasticSearchUrl);
            if (runningInstances.containsKey(elasticSearchUrl)) {
                int actualAmount = runningInstances.get(elasticSearchUrl).size();
                return requiredAmount - actualAmount;
            }
            return requiredAmount;
        }

        if (runningInstances.containsKey(elasticSearchUrl)) {
            int actualAmount = runningInstances.get(elasticSearchUrl).size();
            return -actualAmount;
        }
        return 0;
    }

    /**
     * Handles any passed in arguments
     *
     * @param args the master hostname/port followed by elasticSearchUrls
     */
    public void handleArguments(String[] args) {
        setMasterAddress(args[0]);
        for (int i = 1; i < args.length; i++) {
            String elasticSearchUrl = args[i];
            putRequiredInstances(elasticSearchUrl, 1);
        }
    }

    /**
     * Adds the given task to the currently running tasks, under the given elasticSearchUrl
     *
     * @param elasticSearchUrl the elasticSearchUrl under which to add the given task
     * @param taskId           the task to add
     */
    public void putRunningInstances(String elasticSearchUrl, Protos.TaskID taskId) {
        if (runningInstances.containsKey(elasticSearchUrl)) {
            runningInstances.get(elasticSearchUrl).add(taskId);
        } else {
            ArrayList<Protos.TaskID> instances = new ArrayList<>();
            instances.add(taskId);
            runningInstances.put(elasticSearchUrl, instances);
        }
        logger.info("Now running task {} for ElasticSearch{}", taskId.getValue(), elasticSearchUrl);
    }

    /**
     * Removes the given task from the currently running tasks.
     *
     * @param taskId the task to remove
     */
    public void removeRunningTask(Protos.TaskID taskId) {
        for (List<Protos.TaskID> tasks : runningInstances.values()) {
            if (tasks.contains(taskId)) {
                tasks.remove(taskId);
                logger.info("Removed task {} for ElasticSearch{}", taskId.getValue());
                return;
            }
        }
    }
}