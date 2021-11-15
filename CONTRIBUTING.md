# CoatRack Contributor Guidelines



## Introduction

#### Possibilities to Contribute

Hello, dear prospective contributor. You can support CoatRack in many ways:

- Report bugs using CoatRack's GitHub issue tracker.
- Discuss suggested code changes in open Pull Requests.
- Submit a bugfix.
- Propose new features and improvement ideas.
- Improve the documentation.
- Clean up code.
- Add tests.
- Help us to fulfill the [FIWARE requirements](https://fiware-requirements.readthedocs.io/en/latest/).



Note: If you want to contribute to CoatRack you need to sign the corresponding Contributors License Agreement (CLA) beforehand. Signing instructions can be found in In the corresponding section of this document.



## Conventions

#### Applying Best Practices

The establishment of best practices in CoatRack and compliance of these is important to us. We put great effort to our review process to steadily improve the quality of our code base and we expect the same willingness from you. Here are a few practices that should always be considered: 

* Code is written for readers and should be quickly understood by them. Use descriptive names, a clear architecture, short functions and small, cohesive classes. Add comments and Java Doc if required by readers to understand the code.
* Use a consistent coding style that goes along with the already existing code. Please do not change the formatting of existing code that is not relevant to the issue you are working on.
* Spaces should be used for indentation, not tabs.
* Newly added code should be delivered with automated tests, for example unit tests or integration tests. Ensure to have a high code coverage.



#### Workflow

We use GitHub to host code, track issues and feature requests, as well as accept pull requests. We use the following workflow:

1. Fork the repo. Make sure to disable the GitHub action that tries to push the code of the master branch to our 'dev' deployment environment. Of course, this step must be performed only once. All other steps are run again for each issue.

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
* If you have a guess why this bug might be happening or why you think it didn't work, then tell us.



#### Recommended Terminology

To avoid misunderstandings, we have decided to provide a uniform terminology recommendation for the most important concepts in CoatRack. Try to avoid terms like 'server' and 'client' because the Gateway can be both in different contexts and therefore leads to confusion. As this recommendation did not exist from the very beginning of the project, you may encounter different wordings for the same concept. Here is an overview:

| Recommended Wording      | Also Used Wording              | Description                                                  |
| ------------------------ | ------------------------------ | ------------------------------------------------------------ |
| CoatRack Web Application | (CoatRack) Admin               | This is the central CoatRack component which can be reached at [coatrack.eu](https://coatrack.eu/). It offers a web UI to define services as well as creating gateways and API keys. |
| Gateway                  | Proxy                          | It grants access to the specific services if a Service User sends a valid API key to the gateway. |
| Service Provider (\*)        | Service Offerer, Service Admin | This person defines services as well as API keys which grant access to those services. The Service Provider also hosts the gateway that provides the services. |
| Service User (\*)             | Service Consumer               | This person can access services by contacting a gateway and providing a valid API key. |

(\*) A real person can be a Service User and a Service Provider at the same time.



## Contributor License Agreement

### License

By contributing, you agree that your contributions will be licensed under Apache License 2.0. When you submit code changes, your submissions are understood to be under the same Apache License 2.0 that covers the project.



### Signing Instructions

When submitting a pull request for the first time, you will need to agree to the contributor license agreement (CLA). 

* Individual private persons must agree on the [CLA for individuals](https://github.com/coatrack/cla/latest/individual-cla.pdf) and 

* Organizations must agree on the [CLA for entities](https://github.com/coatrack/cla/latest/entity-cla.pdf). 

If you agree to the respective CLA, you have to create a pull request including a file that officially and transparently documents your intention to sign the CLA:

1. Copy, rename and move [signing-template.md](https://github.com/coatrack/cla/signing-template.md) to `/cla/contributors/<your-github-username>.md`. 
2. Fill in the missing information, including the SHA-256 checksum of the CLA PDF file you want to sign, and add this file to your first pull request. The checksum clearly documents the version of the CLA that you agree to.
3. After you created the pull request, please send an email to `info@coatrack.eu` to inform us about the pull request. 

By merging your pull request, the information that you agreed to the CLA will be part of the official public code base of CoatRack.



### Structure of the 'cla' folder

For transparency reasons, we will briefly explain here how our CLA was created and how the signing process works. Harmony provides [templates](http://harmonyagreements.org/agreements.html) for CLA's in ODT format which are stored in the [original](https://github.com/coatrack/cla/source/original) folder. Based on those templates we made custom versions, adapted to our CoatRack requirements, which are stored in the [customized](https://github.com/coatrack/cla/source/customized) folder. If you want to see the differences between the original and our customized version, you can easily compare the two ODT files (e.g. by using the document comparison feature of LibreOffice). 

The customized CLA ODT's are converted to PDF's and stored in the [latest](https://github.com/coatrack/cla/latest) folder. These PDF files are the files that have to be signed/hashed in order to be allowed to contribute to CoatRack.



## Miscellaneous

#### Contact

If you have questions, remarks or you want to get involved in the project, please contact ```info@coatrack.eu```.



#### References

This document was inspired from the works of [briandk](https://gist.github.com/briandk/3d2e8b3ec8daf5a27a62) and [adam-p](https://github.com/adam-p/markdown-here/blob/master/CONTRIBUTING.md). The content of the [signing-template.md](https://github.com/coatrack/signing-template.md) was derived from [adam-p's agreement file](https://github.com/adam-p/markdown-here/blob/master/contributors/adam-p.md). The signing method was adopted from the [medium/opensource](https://github.com/medium/opensource) project.

