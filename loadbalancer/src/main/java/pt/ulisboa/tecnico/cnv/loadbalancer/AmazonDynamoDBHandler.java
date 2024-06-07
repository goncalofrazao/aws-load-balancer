package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class AmazonDynamoDBHandler {
    private static String AWS_REGION = "us-east-2";
    private static AmazonDynamoDB dynamoDB;
    private static int requestId = 0;
    private static String tableName = "RequestMetrics";
    
    public static void createTable() {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .withRegion(AWS_REGION)
            .build();

        try {
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(
                    new KeySchemaElement().withAttributeName("RequestType").withKeyType(KeyType.HASH),
                    new KeySchemaElement().withAttributeName("RequestId").withKeyType(KeyType.RANGE)
                )
                .withAttributeDefinitions(
                    new AttributeDefinition().withAttributeName("RequestType").withAttributeType(ScalarAttributeType.S),
                    new AttributeDefinition().withAttributeName("RequestId").withAttributeType(ScalarAttributeType.N)
                )
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(10L).withWriteCapacityUnits(10L));

            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            TableUtils.waitUntilActive(dynamoDB, tableName);
        } catch (Exception e) {
            System.err.println("Unable to create table: " + e.getMessage());
        }
    }

    public static void pushItem(String requestType, long imageSize, long basicBlocks, long instructionsCount) {
        dynamoDB.putItem(new PutItemRequest(tableName, newItem(requestType, imageSize, basicBlocks, instructionsCount)));
    }

    public static Map<String, AttributeValue> newItem(String requestType, long imageSize, long basicBlocks, long instructionsCount) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("RequestType", new AttributeValue(requestType));
        item.put("RequestId", new AttributeValue().withN(Integer.toString(requestId)));
        item.put("ImageSize", new AttributeValue().withN(Long.toString(imageSize)));
        item.put("BasicBlocks", new AttributeValue().withN(Long.toString(basicBlocks)));
        item.put("InstructionsCount", new AttributeValue().withN(Long.toString(instructionsCount)));
        requestId++;
        return item;
    }

    public static void describeTable() {
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: \n" + tableDescription + "\n");
    }
    
    public static ScanResult scanWithFilter(String comparisonOperator, AttributeValue attributeValue, String key) {
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition().withComparisonOperator(comparisonOperator).withAttributeValueList(attributeValue);
        scanFilter.put(key, condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDB.scan(scanRequest);        
        return scanResult;
    }

    public static ScanResult scanWithoutFilter() {
        ScanRequest scanRequest = new ScanRequest(tableName);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        
        System.out.println("Result:");
        for (Map<String, AttributeValue> item : scanResult.getItems()) {
            System.out.println(item);
        }
        System.out.println();

        return scanResult;
    }

    public static void main(String[] args) {
        createTable();

        // describeTable();

        scanWithoutFilter();
    }

}
