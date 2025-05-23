---
name: "Maven :: Plugin"
description: |
  Example to use a JKube Plugin in a Java Application.
  The plugin generates a build timestamp file that is later used by an application to return its version in and endpoint. It also downloads the latest Spring Boot Reference Documentation PDF to be exposed by the application as its home landing page.
---

This project demonstrates how to use JKube Plugins to perform additional actions during the different stages of the JKube processes.

The project is divided into two modules:
- `jkube-plugin`
- `app`

Let's start by analyzing the `jkube-plugin` module.

## jkube-plugin

A simple JKube Plugin that generates a build timestamp file that can be used as a version, and downloads the latest version of the Spring Boot documentation in PDF.

The plugin contains just a class implementing the JKubePlugin interface, and a resource file with its declaration so that it can be automatically injected and used by Eclipse JKube.

The files generated by this plugin are then consumed by the `app` module. Let's analyze it.

## app

This module contains a very simple Spring Boot application that exposes 2 endpoints that can be consumed through a web browser.

The first endpoint `/version` returns the contents of the file containing the build timestamp that was generated by the custom plugin.

The second endpoint `/` is the home landing page of the web application. It will automatically load and present the **latest** version of the Spring Boot documentation in PDF, that was downloaded by the custom plugin.

### Maven configuration

Note how the plugin module is declared as a Kubernetes Maven Plugin dependency:
```xml
<plugin>
  <groupId>org.eclipse.jkube</groupId>
  <artifactId>kubernetes-maven-plugin</artifactId>
  <version>${jkube.version}</version>
  <dependencies>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>jkube-plugin</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

### Dockerfile
The Dockerfile is tuned to copy the generated and downloaded files to the container image so that they can be consumed by the application.

```dockerfile
COPY maven/target/classes/jkube-extra/* files/
```

The application will attempt to load these files:
```java
@GetMapping("/version")
public String version() throws IOException {
  if (isProd()) {
    return Files.readString(Path.of("/deployments", "files", "build-timestamp"));
  }
  return Instant.now().toString();
}
```


## Running the example

If you have Minikube, it's as simple as running the following commands from the main quickstart project directory:

```shell
eval $(minikube docker-env)
mvn clean install
```

Since JKube's Maven goals are bound to the install phase, this should automatically deploy the application on Minikube.
You only need to run `minikube service app` from the command line, and a web browser window will open showing the latest Spring Boot Reference Documentation in PDF.
