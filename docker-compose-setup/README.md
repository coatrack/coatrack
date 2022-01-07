# Local CoatRack Deployment



### Deployment For Newcomers

This approach is suggested if you are completely new to CoatRack and want to just want to experience what it actually does. Follow these instructions:

1. Change the `INSERT_SAMPLE_DATA_ON_STARTUP` parameter in `.env` to `true` to initialize the example Gateway in the database.
2. Execute:

```sh
docker-compose --profile example-gateway up
```

* The last step is to check out the service provided by the example Gateway. Search for this URL in your browser:

```http
http://localhost:8088/humidity-by-location?api-key=ee11ee22-ee33-ee44-ee55-ee66ee77ee88
```



### Deployment for Development and Production

This approach is meant for developers and people who would like to deploy a production-ready CoatRack instance with a clean, empty database and without an example Gateway. Just execute:

```sh
docker-compose up
```

