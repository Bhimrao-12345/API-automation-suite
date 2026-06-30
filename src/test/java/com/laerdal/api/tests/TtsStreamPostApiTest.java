package com.laerdal.api.tests;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.laerdal.api.clients.TtsStreamClient;
import com.laerdal.api.model.TtsRequest;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Feature("TTS STREAM POST API")
@Requirement({"LBVOICESER-1335"})
@XrayTest(key = "LBVOICESER-1353")
class TtsStreamPostApiTest {

    @Test
    @DisplayName("No Authorization header returns 400")
    void noAuthHeaderReturns400() {
        TtsStreamClient.synthesizeWithoutAuth(new TtsRequest("en-US", "Hello, this is a test."))
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Empty JSON body returns 400")
    void emptyBodyReturns400() {
        TtsStreamClient.synthesize(new TtsRequest())
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Missing voice field returns 200")
    void missingVoiceFieldReturns200() {
        TtsStreamClient.synthesize(new TtsRequest(null, "Hello, this is a test."))
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Missing speechText field returns 200")
    void missingSpeechTextFieldReturns200() {
        TtsStreamClient.synthesize(new TtsRequest("en-US", null))
                .then().statusCode(200);
    }
}
