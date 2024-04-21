package com.apus.gateway.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static com.apus.gateway.api.dto.LoginResponseDeserializer.parseScope;

public class ClientLoginResponseDeserializer extends StdDeserializer<ClientLoginResponse> {

  protected ClientLoginResponseDeserializer() {
    super(ClientLoginResponse.class);
  }

  @Override
  public ClientLoginResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    ClientLoginResponse loginResponse = new ClientLoginResponse();

    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      String name = jsonParser.getCurrentName();
      jsonParser.nextToken();
      if ("access_token".equals(name)) {
        loginResponse.setAccessToken(jsonParser.getText());
      } else if ("token_type".equals(name)) {
        loginResponse.setTokenType(jsonParser.getText());
      } else if ("expires_in".equals(name)) {
        loginResponse.setExpiresIn(jsonParser.getLongValue());
      } else if ("scope".equals(name)) {
        loginResponse.setScopes(parseScope(jsonParser));
      } else if ("jti".equals(name)) {
        loginResponse.setJti(jsonParser.getText());
      }
    }

    return loginResponse;
  }
}
