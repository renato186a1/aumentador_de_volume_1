package com.opensource.volumebooster;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.util.Log;

public class AudioEngine {
    private static final String TAG = "VolumeBoosterFree";
    private LoudnessEnhancer loudnessEnhancer;
    private Equalizer equalizer;
    private int currentBoostPercent = 0;
    private boolean nonUniformShape = true;

    public AudioEngine() {
        initEffects();
    }

    private void initEffects() {
        try {
            // Sessao 0 aplica o efeito na mixagem global de saida do Android
            loudnessEnhancer = new LoudnessEnhancer(0);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar LoudnessEnhancer: " + e.getMessage());
        }

        try {
            equalizer = new Equalizer(0, 0);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar Equalizer: " + e.getMessage());
        }
    }

    public void applySettings(SharedPreferences prefs) {
        this.currentBoostPercent = prefs.getInt("boost_percent", 0);
        this.nonUniformShape = prefs.getBoolean("shape", true);
        updateAudioEffects();
    }

    public void updateAudioEffects() {
        if (currentBoostPercent <= 0) {
            disableAll();
            return;
        }

        try {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = new LoudnessEnhancer(0);
            }
            // Converte porcentagem de boost (ex: 0 a 200%) em millibels (mB)
            // No GOODEV original, 100% equivale a ~2500-3000 mB de TargetGain
            int targetGainMb = currentBoostPercent * 30;
            loudnessEnhancer.setTargetGain(targetGainMb);
            if (!loudnessEnhancer.getEnabled()) {
                loudnessEnhancer.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao aplicar LoudnessEnhancer: " + e.getMessage());
        }

        try {
            if (equalizer != null) {
                if (nonUniformShape) {
                    short bands = equalizer.getNumberOfBands();
                    short[] range = equalizer.getBandLevelRange();
                    short minLevel = range[0];
                    short maxLevel = range[1];

                    for (short i = 0; i < bands; i++) {
                        int centerFreqMilliHz = equalizer.getCenterFreq(i);
                        int freqHz = centerFreqMilliHz / 1000;
                        // Reduz graves extremos (<150Hz) e agudos (>8000Hz) para evitar estourar o alto-falante
                        if (freqHz < 150) {
                            short cut = (short) Math.max(minLevel / 2, -600);
                            equalizer.setBandLevel(i, cut);
                        } else if (freqHz >= 1000 && freqHz <= 4000) {
                            // Destaca frequencias de voz/clareza
                            short boost = (short) Math.min(maxLevel / 2, 400);
                            equalizer.setBandLevel(i, boost);
                        } else {
                            equalizer.setBandLevel(i, (short) 0);
                        }
                    }
                    if (!equalizer.getEnabled()) {
                        equalizer.setEnabled(true);
                    }
                } else {
                    if (equalizer.getEnabled()) {
                        equalizer.setEnabled(false);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao configurar Equalizer: " + e.getMessage());
        }
    }

    public void disableAll() {
        try {
            if (loudnessEnhancer != null) {
                loudnessEnhancer.setTargetGain(0);
                loudnessEnhancer.setEnabled(false);
            }
        } catch (Exception ignored) {}

        try {
            if (equalizer != null && equalizer.getEnabled()) {
                equalizer.setEnabled(false);
            }
        } catch (Exception ignored) {}
    }

    public void release() {
        disableAll();
        try {
            if (loudnessEnhancer != null) {
                loudnessEnhancer.release();
                loudnessEnhancer = null;
            }
        } catch (Exception ignored) {}

        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }
        } catch (Exception ignored) {}
    }
}
