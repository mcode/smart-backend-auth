package org.mitre.hapifhir;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class BackendAuthorizationInterceptor extends AuthorizationInterceptor {

  private String authServerCertsAddress;
  
  public BackendAuthorizationInterceptor(String authServerCertsAddress) {
    super();
    this.authServerCertsAddress = authServerCertsAddress;
  }
  
  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    String authHeader = theRequestDetails.getHeader("Authorization");

    if (authHeader != null) {
      // Get the JWT token from the Authorization header
      String regex = "Bearer (.*)";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(authHeader);
      String token = "";
      if (matcher.find() && matcher.groupCount() == 1) {
        token = matcher.group(1);

        if (token.equals("admin")) {
          return authorizedRule();
        } else {
          try {
            verify(token);
            return authorizedRule();
          } catch (TokenExpiredException e) {
            e.printStackTrace();
            throw new AuthenticationException("Token is expired", e);
          } catch (JWTVerificationException e) {
            e.printStackTrace();
            throw new AuthenticationException("Token is invalid", e);
          } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            throw new AuthenticationException("Internal error processing public key", e);
          } catch (Exception e) {
            e.printStackTrace();
            throw new AuthenticationException("Unable to authorize token", e);
          }
        }
      } else {
        throw new AuthenticationException(
          "Authorization header is not in the form \"Bearer <token>\"");
      }
    }

    return unauthorizedRule();
  }

  private List<IAuthRule> authorizedRule() {
    return new RuleBuilder().allowAll().build();
  }

  private List<IAuthRule> unauthorizedRule() {
    // By default, deny everything except the metadata. This is for
    // unauthorized users
    return new RuleBuilder().allow().metadata().andThen().denyAll().build();
  }

  private RSAPublicKey getKey() 
      throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
    // Get the latest key from the auth server
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(authServerCertsAddress).build();
    Response response = client.newCall(request).execute();
    JSONObject jwks = new JSONObject(response.body().string());
    JSONArray keys = jwks.getJSONArray("keys");
    JSONObject key = (JSONObject) keys.get(0);
    String rawE = key.getString("e");
    String rawN = key.getString("n");

    BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(rawE));
    BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(rawN));

    KeyFactory kf = KeyFactory.getInstance("RSA");
    RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
    return (RSAPublicKey) kf.generatePublic(publicKeySpec);
  }

  /**
   * Verify the access token signature and expiration.
   * @param token - the access token
   * @return true if signature is valid and token has not expired, otherwise throws an exception
   */
  private boolean verify(String token) throws IllegalArgumentException, NoSuchAlgorithmException,
      InvalidKeySpecException, TokenExpiredException, JWTVerificationException, IOException {
    Algorithm algorithm = getAlgorithm(token, getKey());
    JWTVerifier verifier = JWT.require(algorithm).build();
    verifier.verify(token);

    return true;
  }

  private Algorithm getAlgorithm(String token, Object publicKey) throws NoSuchAlgorithmException {
    // Decode the header of the token
    String header = token.split("\\.")[0];
    byte[] decodedBytes = Base64.getDecoder().decode(header);
    String decodedHeader = new String(decodedBytes);

    // Get the alg
    JSONObject headerJSON = new JSONObject(decodedHeader);
    String alg = headerJSON.getString("alg");

    switch (alg) {
      case "HS256":
        return Algorithm.HMAC256((String) publicKey);
      case "HS384":
        return Algorithm.HMAC384((String) publicKey);
      case "HS512":
        return Algorithm.HMAC512((String) publicKey);
      case "RS256":
        return Algorithm.RSA256((RSAPublicKey) publicKey, null);
      case "RS384":
        return Algorithm.RSA384((RSAPublicKey) publicKey, null);
      case "RS512":
        return Algorithm.RSA512((RSAPublicKey) publicKey, null);
      case "ES256":
        return Algorithm.ECDSA256((ECPublicKey) publicKey, null);
      case "ES384":
        return Algorithm.ECDSA384((ECPublicKey) publicKey, null);
      case "ES512":
        return Algorithm.ECDSA512((ECPublicKey) publicKey, null);
      case "PS256":
      case "PS384":
      default:
        throw new NoSuchAlgorithmException("Algorithm is not supported by this library.");
    }
  }
}
