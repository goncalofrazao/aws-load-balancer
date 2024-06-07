package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Exception;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class DefaultHandler implements HttpHandler {
    String path;
    private static LambdaClient lambdaClient = LambdaClient.builder().credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
    private final static ObjectMapper mapper = new ObjectMapper();

    public DefaultHandler(String path) {
        this.path = path;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            t.sendResponseHeaders(204, -1);
            return;
        }

        InputStream stream = t.getRequestBody();
        
        URI requestURI = t.getRequestURI();
        String query = requestURI.getQuery();
        Map<String, String> queryMap = null;
        if (query != null) {
            queryMap = queryToMap(query);
        }

        ScanResult result = AmazonDynamoDBHandler.scanWithFilter(ComparisonOperator.EQ.toString(), new AttributeValue().withS(requestURI.getPath()), "RequestType");
        float load = LoadBalancer.MAX_LOAD;
        long imageSize, oldImageSize, basicBlocks, instructionsCount;

        if (result.getCount() > 0) {
            if (queryMap == null) {
                oldImageSize = Long.parseLong(result.getItems().get(0).get("ImageSize").getN());
                imageSize = oldImageSize;
                basicBlocks = Long.parseLong(result.getItems().get(0).get("BasicBlocks").getN());
                instructionsCount = Long.parseLong(result.getItems().get(0).get("InstructionsCount").getN());
            } else {
                imageSize = getImageSize(queryMap);
                oldImageSize = Long.parseLong(result.getItems().get(0).get("ImageSize").getN());
                basicBlocks = Long.parseLong(result.getItems().get(0).get("BasicBlocks").getN());
                instructionsCount = Long.parseLong(result.getItems().get(0).get("InstructionsCount").getN());
    
                for (Map<String, AttributeValue> item : result.getItems()) {
                    if (Long.parseLong(item.get("ImageSize").getN()) == imageSize) {
                        oldImageSize = Long.parseLong(item.get("ImageSize").getN());
                        basicBlocks = Long.parseLong(item.get("BasicBlocks").getN());
                        instructionsCount = Long.parseLong(item.get("InstructionsCount").getN());
                    }
                }
            }
            load = LoadBalancer.estimateComplexity(imageSize, oldImageSize, basicBlocks, instructionsCount);
        }

        Worker worker = LoadBalancer.chooseWorker(load);
        if (worker == null) {
            System.out.println("Calling lambda " + requestURI.getPath());
            try {
                String response = invokeLambda(requestURI, stream, query);
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (Exception e) {
                t.sendResponseHeaders(500, -1);
                OutputStream os = t.getResponseBody();
                os.close();
            }
            return;
        }

        String body = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));

        worker.addLoad(load);
        System.out.println("Redirecting to " + worker.getId() + ": new load " + worker.getLoad());

        try {
            String response = post(worker, body, path, query);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            worker.deactivate();
            handle(t);
            // t.sendResponseHeaders(500, -1);
            // OutputStream os = t.getResponseBody();
            // os.close();
        }
        
        worker.reduceLoad(load);
    }

    public String invokeImageprocLambda(InputStream stream, String functionName) {
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");
        String format = resultSplits[0].split("/")[1].split(";")[0];
        String json = "{\"body\":\"" + resultSplits[1] + "\",\"fileFormat\":\"" + format + "\"}";
        // System.out.println(json);
        SdkBytes payload = SdkBytes.fromUtf8String(json);

        InvokeRequest request = InvokeRequest.builder().functionName(functionName).payload(payload).build();

        String response = lambdaClient.invoke(request).payload().asUtf8String();
        response = response.substring(1, response.length() - 1);
        String r = String.format("data:image/%s;base64,%s", format, response);
        // System.out.println(r);
        return r;
    }

    public String invokeRaytracerLambda(InputStream stream, String query) {
        Map<String, String> parameters = queryToMap(query);
        int scols = Integer.parseInt(parameters.get("scols"));
        int srows = Integer.parseInt(parameters.get("srows"));
        int wcols = Integer.parseInt(parameters.get("wcols"));
        int wrows = Integer.parseInt(parameters.get("wrows"));
        int coff = Integer.parseInt(parameters.get("coff"));
        int roff = Integer.parseInt(parameters.get("roff"));
        
        String response = "";
        try {
            Map<String, Object> body = mapper.readValue(stream, new TypeReference<>(){});
            byte[] input = ((String) body.get("scene")).getBytes();
            byte[] textmap = null;
            if (body.containsKey("texmap")) {
                ArrayList<Integer> textmapBytes = (ArrayList<Integer>) body.get("texmap");
                textmap = new byte[textmapBytes.size()];
                for (int i = 0; i < textmapBytes.size(); i++) {
                    textmap[i] = textmapBytes.get(i).byteValue();
                }
            }

            String json = "{\"input\":\"" + Base64.getEncoder().encodeToString(input)
            + "\",\"scols\":\"" + scols 
            + "\",\"srows\":\"" + srows 
            + "\",\"wcols\":\"" + wcols 
            + "\",\"wrows\":\"" + wrows 
            + "\",\"coff\":\"" + coff 
            + "\",\"roff\":\"" + roff;
            
            if (textmap != null) {
                json += "\",\"texmap\":\"" + Base64.getEncoder().encodeToString(textmap) + "\"}";
            } else {
                json += "\"}";
            }

            SdkBytes payload = SdkBytes.fromUtf8String(json);
            // System.out.println(json);
            InvokeRequest request = InvokeRequest.builder().functionName("raytracer").payload(payload).build();
            String res = lambdaClient.invoke(request).payload().asUtf8String();
            String output = res.substring(1, res.length() - 1);
            response = String.format("data:image/bmp;base64,%s", output);

            // System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public String invokeLambda(URI requestURI, InputStream stream, String query) {
        if (requestURI.getPath().equals("/blurimage")) {
            return invokeImageprocLambda(stream, "blurimage");
        } else if (requestURI.getPath().equals("/enhanceimage")) {
            return invokeImageprocLambda(stream, "enhanceimage");
        } else if (requestURI.getPath().equals("/raytracer")) {
            return invokeRaytracerLambda(stream, query);
        }
        return "";
    }

    public long getImageSize(Map<String, String> query) {
        return Long.parseLong(query.get("wcols")) * Long.parseLong(query.get("wrows"));
    }

    public Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    public String post(Worker worker, String requestBody, String path, String query) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://" + worker.getDNS() + ":8000" + path + "?" + query))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
