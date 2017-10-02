package com.malithj.docker.controller;

import java.io.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerController implements Runnable {

    private static final String FILENAME = "/Users/temp/docker-controller/docker_stats";
    public float monitoringFrequency = 100;
    public final int TIME_BETWEEN_KILLS = 7000;
    public float memoryLimit = 400;
    public static int killcount = 0;
    boolean isSwarm;
    private String dockerRunCommand;
    String containerKillCommand = "docker kill";
    String containerStopCommand = "docker stop -t 5";


    /**
     * The controllers an individual docker container or set of containers in a swarm
     *
     * @param monitoringFrequency the time between monitoring
     * @param memoryLimit         kill/stop the container if its memory usage exceeds this limit
     * @param isSwarm             true if this is swarm
     * @param dockerRunCommand    command to restart the container in the case of an individual container
     */
    public DockerController(Float monitoringFrequency, Float memoryLimit, boolean isSwarm, String dockerRunCommand) {
        this.monitoringFrequency = monitoringFrequency;
        this.memoryLimit = memoryLimit;
        this.isSwarm = isSwarm;
        this.dockerRunCommand = dockerRunCommand;
    }

    public void run() {
        Runtime rt = Runtime.getRuntime();

        while (true) {
            try {

                String array[] = getRestartContainerIDs();
                Thread.sleep((long) monitoringFrequency);

                for (int i = 0; i < array.length; i++) {
                    System.out.println(array[i]);
                    stopContainer(array[i], true);
                    Thread.sleep(TIME_BETWEEN_KILLS);
                }

                System.out.print("\n");

                if (!isSwarm) {
                    Process proc = rt.exec(dockerRunCommand);
                    printExecutionOutput(proc);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    /**
     * kill/stop a container with a given ID
     *
     * @param containerID the ID of the container to be killed
     * @param isKill      kill if true, stop otherwise
     * @throws IOException
     */
    private void stopContainer(String containerID, boolean isKill) throws IOException {

        Runtime rt = Runtime.getRuntime();
        String command1 = isKill == true ? containerKillCommand : containerStopCommand + " " + containerID;
        System.out.println("killing the container " + command1);
        Process proc = rt.exec(command1);
        System.out.println("kill counter = " + killcount++);
        printExecutionOutput(proc);

    }

    private void printExecutionOutput(Process process) throws IOException {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        System.out.println("Here is the standard output of the command:\n");
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }

    /**
     * Get container IDs
     *
     * @return the IDs of containers whose memory usage exceeds the specified limit
     * @throws IOException
     */
    private String[] getRestartContainerIDs() throws IOException {
        ArrayList<String> containerToRestart = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec("docker stats --no-stream");
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        String memoryRegex = "%(.*?)MiB /";

        String containerIDRegex = "(.*?)        ";
        Pattern memoryPattern = Pattern.compile(memoryRegex);
        Pattern containerIDPattern = Pattern.compile(containerIDRegex);

        while ((line = stdInput.readLine()) != null) {
            System.out.println(line);
            Matcher m1 = memoryPattern.matcher(line);
            Matcher m2 = containerIDPattern.matcher(line);
            if (m1.find() && m2.find()) {
                float usedMemory = Float.parseFloat(m1.group(1));
                if (usedMemory > memoryLimit) {
                    System.out.println("      Adding to kill list: current memory = " + Float.parseFloat(m1.group(1)) + "   memory limit = " + memoryLimit + "  container id" + m2.group(1));
                    containerToRestart.add(m2.group(1));
                }
            }
        }
        System.out.println("\n");

        return containerToRestart.toArray(new String[containerToRestart.size()]);
    }


    public static void main(String args[]) {
        Float monitoringFrequency = Float.parseFloat(args[0]);
        Float memoryLimit = Float.parseFloat(args[1]);
        Boolean isSwarm = Boolean.parseBoolean(args[2]);
        String dockerRunCommand = args[3];
        DockerController controller = new DockerController(monitoringFrequency, memoryLimit, isSwarm, dockerRunCommand);

        String m = args[1];
        String n = args[2];


        controller.run();
        System.out.println("test");
    }

}