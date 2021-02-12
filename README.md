# CoatRack

CoatRack is a framework to manage backend-to-backend communication via REST services, consisting of:

* distributed, lightweight API gateways and
* a centralized web application to generate and manage those API gateways.

CoatRack can facilitate your work if you have existing REST APIs and you want to do one (or more) of the following: 

* monitoring the access to your APIs,
* authentication/authorization of calls to your APIs via API keys,
* monetization of API calls, based on pay-per-call rules or flatrates.

## Contents

* [Background](#background)
* [Install](#install)
* [Usage](#usage)
* [License](#license)

## Background

In case you would like to manage backend-to-backend communication via REST APIs, e.g. offering backend services to other parties, some general work is required in addition to developing the actual service API, e.g.:

* implementing mechanisms for authentication/authorisation, 
* providing access credentials to the users, 
* monitoring calls to the API, 
* generating statistics. 

CoatRack facilitates these general tasks, so that you can focus on developing the actual service API. 

CoatRack comprises a central web application and distributed lighweight API gateways, which are:

* generated/configured via the central application and 
* delivering statistics about monitored service API calls to the central application.

The following figure shows the typical CoatRack architecture, the CoatRack web application is depicted on the right and one CoatRack Service gateway is depicted on the left. The calls from the client to the service API are routed and logged by a custom CoatRack Gateway, which can be installed in the service provider's local network. Configuration and statistics are accessible via the CoatRack web application.

![CoatRack architecture overview](./spring-boot/admin/src/main/resources/static/images/coatrack-architecture-overview.png)

## Install

Scripts for building and running CoatRack are available in the root folder of the code base. To build CoatRack, use:

```console
./build.sh
```
To run the CoatRack web application, use:

```console
./run-admin-application.sh
```
After starting up, the CoatRack web application will be accessible at `http://localhost:8080` 

## Usage

To start using CoatRack: 

- either run the admin application locally, as explained in the [Install](#install) section
- or open the public instance available at (https://coatrack.eu).

To log-in to CoatRack, a Github account is required. After logging in and filling in the registration form, the CoatRack admin application will open. Use the "Getting started tutorials" that are accessible from inside the application to learn the basics of using CoatRack.

There are two tutorials inside the application:

- Offering service APIs via CoatRack
- Using service APIs offered via CoatRack

## License

CoatRack is licenced under [Apache 2.0 License](./LICENSE).

© 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
