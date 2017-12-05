package com.rvm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class SetupHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private static final Logger LOG = Logger.getLogger(SetupHandler.class);

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        AmazonS3 s3client = getAmazonS3Client();

        String id = ((Map<String, String>) input.get("pathParameters")).get("id");
        String bucketName = getBucketName() + id;

        try {
            if (!(s3client.doesBucketExistV2(bucketName))) {
                s3client.createBucket(new CreateBucketRequest(bucketName));
                String fileName = "sample.data";
                try {
                    File temp = createSampleDataFile();
                    s3client.putObject(new PutObjectRequest(bucketName, fileName, temp));
                    temp.delete();
                    return ApiGatewayResponse.builder()
                            .setStatusCode(201)
                            .setObjectBody("Bucket created: " + bucketName)
                            .build();
                } catch (IOException e) {
                    LOG.error("Problem creating file", e);
                }
            } else {
                return ApiGatewayResponse.builder()
                        .setStatusCode(202)
                        .setObjectBody("Bucket created already exists")
                        .build();
            }

        } catch (AmazonServiceException ase) {
            LOG.error("Error Message:    " + ase.getMessage());
            LOG.error("HTTP Status Code: " + ase.getStatusCode());
            LOG.error("AWS Error Code:   " + ase.getErrorCode());
            LOG.error("Error Type:       " + ase.getErrorType());
        }
        return ApiGatewayResponse.builder()
                .setStatusCode(500)
                .setObjectBody("Something went wrong")
                .build();
    }

    protected File createSampleDataFile() throws IOException {
        String tmpfileName = "sample";
        File temp = File.createTempFile(tmpfileName, ".txt");
        FileWriter writer = new FileWriter(temp);
        writer.write("1,data1\n");
        writer.write("2,data2\n");
        writer.write("3,data3\n");
        writer.write("4,data4\n");
        writer.write("5,data5\n");
        writer.write("6,data6\n");
        writer.write("7,data7\n");
        writer.write("8,data8\n");
        writer.write("9,data9\n");
        writer.write("10,data10\n");
        writer.write("11,data11\n");
        writer.write("12,data12\n");
        writer.close();
        return temp;
    }

    protected AmazonS3 getAmazonS3Client() {
        return AmazonS3ClientBuilder.standard().withRegion(getRegion()).build();
    }


    protected String getRegion() {
        return System.getenv("AWS_REGION");
    }

    protected String getBucketName() {
        return System.getenv("DATA_BUCKET");
    }
}
