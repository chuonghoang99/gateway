package com.apus.gateway.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.security.oauth2.common.util.OAuth2Utils;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class LoginResponseDeserializer extends StdDeserializer<LoginResponse> {

  public LoginResponseDeserializer() {
    super(LoginResponse.class);
  }

  @Override
  public LoginResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    LoginResponse loginResponse = new LoginResponse();

    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      String name = jsonParser.getCurrentName();
      jsonParser.nextToken();
      if ("access_token".equals(name)) {
        loginResponse.setAccessToken(jsonParser.getText());
      }
      else if ("token_type".equals(name)) {
        loginResponse.setTokenType(jsonParser.getText());
      }
      else if ("refresh_token".equals(name)) {
        loginResponse.setRefreshToken(jsonParser.getText());
      }
      else if ("expires_in".equals(name)) {
        loginResponse.setExpiresIn(jsonParser.getLongValue());
      }
      else if ("scope".equals(name)) {
        loginResponse.setScopes(parseScope(jsonParser));
      }
      else if ("user_id".equals(name)) {
        loginResponse.setUserId(jsonParser.getLongValue());
      }
      else if ("jti".equals(name)) {
        loginResponse.setJti(jsonParser.getText());
      }
    }

    return loginResponse;
  }

  static Set<String> parseScope(JsonParser jp) throws IOException {
    Set<String> scope;
    if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
      scope = new TreeSet<>();
      while (jp.nextToken() != JsonToken.END_ARRAY) {
        scope.add(jp.getValueAsString());
      }
    } else {
      String text = jp.getText();
      scope = OAuth2Utils.parseParameterList(text);
    }
    return scope;
  }
}
