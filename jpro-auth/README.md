# JPro Auth

### Introduction
Rely on this library to add sophisticated authentication and authorization to your **JPro/JavaFX**. At the core, it 
provides the interfaces and classes that can be used as a foundation during the authentication and authorization
process, backed by different providers that can also be created, extended or customized via a fine control access with
a degree of customization that can accommodate even the most complex security requirements. By implementing the OAuth2
specifications and to some extent OpenID Discovery, it allows us to integrate with a wide range of identity providers,
including Google, Microsoft, Keycloak and more to be added. To enhance performance and responsiveness, it supports 
asynchronous operations preventing UI blocking during authentication processes.

### Installation
- Gradle
    ```groovy
    dependencies {
          implementation("one.jpro.platform:jpro-auth:0.2.8-SNAPSHOT-SNAPSHOT")
    }
    ```
- Maven
    ```xml
    <dependencies>
      <dependency>
        <groupId>one.jpro.platform</groupId>
        <artifactId>jpro-auth</artifactId>
        <version>0.2.8-SNAPSHOT-SNAPSHOT</version>
      </dependency>
    </dependencies>
    ```

### Features
- Provide core classes and interfaces to implement authentication and authorization processes.
- OAuth2 and OpenID Discovery.
- Asynchronous operations.
- Support multiple identity providers.

### Architecture Overview
1. Basic concepts
   - **Authentication**: The process of verifying the identity of a user.
   - **Authorization**: The process of verifying whether a user has the necessary permissions to perform a task or to
     access a resource.
   - **Credentials**: A set of data that is used to authenticate a user. The data can be a username and password, or a JWT
     token, or other authentication methods such as OAuth2 that is issued by an identity provider.
   
2. Classes and Interfaces
   - The User class is the main implementation of the Authentication interface and holds information like `name`, optional 
   `roles` and optional `attributes`.
