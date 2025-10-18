package fr.flaton.walkietalkie.audio;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Military radio effect with voice processing and noise.
 * Beeps are triggered via callbacks for external sound playback.
 */
public class MilitaryRadioEffect {
    
    private static final int SAMPLE_RATE = 48000; // Voice chat sample rate
    private static final float SILENCE_THRESHOLD = 0.003f; // 0.3% - более чувствительный
    private static final int SILENCE_SAMPLES = (int)(0.3f * SAMPLE_RATE); // 300ms
    
    private final Random random = new Random();
    
    // State tracking
    private boolean isTransmitting = false;
    private int silenceCounter = 0;
    
    // Bandpass filter state - отдельные для каждого канала
    private float lowpassPrev = 0f;
    private float highpassPrev = 0f;
    private float highpassInput = 0f;
    
    // Callbacks for sound events
    private Consumer<Float> onTransmissionStart; // параметр: distance
    private Consumer<Float> onTransmissionEnd;   // параметр: distance
    
    /**
     * Устанавливает callback для начала передачи (для воспроизведения звука включения)
     */
    public void setOnTransmissionStart(Consumer<Float> callback) {
        this.onTransmissionStart = callback;
    }
    
    /**
     * Устанавливает callback для конца передачи (для воспроизведения звука выключения)
     */
    public void setOnTransmissionEnd(Consumer<Float> callback) {
        this.onTransmissionEnd = callback;
    }
    
    public short[] process(short[] input, float distance) {
        if (input == null || input.length == 0) return input;
        
        short[] output = new short[input.length];
        boolean hasVoice = detectVoice(input);
        
        // State machine: начало передачи
        if (hasVoice && !isTransmitting) {
            isTransmitting = true;
            silenceCounter = 0;
            
            // Сбрасываем состояние фильтров для чистого старта
            lowpassPrev = 0f;
            highpassPrev = 0f;
            highpassInput = 0f;
            
            // Триггер звука включения
            if (onTransmissionStart != null) {
                onTransmissionStart.accept(distance);
            }
        }
        
        // Обработка голоса
        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0f;
            
            // Основная обработка голоса
            if (isTransmitting) {
                sample = processSample(sample, distance, hasVoice);
            }
            
            output[i] = (short)Math.max(-32768, Math.min(32767, sample * 32768.0f));
        }
        
        // Детект конца передачи
        if (!hasVoice && isTransmitting) {
            silenceCounter++;
            if (silenceCounter > SILENCE_SAMPLES) {
                isTransmitting = false;
                silenceCounter = 0;
                
                // Триггер звука выключения
                if (onTransmissionEnd != null) {
                    onTransmissionEnd.accept(distance);
                }
            }
        } else if (hasVoice) {
            silenceCounter = 0;
        }
        
        return output;
    }
    
    private boolean detectVoice(short[] input) {
        float sum = 0;
        for (short s : input) {
            sum += Math.abs(s / 32768.0f);
        }
        return (sum / input.length) > SILENCE_THRESHOLD;
    }
    

    
    private float processSample(float sample, float distance, boolean hasVoice) {
        // 1. Bandpass EQ - жесткий, узкий диапазон как в настоящей рации
        float cutoffHigh = 3500f - distance * 1500f; // 3.5kHz -> 2kHz (очень узко)
        float cutoffLow = 400f + distance * 300f; // 400Hz -> 700Hz
        sample = applyBandpass(sample, cutoffLow, cutoffHigh);
        
        // 2. Жесткий distortion
        float drive = 8f + distance * 8f; // 8dB -> 16dB
        sample = applyDistortion(sample, drive);
        
        // 3. Жесткая компрессия
        float ratio = 6f + distance * 6f; // 6:1 -> 12:1
        sample = applyCompressor(sample, 0.03f, ratio);
        
        // 4. Заметный фоновый шум
        float noiseLevel = 0.08f + distance * 0.15f; // 8% -> 23%
        float noise = (random.nextFloat() * 2 - 1) * noiseLevel;
        
        // Ducking: шум падает когда есть голос, но остается слышимым
        if (hasVoice) {
            noise *= 0.4f;
        }
        
        sample += noise * 0.35f;
        
        // Жесткий clipping
        return Math.max(-0.95f, Math.min(0.95f, sample));
    }
    
    private float applyBandpass(float sample, float lowCutoff, float highCutoff) {
        // Highpass filter - убираем низкие частоты
        float alpha_high = (float)Math.exp(-2.0 * Math.PI * lowCutoff / SAMPLE_RATE);
        float highpassOut = alpha_high * (highpassPrev + sample - highpassInput);
        highpassInput = sample;
        highpassPrev = highpassOut;
        sample = highpassOut;
        
        // Lowpass filter - убираем высокие частоты
        float alpha_low = (float)Math.exp(-2.0 * Math.PI * highCutoff / SAMPLE_RATE);
        lowpassPrev = lowpassPrev + alpha_low * (sample - lowpassPrev);
        sample = lowpassPrev;
        
        return sample;
    }
    
    private float applyDistortion(float sample, float driveDB) {
        float drive = (float)Math.pow(10, driveDB / 20.0);
        sample *= drive;
        
        // Жесткий distortion
        sample = (float)Math.tanh(sample * 1.2);
        
        return sample * 0.85f; // Компенсация громкости
    }
    
    private float applyCompressor(float sample, float threshold, float ratio) {
        float abs = Math.abs(sample);
        if (abs > threshold) {
            float excess = abs - threshold;
            float compressed = threshold + excess / ratio;
            sample = sample * (compressed / abs);
        }
        return sample;
    }
    

    
    public void reset() {
        isTransmitting = false;
        silenceCounter = 0;
        lowpassPrev = 0f;
        highpassPrev = 0f;
        highpassInput = 0f;
    }
    
    public boolean isTransmitting() {
        return isTransmitting;
    }
}
