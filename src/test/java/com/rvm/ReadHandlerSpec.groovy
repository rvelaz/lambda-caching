package com.rvm

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import spock.lang.Specification

class ReadHandlerSpec extends Specification {
    ReadHandler testObj
    AmazonS3 s3ClientMock
    def bucketPrefix = 'test-bucket-'

    def setup() {
        testObj = Spy(ReadHandler)
        testObj.getBucketName() >> bucketPrefix
        s3ClientMock = Mock()
    }

    def 'should create the cache if it\'s empty'() {
        given:
        Context contextMock = Mock()
        def id = 'test'
        def data = '1'
        def bucketName = "${bucketPrefix}${id}"
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        pathParameters.put("key", data)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)
        BufferedReader readerMock = Mock()
        readerMock.readLine() >>> ['1,data1', '2,data2', null]
        S3ObjectInputStream inputStreamMock = Mock()
        S3Object objectMock = Mock()

        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        1 * objectMock.getObjectContent() >> inputStreamMock
        1 * testObj.getBufferedReader(inputStreamMock) >> readerMock
        1 * s3ClientMock.getObject(bucketName, 'sample.data') >> objectMock
        1 * readerMock.close()
        assert testObj.cache.size() == 2
        assert testObj.cache.get('1') == 'data1'
        assert testObj.cache.get('2') == 'data2'
        assert response.statusCode == 200
        assert response.body == '"data1"'
    }

    def 'should return 404 if there\'s no value for a key'() {
        given:
        Context contextMock = Mock()
        def id = '123'
        def data = 'some-data'
        def bucketName = "${bucketPrefix}${id}"
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        pathParameters.put("key", data)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)
        BufferedReader readerMock = Mock()
        readerMock.readLine() >>> ['1,data1', '2,data2', null]
        S3ObjectInputStream inputStreamMock = Mock()
        S3Object objectMock = Mock()

        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        1 * objectMock.getObjectContent() >> inputStreamMock
        1 * testObj.getBufferedReader(inputStreamMock) >> readerMock
        1 * s3ClientMock.getObject(bucketName, 'sample.data') >> objectMock
        1 * readerMock.close()
        assert testObj.cache.size() == 2
        assert testObj.cache.get('1') == 'data1'
        assert testObj.cache.get('2') == 'data2'
        assert response.statusCode == 404
    }


    def 'should hit the cache'() {
        given:
        Context contextMock = Mock()
        def id = 'test'
        def data = '1a'
        Map<String, Object> pathParameters = new HashMap<>()
        pathParameters.put("id", id)
        pathParameters.put("key", data)
        Map<String, Object> inputMock = new HashMap<>()
        inputMock.put("pathParameters", pathParameters)

        testObj.cache = ['1':'data1', '2':'data2']
        when:
        ApiGatewayResponse response = testObj.handleRequest(inputMock, contextMock)

        then:
        1 * testObj.getAmazonS3Client() >> s3ClientMock
        0 * s3ClientMock.getObject(*_) >> {}
        assert response.statusCode == 200
        assert response.body == '"data1"'
    }
}
