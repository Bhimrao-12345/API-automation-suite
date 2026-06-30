package com.laerdal.api.clients;

import static io.restassured.RestAssured.given;

import com.laerdal.api.config.EnvConfig;
import com.laerdal.api.config.SpecFactory;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/** Client for {@code GET /tts/v1/voice-configurations}. */
public final class VoiceConfigClient {

    private VoiceConfigClient() {}

    /** Fetch voice configurations using an explicit Bearer token. */
    public static Response getConfigurations(String bearerToken) {
        return given()
                .spec(SpecFactory.request())
                .filter(new AllureRestAssured())
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + bearerToken)
        .when()
                .get(EnvConfig.voiceConfigPath());
    }

    /** Fetch voice configurations using the default tenant's cached token. */
    public static Response getConfigurations() {
        return getConfigurations(AuthClient.defaultToken());
    }

    /** Fetch voice configurations with no Authorization header (negative tests). */
    public static Response getConfigurationsWithoutAuth() {
        return given()
                .spec(SpecFactory.request())
                .filter(new AllureRestAssured())
                .accept(ContentType.JSON)
        .when()
                .get(EnvConfig.voiceConfigPath());
    }
}
