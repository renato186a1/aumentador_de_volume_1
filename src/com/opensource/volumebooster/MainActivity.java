package com.opensource.volumebooster;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private AudioManager audioManager;
    private SeekBar sbVol;
    private SeekBar sbBoost;
    private TextView tvVol;
    private TextView tvBoost;
    private Button btnLimit;
    private int maxAllowedBoost = 60;
    private final int[] limitSteps = new int[]{40, 60, 100, 150, 200};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("booster_prefs", Context.MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        tvVol = findViewById(R.id.tv_vol);
        tvBoost = findViewById(R.id.tv_boost);
        sbVol = findViewById(R.id.sb_vol);
        sbBoost = findViewById(R.id.sb_boost);
        btnLimit = findViewById(R.id.btn_limit);
        Button btnStop = findViewById(R.id.btn_stop);
        CheckBox cbShape = findViewById(R.id.cb_shape);
        CheckBox cbBoot = findViewById(R.id.cb_boot);

        // Solicita permissao de notificacao no Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
            }
        }

        maxAllowedBoost = prefs.getInt("max_boost", 60);
        int currentBoost = prefs.getInt("boost_percent", 0);
        if (currentBoost > maxAllowedBoost) {
            currentBoost = maxAllowedBoost;
        }

        sbBoost.setMax(maxAllowedBoost);
        sbBoost.setProgress(currentBoost);
        updateBoostLabel(currentBoost);
        btnLimit.setText("Máx: " + maxAllowedBoost + "%");

        cbShape.setChecked(prefs.getBoolean("shape", true));
        cbBoot.setChecked(prefs.getBoolean("auto_boot", false));

        syncSystemVolume();

        // Controle da barra de Volume do Sistema
        sbVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioManager != null) {
                    int maxSys = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int targetVol = Math.round((progress * maxSys) / 100.0f);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);
                    tvVol.setText("Vol.: " + progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Controle da barra de Boost (LoudnessEnhancer)
        sbBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateBoostLabel(progress);
                if (fromUser) {
                    prefs.edit().putInt("boost_percent", progress).apply();
                    triggerBoostService();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Alternador rapido de Limite Maximo (40% -> 60% -> 100% -> 150% -> 200%)
        btnLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nextLimit = 60;
                for (int i = 0; i < limitSteps.length; i++) {
                    if (limitSteps[i] == maxAllowedBoost) {
                        nextLimit = limitSteps[(i + 1) % limitSteps.length];
                        break;
                    }
                }
                maxAllowedBoost = nextLimit;
                prefs.edit().putInt("max_boost", maxAllowedBoost).apply();
                btnLimit.setText("Máx: " + maxAllowedBoost + "%");

                int cur = sbBoost.getProgress();
                if (cur > maxAllowedBoost) {
                    cur = maxAllowedBoost;
                    prefs.edit().putInt("boost_percent", cur).apply();
                }
                sbBoost.setMax(maxAllowedBoost);
                sbBoost.setProgress(cur);
                updateBoostLabel(cur);
                triggerBoostService();
            }
        });

        // Botao Desligar Boost e Fechar Servico
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putInt("boost_percent", 0).apply();
                sbBoost.setProgress(0);
                updateBoostLabel(0);
                Intent stopIntent = new Intent(MainActivity.this, BoostService.class);
                stopIntent.setAction("STOP_BOOST");
                startService(stopIntent);
                finish();
            }
        });

        cbShape.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("shape", isChecked).apply();
                triggerBoostService();
            }
        });

        cbBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("auto_boot", isChecked).apply();
            }
        });
    }

    private void syncSystemVolume() {
        if (audioManager != null) {
            int maxSys = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curSys = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int pct = maxSys > 0 ? Math.round((curSys * 100.0f) / maxSys) : 0;
            sbVol.setProgress(pct);
            tvVol.setText("Vol.: " + pct + "%");
        }
    }

    private void updateBoostLabel(int boost) {
        tvBoost.setText("Boost: " + boost + "%");
    }

    private void triggerBoostService() {
        Intent serviceIntent = new Intent(this, BoostService.class);
        int boost = prefs.getInt("boost_percent", 0);
        if (boost > 0) {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            stopService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSystemVolume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            sbVol.postDelayed(new Runnable() {
                @Override
                public void run() {
                    syncSystemVolume();
                }
            }, 100);
        }
        return super.onKeyDown(keyCode, event);
    }
}
