package org.apache.mesos.kibana.scheduler;

import org.apache.mesos.Protos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    private static final String dockerImageName = "kibana";
    private static final double CPU = 0.1D;
    private static final double MEM = 128D;
    private static final double PORTS = 1D;
    protected Map<String, Integer> requiredInstances = new HashMap<>();
    protected Map<String, List<Protos.TaskID>> runningInstances = new HashMap<>();
    private String masterAddress;

    public static String getDockerImageName() {
        return dockerImageName;
    }

    public static double getCPU() {
        return CPU;
    }

    public static double getMEM() {
        return MEM;
    }

    public static double getPORTS() {return PORTS;}

    public String getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(String masterAddress) {
        this.masterAddress = masterAddress;
    }

    /**
     * Adds the given integer to the required amount of instances for the given elasticSearchUrl.
     * If the new required amount of instances is equal to or lower than 0, the elasticSearchUrl is removed from the requirements.
     * TODO: find a name that better matches the method
     * TODO: Is handling of instances configuration?
     *
     * @param elasticSearchUrl the elasticSearchUrl to change the required amount of instances for
     * @param amount           the amount by which to change the required amount of instances
     */
    public void putRequiredInstances(String elasticSearchUrl, int amount) {
        if (requiredInstances.containsKey(elasticSearchUrl)) {
            int newAmount = amount + requiredInstances.get(elasticSearchUrl).intValue();
            if (newAmount <= 0) {
                requiredInstances.remove(elasticSearchUrl);
            } else {
                requiredInstances.put(elasticSearchUrl, newAmount);
            }
        } else if (amount > 0) {
            requiredInstances.put(elasticSearchUrl, amount);
        }
    }

    /**
     * Gets a Map with all known elasticSearchUrls and the delta between the required and running number of instances.
     *
     * @return a Map with all known elasticSearchUrls and the delta between the required and running number of instances
     */
    public Map<String, Integer> getRequirementDeltaMap() {
        //TODO clean up distinct merge
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
        if (runningInstances.containsKey(elasticSearchUrl))
            runningInstances.get(elasticSearchUrl).add(taskId);
        else {
            ArrayList<Protos.TaskID> instances = new ArrayList<>();
            instances.add(taskId);
            runningInstances.put(elasticSearchUrl, instances);
        }
    }

    /**
     * Removes the given task from the currently running tasks.
     *
     * @param task the task to remove
     */
    public void removeRunningTask(Protos.TaskID task) {
        for (List<Protos.TaskID> tasks : runningInstances.values()) {
            if (tasks.contains(task)) {
                tasks.remove(task);
                return;
            }
        }
    }
}