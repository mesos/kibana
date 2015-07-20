package org.apache.mesos.kibana.scheduler;

import org.apache.commons.cli.*;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Contains the configuration for a KibanaScheduler.
 * Used to manage task settings and required/running tasks.
 */
public class SchedulerConfiguration {


    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfiguration.class);
    private static final String dockerImageName = "kibana"; // the name of Kibana Docker image to use when starting a task
    private static final double requiredCpu = 0.1D;                 // the amount of CPUs a task needs
    private static final double requiredMem = 128D;                 // the amount of memory a task needs
    private static final double requiredPortCount = 1D;                 // the amount of ports a task needs
    private static final Options options = new Options() {{
        addOption("zk", "zookeeperUrl", true, "Zookeeper URL (zk://host:port/mesos)");
        addOption("es", "elasticsearchUrls", true, "ElasticSearch URLs (http://host:port;http://host:port)");
        addOption("p", "apiPort", true, "TCP port for the JSON API service (9001)");
    }};

    protected Map<String, Integer> requiredTasks = new HashMap<>();             // a map containing the required tasks: <elasticSearchUrl, numberOfInstances>
    protected Map<String, List<Protos.TaskID>> runningTasks = new HashMap<>();  // a map containing the currently running tasks: <elasticSearchUrl, listOfTaskIds>
    private Map<Protos.TaskID, Long> usedPortNumbers = new HashMap<>();               // a list containing the currently used ports, part of the Docker host ports workaround
    private String zookeeperUrl;   // the url of the Mesos zookeeper //TODO There can be multiple zookeepers..
    private String apiPort;

    /**
     * Returns the name of the Kibana Docker image
     *
     * @return the name of the Kibana Docker image
     */
    public static String getDockerImageName() {
        return dockerImageName;
    }

    /**
     * Returns the amount of requiredCpu a task needs
     *
     * @return the amount of requiredCpu a task needs
     */
    public static double getRequiredCpus() {
        return requiredCpu;
    }

    /**
     * Returns the amount of memory a task needs
     *
     * @return the amount of memory a task needs
     */
    public static double getRequiredMem() {
        return requiredMem;
    }

    /**
     * Returns the amount of ports a task needs
     *
     * @return the amount of ports a task needs
     */
    public static double getRequiredPortCount() {
        return requiredPortCount;
    }

    public static Options getOptions() {
        return options;
    }

    public String getApiPort() {
        return apiPort;
    }

    public void setApiPort(String apiPort) {
        this.apiPort = apiPort;
    }

    /**
     * Returns the address of the Mesos master
     *
     * @return the address of the Mesos master
     */
    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    /**
     * Sets the zookeeper address
     *
     * @param zookeeperUrl the address of the mesos master
     */
    public void setZookeeperUrl(String zookeeperUrl) {
        logger.info("Setting Mesos master address to {}", zookeeperUrl);
        this.zookeeperUrl = zookeeperUrl;
    }

    /**
     * Picks a port number from the given offer's port resources
     *
     * @param taskId
     * @param offer  the offer from which's resources to pick a port
     * @return a port number
     */
    public long pickAndRegisterPortNumber(Protos.TaskID taskId, Protos.Offer offer) {
        for (Protos.Resource resource : offer.getResourcesList()) {
            if (!resource.getName().equals("ports")) continue;

            List<Protos.Value.Range> offeredRanges = resource.getRanges().getRangeList();
            for (Protos.Value.Range portRange : offeredRanges) {
                long begin = portRange.getBegin();
                long end = portRange.getEnd();
                for (long port = begin; port < end; port++) {
                    if (!usedPortNumbers.values().contains(port)) {
                        usedPortNumbers.put(taskId, port);
                        return port;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Increases the required number of instances for the given elasticSearchUrl by the given amount.
     * If the resulting amount of required instances is equal to or lower than 0, the elasticSearchUrl entry is removed from the requiredTasks.
     *
     * @param elasticSearchUrl the elasticSearchUrl to change the required amount of instances for
     * @param amount           the amount by which to change the required amount of instances
     */
    public void registerRequirement(String elasticSearchUrl, int amount) {
        if (requiredTasks.containsKey(elasticSearchUrl)) {
            int newAmount = amount + requiredTasks.get(elasticSearchUrl).intValue();
            if (newAmount <= 0) {
                requiredTasks.remove(elasticSearchUrl);
                logger.info("RequiredInstances: No more instances are required for ElasticSearch {}", elasticSearchUrl);
            } else {
                requiredTasks.put(elasticSearchUrl, newAmount);
                logger.info("RequiredInstances: Now requiring {} instances for ElasticSearch {}", newAmount, elasticSearchUrl);
            }
        } else if (amount > 0) {
            requiredTasks.put(elasticSearchUrl, amount);
            logger.info("RequiredInstances: Now requiring {} instances for ElasticSearch {}", amount, elasticSearchUrl);
        }
    }

    /**
     * Returns a Map with all known elasticSearchUrls and the delta between the required and running number of instances.
     *
     * @return a Map with all known elasticSearchUrls and the delta between the required and running number of instances
     */
    public Map<String, Integer> getRequirementDeltaMap() {
        Set<String> elasticSearchUrls = new HashSet<>();
        elasticSearchUrls.addAll(requiredTasks.keySet());
        elasticSearchUrls.addAll(runningTasks.keySet());

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
        if (requiredTasks.containsKey(elasticSearchUrl)) {
            int requiredAmount = requiredTasks.get(elasticSearchUrl);
            if (runningTasks.containsKey(elasticSearchUrl)) {
                int actualAmount = runningTasks.get(elasticSearchUrl).size();
                return requiredAmount - actualAmount;
            }
            return requiredAmount;
        }

        if (runningTasks.containsKey(elasticSearchUrl)) {
            int actualAmount = runningTasks.get(elasticSearchUrl).size();
            return -actualAmount;
        }
        return 0;
    }

    /**
     * Adds the given task to the currently running tasks, under the given elasticSearchUrl
     *
     * @param elasticSearchUrl the elasticSearchUrl under which to add the given task
     * @param taskId           the task to add
     */
    public void registerTask(String elasticSearchUrl, Protos.TaskID taskId) {
        if (runningTasks.containsKey(elasticSearchUrl)) {
            runningTasks.get(elasticSearchUrl).add(taskId);
        } else {
            ArrayList<Protos.TaskID> instances = new ArrayList<>();
            instances.add(taskId);
            runningTasks.put(elasticSearchUrl, instances);
        }
        logger.info("Now running task {} for ElasticSearch{}", taskId.getValue(), elasticSearchUrl);
    }

    /**
     * Unregisters the given task and its ports
     *
     * @param taskId the task to unregister
     */
    public void unregisterTask(Protos.TaskID taskId) {
        for (List<Protos.TaskID> tasks : runningTasks.values()) {
            if (tasks.contains(taskId)) {
                tasks.remove(taskId);
                usedPortNumbers.remove(taskId);
                logger.info("Removed task {} for ElasticSearch{}", taskId.getValue());
                return;
            }
        }
    }

    /**
     * Handles any passed in arguments
     *
     * @param args zookeeper address, followed by elasticSearchUrls
     */
    public void parseLaunchArguments(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        String port = commandLine.getOptionValue("apiPort", "9001");
        setApiPort(port);

        String zkUrl = commandLine.getOptionValue("zk");
        if (zkUrl != null) {
            setZookeeperUrl(zkUrl);
        } else {
            throw new MissingArgumentException("Zookeeper URL is required.");
        }

        String esUrls = commandLine.getOptionValue("es");
        if (esUrls != null) {
            for (String esUrl : esUrls.split(";")) {
                registerRequirement(esUrl, 1);
            }

        }
    }

    public Protos.TaskID getYoungestTask(String elasticSearchUrl) {
        if (runningTasks.containsKey(elasticSearchUrl)) {
            List<Protos.TaskID> tasks = runningTasks.get(elasticSearchUrl);
            return tasks.get(tasks.size() - 1);
        }
        return null;
    }
}