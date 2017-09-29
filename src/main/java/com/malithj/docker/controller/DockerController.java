package com.malithj.docker.controller;


import java.io.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerController implements Runnable
{

    private static final String FILENAME = "/Users/temp/docker-controller/docker_stats";
    public final int MONITERING_FREQUENCY = 1000;
    public final int TIME_BETWEEN_KILLS= 10000;
    public final float MEMORY_LIMIT = 450;
    private int count;


    public DockerController() {
        count = 0;
    }

    public void run() {


        while(true)
        {
            try {
                String command = "docker stop --time 5";
                Runtime rt = Runtime.getRuntime();

                String array [] = getRestartContainerIDs();


                Thread.sleep(MONITERING_FREQUENCY);

                for (int i = 0; i < array.length; i++)
                {
                    String command1 = command + " " + array[i];
                    System.out.println("killing the container " +  command1);
                    Process proc = rt.exec(command1);
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    System.out.println("Here is the standard output of the command:\n");
                    String s;

                    while ((s = stdInput.readLine()) != null) {
                        System.out.println(s);
                    }

                    System.out.println("Here is the standard error of the command (if any):\n");
                    while ((s = stdError.readLine()) != null) {
                        System.out.println(s);
                    }

                    Thread.sleep(TIME_BETWEEN_KILLS);
                }

                System.out.print("\n");


            }catch (Exception e)
            {
                e.printStackTrace();
            }

        }

    }


    private String [] getRestartContainerIDs() throws IOException {
        ArrayList<String> containerToRestart = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        Process proc= rt.exec("docker stats --no-stream");
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
            if(m1.find() && m2.find()) {

                System.out.println("(a) - current memory = " + m1.group(1) + "memory limit = " + MEMORY_LIMIT);

                if(Float.parseFloat(m1.group(1)) > MEMORY_LIMIT) {

                    System.out.println("(b) current memory = " + m1.group(1) + "memory limit = " + MEMORY_LIMIT);
                    containerToRestart.add(m2.group(1));
                }
            }
        }
        System.out.println("\n");

        return containerToRestart.toArray(new String[containerToRestart.size()]);
    }


    public static void main(String args[])
    {
        DockerController controller = new DockerController();
        controller.run();
        System.out.println("test");
    }

}