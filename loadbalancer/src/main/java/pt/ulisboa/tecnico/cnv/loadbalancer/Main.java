package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import java.util.Scanner;

public class Main {
    public static void ui() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            System.out.println("Input: " + input);
            if (input.equals("list")) {
                LoadBalancer.listWorkers();
            } else if (input.equals("stats")) {
                LoadBalancer.printStats();
            } else if (input.equals("exit")) {
                break;
            }
        }

        for (Worker worker : LoadBalancer.getWorkers()) {
            AutoScaler.terminateInstance(worker.getId());
        }

        scanner.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
			System.out.println("Usage: LoadBalancer <AMI_ID>");
			System.exit(1);
		}
		AutoScaler.setAmiId(args[0]);

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
		server.createContext("/", new DefaultHandler("/"));
		server.createContext("/raytracer", new DefaultHandler("/raytracer"));
		server.createContext("/blurimage", new DefaultHandler("/blurimage"));
		server.createContext("/enhanceimage", new DefaultHandler("/enhanceimage"));
		server.start();
		System.out.println("Server started");

        Thread uiThread = new Thread(() -> ui());
        uiThread.start();

        AmazonDynamoDBHandler.createTable();
        AutoScaler.scaleUp();

        Thread healthCheckThread = new Thread(() -> {
            while (true) {
                AutoScaler.healthCheck();
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        healthCheckThread.start();

        Thread autoScaleThread = new Thread(() -> {
            while (true) {
                AutoScaler.autoScale();
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        autoScaleThread.start();

        uiThread.join();
    }
}
