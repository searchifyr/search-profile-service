# search-profile-service

Service for CRUD of search profiles

## Description

The Search-Profile-Service is a backend application that allows for creating and storing personalized search profiles based on an Elasticsearch instance. 
These search profiles can be used to execute Elasticsearch queries or to obtain direct search results.

This service exposes endpoints for:
- CRUD of applications
- CRUD of searchprofiles 
- CRUD of users
- Indexing of json-documents into an application
- Reading the mapping of the json-documents stored within an application

---
## Deployment

To start the `backend` with `maven` type:
```
$ ./mvnw clean spring-boot:run
```
Now the backend is running locally with maven.

To run all tests of the backend run:
```
$ ./mvnw clean verify -Dtest='!*ContainerizedTest'
```
To run static code analysis type
```
$ ./mvnw clean spotbugs:check
```

---
### ***Deployment of services in containers***

Requirements:
- AMD64 system
- docker-engine
- docker-compose installed.

If you also want to start the frontend (or mongodb and elasticsearch) you have to use the [deployment]() repository. Change your directory to the one of the `deployment` repository and start all services (backend - latest image; frontend - latest image, mongodb and elasticsearch) by typing:

```
$ docker-compose up -d
```

To only start a specific service, f.e. you want so start the frontend in a container to test things you added locally to the backend (for this example the command would be `docker-compose up web-app-service -d`), type:

```
$ docker-compose up [service-name] -d
```
Because the frontend and backend need mongodb and elasticsearch to start, its also been started and you dont have to specifically start both of them.

You can stop and remove all containers at once by typing:
```
$ docker-compose down
```
For additional commands and infos on how to use the service in containers see this [README]().

---
When everything is running, you can check the web app by calling `localhost:8080`.

To check the `openapi specification` of the project you need to call `localhost:7080/api/v1/doc`

---
## Tech Stack
- Java 17
- Spring Web
- Spring Doc
- Spring DevTools
- Spring Security
- Testcontainers
- Lombok
- MongoDB
- Java Elasticsearchclient
- Passay

---
## Package structure

| Function                                   | Submodule                    |
|--------------------------------------------|------------------------------|
| API for the controllers                    | api                          |
| Exception handling on the controller level | api/advice                   |
| Models used on the controller level        | api/model                    |
| Validation                                 | api/model/validator          |
| Defined routes for controller endpoints    | api/routes                   |
| Clients for communication                  | client                       |
| Implementation of clients                  | client/impl                  |
| Configuration                              | config                       |
| Custom exceptions                          | exception                    |
| Cross service models                       | model                        |
| Cross service enums                        | model/enums                  |
| Data access to databases                   | persistence                  |
| Database access to a MongoDB               | persistence/mongo            |
| Mongo specific configuration               | persistence/mongo/config     |
| Mongo-Migration                            | persistence/mongo/migration  |
| Mongo-Documents                            | persistence/mongo/model      |
| Mongo-Repository                           | persistence/mongo/repository |
| Services                                   | service                      |
| Implementation of services                 | service/impl                 |
| Utilities                                  | util                         |

---
## Necessary environment variables

| Environment Variables      | Function                                            |
|----------------------------|-----------------------------------------------------|
| MONGO_HOST                 | Host address of the mongodb instance                |
| MONGODB_AUTHENTICATION_DB  | Name of the authentication database                 |
| MONGODB_USERNAME           | Root-Username for mongodb                           |
| MONGODB_PASSWORD           | Root-Password for mongodb                           |
| ELASTICSEARCH_PORT         | Port of the elasticsearch instance                  |
| ELASTICSEARCH_HOST         | Host address of the elasticsearch instance          |
| ELASTICSEARCH_PASSWORD     | Root-Password for elasticsearch                     |
| GITHUB_CLIENT_ID           | Client id for OAuth authentication via Github       |
| GITHUB_CLIENT_SECRET       | Client secret for OAuth authentication via Github   |
| ADMIN_ID                   | Github user id of user that should have admin rights|
| LOGIN_REDIRECT_FRONTEND    | Redirect url after successful Github OAuth login    |
| LOGIN_CROSS_DOMAIN_REDIRECT| Boolean if user should get redirected after login   |
| FRONTEND_COOKIE_DOMAIN     | defines domain where cookies should be saved at     |

---
## Open-API specification
On service deployment the open-api specification for `/api/v1` can
be found under `/api/v1/doc`.</br>
The Open-API specification file itself can be found in 
`src/main/resources/static/openapi.yml`.

---
## Jacoco Test Report
This Projects uses the Jacoco Code coverage software metric tool. 
You can create a Test Code coverage report. 

How to:
1. Run mvn clean test
2. Run mvn jacoco:report

This will create a ./target/site/index.html .
Open this file with your default browser to inspect code coverage provided by our unittests.

---
## Styleguide

This project uses a [custom checkstyle rule set](./checkstyle.xml) as styleguide. </br>

---
