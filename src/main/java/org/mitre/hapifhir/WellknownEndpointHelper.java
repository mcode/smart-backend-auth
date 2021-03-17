package org.mitre.hapifhir;

import org.json.JSONArray;
import org.json.JSONObject;

public class WellknownEndpointHelper {

  // Well Known JSON Keys
  private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
  private static final String RESPONSE_TYPES_SUPPORTED_KEY = "response_types_supported";
  private static final String SCOPES_SUPPORTED_KEY = "scopes_supported";

  public static String getWellKnownJson(String tokenEndpointUrl) {
    JSONArray scopesSupported = new JSONArray();
    scopesSupported.put("system/*.read");
    scopesSupported.put("offline_access");

    JSONArray responseTypesSupported = new JSONArray();
    responseTypesSupported.put("token");

    JSONObject wellKnownJson = new JSONObject();
    wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY, tokenEndpointUrl);
    wellKnownJson.put(RESPONSE_TYPES_SUPPORTED_KEY, responseTypesSupported);
    wellKnownJson.put(SCOPES_SUPPORTED_KEY, scopesSupported);

    return wellKnownJson.toString(2);
  }
}
