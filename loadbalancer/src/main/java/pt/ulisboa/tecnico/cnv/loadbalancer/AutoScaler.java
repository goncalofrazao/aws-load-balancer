package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Statistic;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class AutoScaler {
    private static String AWS_REGION = "us-east-2";
	private static String AMI_ID = "ami-0b5e57947118ef29f";
	private static String KEY_NAME = "mykeypair";
	private static String SEC_GROUP_ID = "launch-wizard-1";
    private static AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    private static AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();

    public static boolean SCALLING = false;

    private static int minInstances = 1;
    public static long OBS_TIME = 1000 * 60 * 5;

    public static void setAmiId(String amiId) {
        AMI_ID = amiId;
    }

    public static AmazonEC2 getEC2() {
        return ec2;
    }

    public static void scaleUp() throws Exception {
        SCALLING = true;
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withImageId(AMI_ID)
							.withInstanceType("t2.micro")
							.withMinCount(1)
							.withMaxCount(1)
							.withKeyName(KEY_NAME)
							.withSecurityGroupIds(SEC_GROUP_ID)
                            .withMonitoring(true);

		RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        String instanceId = instance.getInstanceId();
        Thread.sleep(1000);
        instance = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId)).getReservations().get(0).getInstances().get(0);
        
        String instanceDNS = instance.getPublicIpAddress();
        System.out.println(instanceDNS);
		System.out.println(instanceId + " with ip " + instanceDNS + " launched");
        Thread.sleep(30000);
        System.out.println("Instance " + instanceId + " is ready");
		LoadBalancer.addWorker(new Worker(instanceId, instanceDNS));
        SCALLING = false;
    }

    public static void scaleDown() {
        List<Worker> workers = LoadBalancer.getWorkers();
        Worker minWorker = workers.get(0);
        if (workers.size() > minInstances) {
            for (Worker worker : workers) {
                if (worker.getLoad() < minWorker.getLoad()) {
                    minWorker = worker;
                }
            }
            System.out.println("Terminating instance " + minWorker.getDNS() + " with load " + minWorker.getLoad());
            minWorker.deactivate();
        }
    }

    public static void terminateInstance(String instanceId) {
		TerminateInstancesRequest request = new TerminateInstancesRequest();
		request.withInstanceIds(instanceId);
		ec2.terminateInstances(request);
		System.out.println(instanceId + " instance terminated");
	}

    public static void autoScale() {
        List<Worker> workers = LoadBalancer.getWorkers();
        
        double averageCPUUtilization = 0;
        for (Worker worker : workers) {
            String instanceId = worker.getId();
            
            if (!worker.isActive() && worker.getLoad() == 0f) {
                terminateInstance(instanceId);
                LoadBalancer.removeWorker(worker);
                continue;
            }
            
            Instance instance = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId)).getReservations().get(0).getInstances().get(0);
            if (!instance.getState().getName().equals("running")) {
                continue;
            }

            GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                    .withNamespace("AWS/EC2")
                    .withMetricName("CPUUtilization")
                    .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                    .withStartTime(new Date(new Date().getTime() - OBS_TIME))
                    .withEndTime(new Date())
                    .withPeriod(60)
                    .withStatistics(Statistic.Average);
            GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
            List<Datapoint> datapoints = result.getDatapoints();
            
            if (datapoints.size() == 0) {
                continue;
            }

            double averageWorker = 0.0f;
            for (Datapoint datapoint : datapoints) {
                averageWorker += datapoint.getAverage();
            }

            averageWorker /= datapoints.size();
            averageCPUUtilization += averageWorker;
        }

        averageCPUUtilization /= workers.size();
        System.out.println("Average CPU Utilization: " + averageCPUUtilization);
        if (averageCPUUtilization > 60) {
            try {
                scaleUp();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else if (averageCPUUtilization < 20 && workers.size() > minInstances) {
            scaleDown();
        }
        
    }

    public static void healthCheck() {
        List<Worker> workers = LoadBalancer.getWorkers();
        List<Worker> toRemove = new ArrayList<>();
        for (Worker worker : workers) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + worker.getDNS() + ":8000/"))
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
                worker.resetHealthCheck();
            } catch (Exception e) {
                worker.incrementHealthCheck();
                if (worker.isNotHealthy()) {
                    toRemove.add(worker);
                }
            }
        }

        for (Worker worker : toRemove) {
            terminateInstance(worker.getId());
            workers.remove(worker);
        }
    }
}
