# CoatRack Contribution and Developer Guidelines



## Introduction

#### Possibilities to Contribute

Hello, dear prospective contributor. You can support CoatRack in many ways:

- Report bugs using GitHub's issue tracker.
- Discuss the current state of the code in open Pull Requests.
- Submit a bugfix.
- Propose new features and improvement ideas.
- Improve the documentation.
- Clean up code.
- Add tests.
- Help us to fulfill the [FIWARE requirements](https://fiware-requirements.readthedocs.io/en/latest/).



Note: If you want to contribute to CoatRack you need to sign the corresponding Contributors License Agreement (CLA) previously. Signing instructions can be found in [signing-tutorial.md](https://github.com/coatrack/signing-tutorial.md). 



## Conventions

#### Applying Best Practices

The establishment of best practices in CoatRack and compliance of these is important to us. We put great effort to our review process to steadily improve the quality of our code base and we expect the same willingness from you. Here are a few practices that should always be considered: 

* Code is written for readers and should be quickly understood by them. Use descriptive names, a clear architecture, short functions and small, cohesive classes. Add Java Docs if necessary.
* Use a consistent coding style that goes along with the already existing code. Please do not change the formatting of exiting code that is not relevant to the issue you are working on.
* Spaces should be used for indentation, not tabs.
* Newly added code should be delivered with automated tests, for example unit tests or integration tests. Ensure to have a high code coverage.



#### Workflow

We use GitHub to host code, track issues and feature requests, as well as accept pull requests. We use the following workflow:

1. Fork the repo. Of course, this step must be performed only once. All others are run again for each issue.

2. You can either work on an existing issue or you have to create a descriptive issue before you can make code changes. 

3. Create a branch from `master` for the issue you are working on. 

   * Use this naming scheme: 
     * `<commit-type>/#<issue-number>-<short-description>`. 
     * Commits types are 'enhancement', 'feature' and 'bugfix'.

   * Example: 
     * `enhancement/#122-update-of-pom.xml-dependencies`

4. If you intend to do fundamental changes, e.g. changing APIs, please contact us beforehand. This is to avoid breaking existing functionality.

5. Before creating a pull request, ensure that
   1. all tests are are passing,
   2. the comment of the pull request contains at least this line: 
      * `closes #<issue-number>` .

6. At least two active maintainers of the projects need to review your code before it is allowed to be merged.



#### How to Make a Bug Report

* Add a quick descriptive summary and/or background information.
* Tell us what behavior of the application you expected and what you actually encountered.
* Screenshots and log files are very helpful for us.
* You should be able to reproduce the bug and enable us to do the same. Write down every single step that leads to the bug.
* If you have a guess why this bug might be happening or why you think, it didn't work, then tell us.



#### Recommended Terminology

To avoid misunderstandings, we have decided to provide a uniform terminology recommendation for the most important concepts in CoatRack. Try to avoid terms like 'server' and 'client' because the Gateway can be both in different contexts and therefore leads to confusion. As this recommendation did not exist from the very beginning of the project, you may encounter different wordings for the same concept. Here is an overview:

| Recommended Wording      | Also Used Wording              | Description                                                  |
| ------------------------ | ------------------------------ | ------------------------------------------------------------ |
| CoatRack Web Application | (CoatRack) Admin               | This is the central CoatRack component which can be reached at [coatrack.eu](https://coatrack.eu/). It offers a web UI to define services as well as creating gateways and API keys. |
| Gateway                  | Proxy                          | It grants access to the specific services if a Service User sends a valid API key to the gateway. |
| Service Provider (\*)        | Service Offerer, Service Admin | This person defines services as well as API keys which grant access to those services. The Service Provider also hosts the gateway that provides the services. |
| Service User (\*)             | Service Consumer               | This person can access services by contacting a gateway and providing a valid API key. |

(\*) A real person can be a Service User and a Service Provider at the same time.



## Miscellaneous

#### Contact

If you have questions, remarks or you want to get involved in the project, please contact ```info@coatrack.eu```.



#### References

This document was derived from [briandk/CONTRIBUTING.md](https://gist.github.com/briandk/3d2e8b3ec8daf5a27a62).
