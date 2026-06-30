package com.laerdal.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body for {@code POST /tts/v1/tts}.
 *
 * <pre>{@code
 * { "voice": "Female_Aria_Hopeful_LowPitch", "speechText": "..." }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TtsRequest {

    public String voice;
    public String speechText;

    public TtsRequest() {
    }

    public TtsRequest(String voice, String speechText) {
        this.voice = voice;
        this.speechText = speechText;
    }
}
