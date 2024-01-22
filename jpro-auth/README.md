# JPro Auth

## Introduction

Rely on this library to add sophisticated authentication and authorization to your **JPro/JavaFX**. At the core, it
provides the interfaces and classes that can be used as a foundation during the authentication and authorization
process, backed by different providers that can also be created, extended or customized via a fine control access with
a degree of customization that can accommodate even the most complex security requirements. By implementing the OAuth
2.0 specifications (the industry-standard protocol for authorization) and to some extent OpenID Discovery, it allows
us to integrate with a wide range of identity providers, including Google, Microsoft, Keycloak and more to be added.
To enhance performance and responsiveness, it supports asynchronous operations preventing UI blocking during
authentication processes.

## Features

- Provide core classes and interfaces to implement authentication and authorization processes.
- OAuth2 and OpenID Discovery.
- Asynchronous operations.
- Support multiple identity providers.

## Architecture Overview

1. Basic concepts
    - **Authentication**: The process of verifying the identity of a user.
    - **Authorization**: The process of verifying whether a user has the necessary permissions to perform a task or to
      access a resource.
    - **Credentials**: A set of data that is used to authenticate a user. The data can be a username and password, or a
      JWT token, or other authentication methods such as OAuth2 that is issued by an identity provider.

2. Advanced concepts
    - **Identity Provider**: A service that authenticates users and issues credentials. It can be a Google, Microsoft,
      Keycloak or any other OAuth2 provider.
    - **OAuth2**: An open standard for access delegation, commonly used as a way for Internet users to grant websites or
      applications access to their information without giving them the passwords. The following OAuth2 flows are
      supported:
        1. **Authorization Code Flow**: The most commonly used flow, where the user is redirected to the identity
           provider to authenticate and authorize the application. The authorization code grant type is used to obtain
           both access tokens and refresh tokens and is optimized for confidential clients.
           Defined as part of the [OAuth 2.0 specification in RFC 6749, section 4.1](https://tools.ietf.org/html/rfc6749#section-4.1)
        2. **Password Credentials Flow**: This flow is suitable in cases where the resource owner has a trust
           relationship with the client, such as the device operating system or a highly privileged application. The
           authorization server should take special care when enabling this grant type and only allow it when other
           flows are not viable. Defined as part of the OAuth 2.0 specification in [RFC 6749, section 4.3](https://tools.ietf.org/html/rfc6749#section-4.3)
        3. **Client Credentials Flow**: This flow involves an application exchanging its application credentials, such
           as client ID and client secret, for an access token. This flow is best suited for Machine-to-Machine (M2M)
           applications, such as CLIs, daemons, or backend services, because the system must authenticate and authorize
           the application instead of a user. [Defined as part of the OAuth 2.0 specification in RFC 6749, section 4.4](https://tools.ietf.org/html/rfc6749#section-4.4)
        4. **JWT Flow**: This flow is similar to the authorization code flow, but instead of returning an authorization
           code, the authorization server returns a signed JWT token. This flow is suitable for clients that cannot
           maintain the confidentiality of their client secret, such as native applications.
           Defined as part of the OAuth 2.0 specification in [RFC 7523, section 2.1](https://tools.ietf.org/html/rfc7523#section-2.1)
    - **OpenID Discovery**: An open standard for authentication, commonly used as a way for Internet users to log in to
      websites or applications using their existing accounts from other websites but without giving them the passwords.

3. Main Classes and Interfaces
    - The **User** class is the main implementation of the **Authentication** interface and holds information
      like `name`, optional `roles` and optional `attributes`. After a successful authentication, this information
      is immutable and cannot be changed unless a new instance is created. While roles can be used for authorization
      purposes, attributes can be used to hold additional information about the user and authentication metadata.
    - The **Credentials** interface is the base interface for all credentials. Currently, there are three
      implementations:
        1. **UsernamePasswordCredentials**: This is the most basic implementation of the **Credentials** interface and
           holds a `username` and `password`.
        2. **TokenCredentials**: This implementation holds a JWT token issued by an identity provider.
        3. **OAuth2Credentials**: This implementation holds the `accessToken`, `idToken` and `refreshToken` issued by an
           OAuth2 provider.
    - The **AuthenticationProvider** interface is the base interface for all authentication providers. The
      authentication
      is done asynchronously by calling the `authenticate` method with a given **Credentials** object and the result
      is a **User** object. Currently, there are three major implementations:
        1. **BasicAuthenticationProvider**: This is the most basic implementation and can be used to authenticate users
           with a username and password.
        2. **TokenAuthenticationProvider**: This implementation can be used to authenticate users with a JWT token.
        3. **OAuth2AuthenticationProvider**: This implementation can be used to authenticate users with an OAuth2 based
           providers. Currently, we provide ready to be used, out of the box, pre-configured classes for known
           providers like Google, Microsoft and Keycloak. However, it is still possible to create a custom ones for any
           OAuth2 provider.
    - The **Option** classes are used to configure the authentication and authorization process and increase the
      flexibility of the API.
    - The **AuthAPI** class is the main entry point to the authentication and authorization process. It provides methods
      to authenticate users with a given **AuthenticationProvider** and to authorize users with a given **Credentials**
      object.

## Getting Started

### Installation

The `jpro-auth-core` module contains the core classes and interfaces to implement authentication and authorization
processes. Add the following configuration to your project based on the build tool you are using:

- Gradle
    ```groovy
    dependencies {
          implementation("one.jpro.platform:jpro-auth:0.2.12-SNAPSHOT")
    }
    ```
- Maven
    ```xml
    <dependencies>
      <dependency>
        <groupId>one.jpro.platform</groupId>
        <artifactId>jpro-auth-core</artifactId>
        <version>0.2.12-SNAPSHOT</version>
      </dependency>
    </dependencies>
    ```

The `jpro-auth-routing` module provides additional API to simplify the combination of the routing capabilities during
the authentication process. Add the following configuration to your project based on the build tool you are using:

- Gradle
    ```groovy
    dependencies {
          implementation("one.jpro.platform:jpro-auth-routing:0.2.12-SNAPSHOT")
    }
    ```
- Maven
    ```xml
    <dependencies>
      <dependency>
        <groupId>one.jpro.platform</groupId>
        <artifactId>jpro-auth-routing</artifactId>
        <version>0.2.12-SNAPSHOT</version>
      </dependency>
    </dependencies>
    ```

**Note**: The `jpro-auth-core` module will automatically be included as a dependency of the `jpro-auth-routing` module,
so there is no need to add it explicitly.

### Usage

- The following example shows how to authenticate a user with a username and password using the
  `BasicAuthenticationProvider` in combination with `UsernamePasswordCredentials` class.
    ```java
    public class BasicAuthExample {
        public static void main(String[] args) {
            // Create a basic AuthenticationProvider with a single role "USER"
            BasicAuthenticationProvider basicAuthProvider = AuthAPI.basicAuth()
                    .roles(Set.of("USER"))
                    .create();
            
            // Username and Password Credentials holds the username and password
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("someuser", "somepassword");
            
            // Authenticate the user with the credentials
            basicAuthProvider.authenticate(credentials)
                    .thenAccept(user -> {
                        System.out.println("User authenticated: " + user.getName());
                    })
                    .exceptionally(throwable -> {
                        System.out.println("Authentication failed: " + throwable.getMessage());
                        return null;
                    });
        }
    }
    ```
  
### Combined with the Routing API
By simply adding the `jpro-auth-routing` dependency to your project, the authentication process can be simplified even 
further resulting in a more concise and readable code. The following example shows how to authenticate a user with a 
username and password using the `BasicAuthenticationProvider` in combination with `UsernamePasswordCredentials` class
and the `AuthFilter` class provided by this module.

```java
public class BasicAuthExample extends RouteApp {
    
    // Create a basic AuthenticationProvider with a single role "USER"
    BasicAuthenticationProvider basicAuthProvider = AuthAPI.basicAuth()
            .roles(Set.of("USER"))
            .create();

    // Username and Password Credentials holds the username and password
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials();
    
    @Override
    public Route createRoute() {
        return Route.empty()
                .and(Route.get("/", request -> Response.node(new LoginPage(this, basicAuthProvider, credentials))))
                .when(request -> isUserAuthenticated(), Route.empty()
                        .and(Route.get("/user/signed-in", request -> Response.node(new SignedInPage(this)))))
                .filter(AuthFilter.create(basicAuthProvider, credentials, user -> {
                    setUser(user);
                    return Response.redirect("/user/signed-in");
                }, error -> Response.node(new ErrorPage(error))));
    }
}
```

Another example shows how to authenticate a user with an OAuth2 provider using the `GoogleAuthenticationProvider` in
combination with the `OAuth2Filter` class provided by this module.

```java
public class OAuth2Example extends RouteApp {
    
    @Override
    public Route createRoute() {
        // Create an OAuth2 AuthenticationProvider for Google
        GoogleAuthenticationProvider googleAuthProvider = AuthAPI.googleAuth()
                .clientId("your-client-id")
                .clientSecret("your-client-secret")
                .create(getStage());
        
        return Route.empty()
                .and(Route.get("/", request -> Response.node(new LoginPage(googleAuthProvider))))
                .when(request -> isUserAuthenticated(), Route.empty()
                        .and(Route.get("/user/signed-in", request -> Response.node(new SignedInPage(this, googleAuthProvider)))))
                .filter(OAuth2Filter.create(googleAuthProvider, user -> {
                    setUser(user);
                    return Response.redirect("/user/signed-in");
                }, error -> Response.node(new ErrorPage(error))));
    }
}
```
Inside the `LoginPage` class, the `googleAuthProvider` is used to create a login button that redirects the user to the
Google's login page.

```java
public class LoginPage extends VBox {
    
    public LoginPage(GoogleAuthenticationProvider googleAuthProvider) {
        Button loginButton = new Button("Login with Google");
        googleProviderButton.setOnAction(event -> OAuth2Filter.authorize(loginButton, googleAuthProvider));
        
        getChildren().add(loginButton);
    }
}
```


### Launch the examples

**Basic login sample**

* As desktop application
  ```shell
  ./gradlew jpro-auth:example:run -Psample=basic-login
  ```
* As JPro application
  ```shell
  ./gradlew jpro-auth:example:jproRun -Psample=basic-login
  ```

**Google login sample**

* As desktop application
  ```shell
  ./gradlew jpro-auth:example:run -Psample=google-login
  ```
* As JPro application
  ```shell
  ./gradlew jpro-auth:example:jproRun -Psample=google-login
  ```

**OAuth sample**

* As desktop application
  ```shell
  ./gradlew jpro-auth:example:run -Psample=oauth
  ```
* As JPro application
  ```shell
  ./gradlew jpro-auth:example:jproRun -Psample=oauth
  ```