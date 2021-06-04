# Coatrack Roadmap

This product is a FIWARE Generic Enabler. If you would like to learn about the overall Roadmap of FIWARE, please check
"Roadmap" on the [FIWARE Catalogue](https://www.fiware.org/developers/catalogue/).

### Introduction

This section elaborates on proposed new features or tasks that are expected to be added to the product in the
foreseeable future. There should be no assumption of a commitment to deliver these features on specific dates or in the
order given. The development team will be doing their best to follow the proposed dates and priorities, but please bear
in mind that plans to work on a given feature or task may be revised. All information is provided as general guidelines
only, and this section may be revised to provide newer information at any time.

Disclaimer:

-   This section has been last updated in June 2021. Please take into account its content could be obsolete.
-   Note that we develop this software in an Agile way, so the development plan is continuously under review. Thus, 
    this roadmap has to be understood as a rough plan of features to be done along time, which is fully valid only at 
    the time of writing it. This roadmap has not to be understood as a commitment on features and/or dates.
-   Some of the roadmap items may be implemented by external community developers, out of the scope of GE owners. Thus,
    the moment in which these features will be finalized cannot be assured.

### Short term

The following list of features are planned to be addressed in the short term, and incorporated in a next release of the
product:

-   Allow CoatRack gateway to keep working for a short time without connection to central web application, 
    e.g. during maintenance downtimes of web application
-   Support basic "health monitoring" of CoatRack gateways, 
    to allow a quick overview which gateways are currently running/(dis-)connected
-   Support automated deployment to different test environments, in order to facilitate quick deployment/test cycles
-   Add CII Best Practices Badge and complete questionnaire.
-   Update Oauth2 configuration/libraries in order to be compatible with upcoming Github API changes 
    (Auth token in HTTP header)
-   Kubernetes deployment


### Medium term

The following list of features are planned to be addressed in the medium term, typically within the subsequent
release(s) generated in the next 9 months after the next planned release:

-   Fulfill all requirements as a full Generic Enabler
-   Add scripts and instructions to facilitate deployment of independent new CoatRack instances, 
    including their own configuration server
-   Additional visualisations on statistics dashboard (e.g. number of calls per service, distribution of errors over time)
-   Based on the existing integration from IoF2020, investigate specific requirements to further facilitate usage of 
    CoatRack in combination with FIWARE BAE and FIWARE Context Broker (e.g. facilitate deployment and configuration to work 
    in front of Context Broker)
-   Investigation compatibility 3scale
-   Feature enabling users to delete their CoatRack user account
-   Integrate database migration framework (e.g. Flyway or Liquibase)
-   Improve the authentication mechanism between web application and gateway (configurable API key for web application 
    and unification of two existing gateway IDs)

### Long term

The following list of features are proposals regarding the longer-term evolution of the product even though the
development of these features has not yet been scheduled for a release in the near future. Please feel free to contact
us if you wish to get involved in the implementation or influence the roadmap:

-   Allow several CoatRack user accounts to "share" gateways, so that several people could together "keep an eye" on a running gateway
-   Rework the management of usage metrics/statistics data in the web application, either simplifying the existing storage/filtering 
    approach or replacing by some standardized metrics database approach 
