package com.rvm

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectRequest
import spock.lang.Specification

class SetupHandlerSpec extends Specification {
    SetupHandler testObj
    AmazonS3 s3ClientMock
    def bucketPrefix = 'test-bucket-'

    def setup(){
        testObj = Spy(SetupHandler)
        s3ClientMock = Mock()
        testObj.getBucketName() >> bucketPrefix
    }

    def 'should not create a bucket if exists'(){
        given:
        Context contextMock = Mock()
        def id = '123'
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)

        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        1 * s3ClientMock.doesBucketExistV2("${bucketPrefix}${id}") >> true
        response.statusCode == 202
    }

    def 'should create a bucket with a file containing sample data'(){
        given:
        Context contextMock = Mock()
        def id = '123'
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)
        File tmpFile = Mock()

        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        1 * s3ClientMock.doesBucketExistV2("${bucketPrefix}${id}") >> false
        1 * testObj.createSampleDataFile() >> tmpFile
        1 * s3ClientMock.putObject(_) >> { args ->
            assert ((PutObjectRequest)args[0]).getBucketName() =="${bucketPrefix}${id}"
            assert ((PutObjectRequest)args[0]).getKey() == 'sample.data'
        }
        1 * tmpFile.delete() >> {}
        response.statusCode == 201
    }

    def 'should return 500 if something went wrong'(){
        given:
        Context contextMock = Mock()
        def id = '123'
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)

        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        1 * s3ClientMock.doesBucketExistV2("${bucketPrefix}${id}") >> false
        1 * testObj.createSampleDataFile() >> { throw new IOException() }

        response.statusCode == 500
    }
}
