# SMART Backend Authorization Helper

This project is a library to assist with the SMART Backend Authorization protocol on HAPI-based FHIR servers.
Due to the many different ways HAPI servers can be setup, there is some configuration required.


# Installation

This project can be added to an existing Maven-based project, add this dependency to `pom.xml`:

```xml
<dependency>
  <groupId>org.mitre.hapifhir</groupId>
  <artifactId>smart-backend-auth</artifactId>
  <version>0.0.1</version>
</dependency>
```

Or for a Gradle-based project, add this to `build.gradle`:

```
compile 'org.mitre.hapifhir:smart-backend-auth:0.0.1'

```

# Usage

There are two main components needed for a server to support the SMART Backend Authorization Protocol:
1. The server must support token-based authentication, using an interceptor to validate incoming requests.
  - This library provides a `BackendAuthorizationInterceptor`

2. The server must expose the token URL via the server /metadata (CapabilityStatement) and the /.well-known/smart-configuration endpoints.
  - This library provides a helper to override the server metadata, and another helper to construct the right format for .well-known/smart-configuration. Because there is no one common way to get properties across HAPI servers, is up to the user to expose an endpoint with this content. An example implementation using a RestController and web.xml is included below


For example, using a JPA Starter HAPI FHIR Server:


```java

import org.mitre.hapifhir.BackendAuthorizationInterceptor;
import org.mitre.hapifhir.SMARTServerCapabilityStatementProvider;

...

public class JpaRestfulServer extends RestfulServer {


  protected void initialize() {
    ...
    BackendAuthorizationInterceptor authorizationInterceptor =
      new BackendAuthorizationInterceptor(HapiProperties.getAuthServerCertsAddress());
    this.registerInterceptor(authorizationInterceptor);
    ...

    SMARTServerCapabilityStatementProvider smartCSProvider =
        new SMARTServerCapabilityStatementProvider(HapiProperties.getAuthServerTokenAddress());

    setServerConformanceProvider(smartCSProvider);
  }
}

```

---

```java
WellKnownEndpointController.java:

package ca.uhn.fhir.jpa.starter;

import javax.servlet.http.HttpServletRequest;

import org.mitre.hapifhir.WellknownEndpointHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellknownEndpointController {
    /**
     * Get request to support well-known endpoints for authorization metadata. See
     * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
     *
     * @return String representing json object of metadata returned at this url
     * @throws IOException when the request fails
     */
    @GetMapping(path = "/smart-configuration", produces = {"application/json"})
    public String getWellKnownJson(HttpServletRequest theRequest) {
      String yourTokenUrl = HapiProperties.getAuthServerTokenAddress();
      
      return WellknownEndpointHelper.getWellKnownJson(yourTokenUrl);
    }
}

```


```xml
web.xml:

  <servlet>
    <servlet-name>fhirServlet</servlet-name>
    <servlet-class>ca.uhn.fhir.jpa.starter.JpaRestfulServer</servlet-class>
    <load-on-startup>1</load-on-startup>
  </servlet>
  <servlet-mapping>
    <servlet-name>fhirServlet</servlet-name>
    <url-pattern>/fhir/*</url-pattern>
  </servlet-mapping>

  <servlet>
        <servlet-name>wellknown</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextClass</param-name>
            <param-value>
                org.springframework.web.context.support.AnnotationConfigWebApplicationContext
            </param-value>
        </init-param>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>
               ca.uhn.fhir.jpa.starter.wellknown.WellKnownEndpointController
            </param-value>
        </init-param>
        <load-on-startup>3</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>wellknown</servlet-name>
        <url-pattern>/fhir/.well-known/*</url-pattern>
    </servlet-mapping>

```



# Development

To install the current working version to your local Maven repo, run
```
./gradlew publishToMavenLocal
```


# License
Copyright 2021 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.