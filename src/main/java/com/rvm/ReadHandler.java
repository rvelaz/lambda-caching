package com.rvm;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReadHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {
    private static final Logger LOG = Logger.getLogger(ReadHandler.class);
    Map<String, String> cache;

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        AmazonS3 s3client = getAmazonS3Client();
        String id = ((Map<String, String>) input.get("pathParameters")).get("id");
        String key = ((Map<String, String>) input.get("pathParameters")).get("key");
        String bucketName = getBucketName() + id;

        if (cache != null) {
            LOG.debug("Hitting cache");
            if (cache.get(key) != null) {
                return ApiGatewayResponse.builder()
                        .setStatusCode(200)
                        .setObjectBody(cache.get(key))
                        .build();
            } else {
                return ApiGatewayResponse.builder()
                        .setStatusCode(404)
                        .setObjectBody(cache.get(key))
                        .build();
            }
        } else {
            if (cache == null) {
                cache = new HashMap<>();
            }
            try {

                String fileName = "sample.data";
                LOG.debug("About to read file from bucket" + bucketName + ". Adding a 5s delay");
                Thread.sleep(5000);
                S3Object object = s3client.getObject(bucketName, fileName);
                InputStream objectData = object.getObjectContent();
                BufferedReader reader = getBufferedReader(objectData);
                String str = "";
                while ((str = reader.readLine()) != null) {
                    String[] keyValue = str.split(",");
                    cache.put(keyValue[0], keyValue[1]);
                }
                reader.close();

                if (cache.get(key) == null) {
                    return ApiGatewayResponse.builder()
                            .setStatusCode(404)
                            .build();
                }
                return ApiGatewayResponse.builder()
                        .setStatusCode(200)
                        .setObjectBody(cache.get(key))
                        .build();

            } catch (IOException e) {
                LOG.error("Error reading file", e);
            } catch (AmazonServiceException ase) {
                LOG.error("Error Type:       " + ase.getErrorType());
                LOG.error("Error Message:    " + ase.getMessage());
                LOG.error("HTTP Status Code: " + ase.getStatusCode());
                LOG.error("AWS Error Code:   " + ase.getErrorCode());

            } catch (InterruptedException e) {
                LOG.error("Error waiting", e);
            }
        }

        return ApiGatewayResponse.builder()
                .setStatusCode(500)
                .setObjectBody("There was an error")
                .build();
    }

    protected BufferedReader getBufferedReader(InputStream objectData) {
        return new BufferedReader(new InputStreamReader(objectData));
    }


    protected String getRegion() {
        return System.getenv("AWS_REGION");
    }

    protected String getBucketName() {
        return System.getenv("DATA_BUCKET");
    }

    protected AmazonS3 getAmazonS3Client() {
        return AmazonS3ClientBuilder.standard().withRegion(getRegion()).build();
    }

}
