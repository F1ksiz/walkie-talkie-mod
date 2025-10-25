package fr.flaton.walkietalkie.audio;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Military radio effect with voice processing and noise.
 * Beeps are triggered via callbacks for external sound playback.
 */
public class PhysicalRadioEffect {
    
    private static final int SAMPLE_RATE = 48000; // Voice chat sample rate
    private static final float SILENCE_THRESHOLD = 0.003f; // 0.3% - более чувствительный
    private static final int SILENCE_SAMPLES = (int)(0.3f * SAMPLE_RATE); // 300ms
    
    private final Random random = new Random();

    public short[] process(short[] input, float distance) {
        short[] output = input.clone();
        return output;
    }
    
    private boolean detectVoice(short[] input) {
        float sum = 0;
        for (short s : input) {
            sum += Math.abs(s / 32768.0f);
        }
        return (sum / input.length) > SILENCE_THRESHOLD;
    }
}
