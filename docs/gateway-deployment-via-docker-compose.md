# Deploy Gateway via docker-compose using local servers

* The docker-compose.yml for proxies should work out of the box for usual CoatRack user who seeks to connect with a remote Web Application and a Config Server. 
* However, as developers you will set up local servers. The problem is that when a docker container accesses `localhost`, it will send a request to itself instead of a host device port. To realize this setup anyhow, perform the following instructions.



### Starting the Config Server

* Remove the the following attributes from Config Server application.yml and start the Config Server.

  ```yml
  spring:
    datasource <- 
    jpa:
      properties:
        hibernate:
          dialect <- 
  ```

  

### Configuring  Admin/Web Application

* `localhost` needs to be replaced or overridden with `host.docker.internal` (see attributes below) and start the admin server. For that use a custom profile like application-private-\<name\>.yml and override these parameters in the application.yml.

```yml
ygg:
  admin:
    api-base-url-for-gateway: http://host.docker.internal:8080/api/
  proxy:
    generate-bootstrap-properties:  
      spring.cloud.config.uri: http://host.docker.internal:8998
```



### Configuring the docker-compose.yml

* Create a Gateway and download its docker-compose.yml.
* Remove the "ports: 8080:8080" attribute in this file.
* Let the docker container be part of the hosts local network, by adding the service attribute `network_mode: "host"`.
* Run `docker-compose up`.