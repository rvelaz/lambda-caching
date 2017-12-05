
# Lambda in-memory caching

A class variable can be used to to in-memory caching. Each instance of the AWS Lambda function will have its own copy of
the class variable, which means that if there a re multiple concurrent requests, each lambda instance will have to fetch
data and populate its caching.


# Cold starts

To avoid cold starts and having to re-populate the in-memory cache, the functions are invoked on a schedule basis.



## Setup

# Install serverless framework

https://serverless.com/framework/docs/getting-started/

And install plugins:

```
npm install
```


# Configure AWS keys
The user should have admin access

export AWS_ACCESS_KEY_ID=<your-key-here>
export AWS_SECRET_ACCESS_KEY=<your-secret-key-here>


# Deploy app

```
./gradlew build

sls deploy
```

This will output something like:

````
Service Information
service: in-memory-caching
stage: dev
region: eu-west-1
api keys:
  None
endpoints:
  POST - https://XXXX.execute-api.eu-west-1.amazonaws.com/dev/setup/{id}
  GET - https://XXX.execute-api.eu-west-1.amazonaws.com/dev/data/{id}
functions:
  setup: in-memory-caching-dev-setup
  read: in-memory-caching-dev-read

```

# Create sample data

```
curl -X POST https://XXXX.execute-api.eu-west-1.amazonaws.com/dev/setup/SOME_ID
```

This will create the bucket rvm-memory-caching--SOME_ID (or what you configure in serverless.yml DATA_BUCKET)
with a file that contains some data.


# Read the data

```
curl https://XXX.execute-api.eu-west-1.amazonaws.com/dev/data/SOME_ID/KEY_ID
```

KEY_ID values go from 1 to 12.

The first time it will read the data from a file in the bucket and will create an in memory caching. Next requests
will get data from the cache.

# Logs

To take a look at the logs when data is read:

```
sls logs -f read
```
