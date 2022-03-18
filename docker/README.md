# Local CoatRack Deployment



## For  CoatRack Users

### Deployment For Newcomers

This approach is suggested if you are completely new to CoatRack and want to just want to experience what it actually does. Follow these instructions:

1. Change the `INSERT_SAMPLE_DATA_ON_STARTUP` parameter in `.env` to `true` to initialize the example Gateway in the database.
2. Go to the directory `docker-compose-deployment` and execute:

```sh
docker-compose --profile example-gateway up
```

* The last step is to check out the service provided by the example Gateway. Search for this URL in your browser:

```http
http://localhost:8088/humidity-by-location?api-key=ee11ee22-ee33-ee44-ee55-ee66ee77ee88
```



### Deployment for Development and Production

This approach is meant for developers and people who would like to deploy a production-ready CoatRack instance with a clean, empty database and without an example Gateway. Just go to the directory `docker-compose-deployment` and execute:

```sh
docker-compose up
```



## For CoatRack Developers

There are two scripts in the `docker` directory: 

* `build-and-push-images` builds the docker images of CoatRack modules from source and pushes the image to Dockerhub. This only works, when the docker daemon is already logged in to the repository. This script is especially useful within a CI pipeline to update the latest Dockerhub images of CoatRack.
* `build-and-deploy-images-locally` also build the above mentioned docker images from scratch but additionally deploys a production-ready instance locally. This is especially useful for developers who change the source code locally and want to test the impact of that change in a realistic setup.
