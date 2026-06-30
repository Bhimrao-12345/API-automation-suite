package com.laerdal.api.clients;

import static io.restassured.RestAssured.given;

import com.laerdal.api.config.EnvConfig;
import com.laerdal.api.config.SpecFactory;
import com.laerdal.api.model.TtsRequest;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.response.Response;

/**
 * Client for {@code POST /tts/v1/tts-stream}. Returns audio bytes
 * ({@code application/octet-stream}).
 */
public final class TtsStreamClient {

    private TtsStreamClient() {}

    /** Synthesize using an explicit bearer token and a {@link TtsRequest} body. */
    public static Response synthesize(String bearerToken, TtsRequest body) {
        return given()
                .spec(SpecFactory.request())
                .filter(new AllureRestAssured())
                .accept("*/*")
                .header("Authorization", "Bearer " + bearerToken)
                .body(body)
        .when()
                .post(EnvConfig.ttsStreamPath());
    }

    /** Synthesize using the default tenant's cached token. */
    public static Response synthesize(TtsRequest body) {
        return synthesize(AuthClient.defaultToken(), body);
    }

    /** Call the endpoint with no Authorization header (negative tests). */
    public static Response synthesizeWithoutAuth(TtsRequest body) {
        return given()
                .spec(SpecFactory.request())
                .filter(new AllureRestAssured())
                .accept("*/*")
                .body(body)
        .when()
                .post(EnvConfig.ttsStreamPath());
    }
}
