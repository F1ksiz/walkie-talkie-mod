package fr.flaton.walkietalkie.audio;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.Arrays;

public class BucketBasedProcessor {
    private static final int BUCKETS_COUNT = 11; // 0.0, 0.1, ..., 1.0

    private final byte[][] opusCache;           // Кэш обработанных opus по корзинам
    private final short[] basePcm;              // Декодированный исходный PCM
    private final byte[] originalOpus;          // Оригинальные Opus-данные на случай отката
    private final OpusEncoder encoder;          // Повторное использование encoder на пакет

    public BucketBasedProcessor(byte[] opusData, OpusDecoder decoder, OpusEncoder encoder) {
        this.opusCache = new byte[BUCKETS_COUNT][];
        this.originalOpus = opusData != null ? Arrays.copyOf(opusData, opusData.length) : null;
        this.encoder = encoder;
        short[] decoded = null;
        try {
            decoded = (decoder != null && opusData != null) ? decoder.decode(opusData) : null;
        } catch (Exception ignored) {
        }
        this.basePcm = decoded; // может быть null
    }

    private static int bucketIndex(float f) {
        // f ожидается в [0,1]; зажмем на всякий случай
        float clamped = Math.max(0f, Math.min(1f, f));
        int idx = (int) Math.floor(clamped * (BUCKETS_COUNT - 1));
        return Math.max(0, Math.min(BUCKETS_COUNT - 1, idx));
    }

    /**
     * Возвращает Opus-данные, обработанные с учетом дистанции. Результаты кэшируются по корзинам.
     */
    public byte[] process(float distance) {
        int idx = bucketIndex(distance);
        byte[] cached = opusCache[idx];
        if (cached != null) {
            return cached;
        }

        // Если не удалось декодировать — вернем исходник
        if (basePcm == null || basePcm.length == 0 || encoder == null) {
            return originalOpus;
        }

        // Для каждого бакета создаем отдельный эффект (изолированное состояние)
        PhysicalRadioEffect effect = new PhysicalRadioEffect();
        short[] processedPcm = effect.process(basePcm, Math.max(0f, Math.min(1f, distance)));

        byte[] encoded;
        try {
            encoded = encoder.encode(processedPcm);
        } catch (Exception e) {
            encoded = null;
        }

        if (encoded == null) {
            encoded = originalOpus;
        }

        opusCache[idx] = encoded;
        return encoded;
    }
}
