package fr.flaton.walkietalkie.audio;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.function.Consumer;

/**
 * Processes audio packets to apply military radio effects
 */
public class AudioProcessor {

    private final MilitaryRadioEffect radioEffect;
    private Consumer<Float> onTransmissionStart;
    private Consumer<Float> onTransmissionEnd;

    public AudioProcessor() {
        this.radioEffect = new MilitaryRadioEffect();
    }
    
    /**
     * Set callback for transmission start (for playing beep sound)
     */
    public void setOnTransmissionStart(Consumer<Float> callback) {
        this.onTransmissionStart = callback;
        this.radioEffect.setOnTransmissionStart(callback);
    }
    
    /**
     * Set callback for transmission end (for playing beep sound)
     */
    public void setOnTransmissionEnd(Consumer<Float> callback) {
        this.onTransmissionEnd = callback;
        this.radioEffect.setOnTransmissionEnd(callback);
    }

    /**
     * Process opus-encoded audio data with military radio effect
     * @param opusData The opus-encoded audio data
     * @param decoder Opus decoder instance
     * @param encoder Opus encoder instance
     * @param distance Distance to listener (0-1, where 1 is far)
     * @return Processed opus-encoded audio data
     */
    public byte[] processOpusAudio(byte[] opusData, OpusDecoder decoder, OpusEncoder encoder, float distance) {
        try {
            // Decode opus to PCM
            short[] pcmData = decoder.decode(opusData);
            
            if (pcmData == null || pcmData.length == 0) {
                return opusData;
            }
            
            // Apply military radio effect with distance
            short[] processed = radioEffect.process(pcmData, distance);
            
            // Encode back to opus
            byte[] encodedData = encoder.encode(processed);
            
            return encodedData != null ? encodedData : opusData;
            
        } catch (Exception e) {
            return opusData;
        }
    }

    /**
     * Process without distance parameter (defaults to 0)
     */
    public byte[] processOpusAudio(byte[] opusData, OpusDecoder decoder, OpusEncoder encoder) {
        return processOpusAudio(opusData, decoder, encoder, 0f);
    }

    /**
     * Reset the audio processor state
     */
    public void reset() {
        radioEffect.reset();
    }
}
