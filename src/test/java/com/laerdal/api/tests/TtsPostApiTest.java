package com.laerdal.api.tests;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import app.getxray.xray.junit.customjunitxml.annotations.XrayTest;
import com.laerdal.api.clients.TtsClient;
import com.laerdal.api.model.TtsRequest;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Feature("TTS POST API")
@Requirement({"LBVOICESER-1330"})
@XrayTest(key = "LBVOICESER-1352")
class TtsPostApiTest {

    @Test
    @DisplayName("No Authorization header returns 400")
    void noAuthHeaderReturns400() {
        TtsClient.synthesizeWithoutAuth(new TtsRequest("en-US", "Hello, this is a test."))
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Empty JSON body returns 400")
    void emptyBodyReturns400() {
        TtsClient.synthesize(new TtsRequest())
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Missing voice field returns 400")
    void missingVoiceFieldReturns400() {
        TtsClient.synthesize(new TtsRequest(null, "Hello, this is a test."))
                .then().statusCode(400);
    }

    @Test
    @DisplayName("Missing speechText field returns 400")
    void missingSpeechTextFieldReturns400() {
        TtsClient.synthesize(new TtsRequest("en-US", null))
                .then().statusCode(400);
    }
}
