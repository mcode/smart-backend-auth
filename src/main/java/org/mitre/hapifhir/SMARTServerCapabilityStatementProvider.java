package org.mitre.hapifhir;

import ca.uhn.fhir.rest.api.server.RequestDetails;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;

public class SMARTServerCapabilityStatementProvider extends ServerCapabilityStatementProvider {

  private String tokenAddress;
  
  // post processing steps are things on the CapabilityStatement like "setTitle" or "setPublisher"
  // ie, things users may want to set, but I don't want to define setters for every possible thing
  private List<Consumer<CapabilityStatement>> postProcessSteps;
  
  public SMARTServerCapabilityStatementProvider(String tokenAddress) {
    super();
    
    this.tokenAddress = tokenAddress;
    this.postProcessSteps = new LinkedList<>();
  }
  
  public SMARTServerCapabilityStatementProvider with(Consumer<CapabilityStatement> function) {
    postProcessSteps.add(function);
    return this; // for chaining
  }
  
  @Override
  public CapabilityStatement getServerConformance(
      HttpServletRequest request, RequestDetails requestDetails) {
    CapabilityStatement c = super.getServerConformance(request, requestDetails);

    CapabilityStatementRestSecurityComponent securityComponent =
        buildSecurityComponent(tokenAddress);

    // Get the CapabilityStatementRestComponent for the server if one exists
    List<CapabilityStatementRestComponent> restComponents = c.getRest();
    CapabilityStatementRestComponent rest = null;
    for (CapabilityStatementRestComponent rc : restComponents) {
      if (rc.getMode().equals(RestfulCapabilityMode.SERVER)) {
        rest = rc;
        break;
      }
    }

    if (rest == null) {
      // Create new rest component
      rest = new CapabilityStatementRestComponent();
      rest.setMode(RestfulCapabilityMode.SERVER);
      rest.setSecurity(securityComponent);
      c.addRest(rest);
    } else {
      rest.setSecurity(securityComponent);
    }

    // now apply our post-processing steps, if any
    postProcessSteps.forEach(step -> step.accept(c));

    return c;
  }
    
  private static CapabilityStatementRestSecurityComponent buildSecurityComponent(String tokenAddr) {
    CapabilityStatementRestSecurityComponent securityComponent =
        new CapabilityStatementRestSecurityComponent();
    Extension oauthExtension = new Extension();

    Extension tokenEndpointUri = new Extension("token", new UriType(tokenAddr));
    oauthExtension.setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
    oauthExtension.setExtension(Collections.singletonList(tokenEndpointUri));
    securityComponent.addExtension(oauthExtension);

    return securityComponent;
  }
}
