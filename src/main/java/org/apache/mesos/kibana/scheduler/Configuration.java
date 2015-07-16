package org.apache.mesos.kibana.scheduler;

public class Configuration {
    private static final String dockerImageName = "kibana";
    private static final double CPU = 0.1D;
    private static final double MEM = 128D;


    public static String getDockerImageName() {
        return dockerImageName;
    }

    public static double getCPU() {
        return CPU;
    }

    public static double getMEM() {
        return MEM;
    }
}