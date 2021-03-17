package org.mitre.hapifhir.examples;

import javax.servlet.http.HttpServletRequest;

import org.mitre.hapifhir.WellknownEndpointHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellKnownEndpointController {
  /**
   * Get request to support well-known endpoints for authorization metadata. See
   * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
   *
   * @return String representing json object of metadata returned at this url
   * @throws IOException when the request fails
   */
  @GetMapping(path = "/smart-configuration", produces = { "application/json" })
  public String getWellKnownJson(HttpServletRequest theRequest) {
    String yourTokenUrl = ""; // get by configuration here

    return WellknownEndpointHelper.getWellKnownJson(yourTokenUrl);
  }
}
