package com.rc.metronome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //region Variables
    private Handler handler;
    private Runnable playRunner;
    private LinearLayout mainLayout, settingsLayout;
    private EditText tempo_et;
    private Button playButton, increaseButton, decreaseButton, backButton;
    private ImageButton settingsButton;
    private RadioGroup beatSoundGroup, timeSigGroup;

    private SoundPool soundPool;
    private int[] beepSound;
    private int[] setSound;
    private int[] blockSound;
    private int[] snareSound;

    private int counter;
    private int time_signature = 4;
    private int MAX_TEMPO = 250, MIN_TEMPO = 30;

    private boolean running, loadComplete;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Objects.requireNonNull(getSupportActionBar()).hide();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //region SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        beepSound = new int[2]; // I created with free synth software
        beepSound[0] = soundPool.load(this, R.raw.beep_accent, 1);
        beepSound[1] = soundPool.load(this, R.raw.beep_standard, 1);

        blockSound = new int[2]; // Royalty free from bigsoundbank.com
        blockSound[0] = soundPool.load(this, R.raw.block_1, 1);
        blockSound[1] = soundPool.load(this, R.raw.block_2, 1);

        snareSound = new int[2]; // Royalty free from bigsoundbank.com
        snareSound[0] = soundPool.load(this, R.raw.snare_acc, 1);
        snareSound[1] = soundPool.load(this, R.raw.snare1, 1);

        setSound = new int[2];
        setSound = blockSound;
        //endregion


        handler = new Handler(Looper.getMainLooper());

        //region FindViews
        tempo_et = findViewById(R.id.Tempo_ET);
        playButton = findViewById(R.id.PlayButton);
        increaseButton = findViewById(R.id.IncreaseButton);
        decreaseButton = findViewById(R.id.DecreaseButton);
        mainLayout = findViewById(R.id.MainLayout);
        settingsLayout = findViewById(R.id.SettingsLayout);
        settingsButton = findViewById(R.id.SettingsButton);
        backButton = findViewById(R.id.BackButtton);
        beatSoundGroup = findViewById(R.id.BeatSoundGroup);
        timeSigGroup = findViewById(R.id.TimeSigGroup);
        //endregion

        counter = 0;
        //region ClickListeners
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //This ensures that while the metronome is running, the screen will stay on
                if (v.getKeepScreenOn()) {
                    v.setKeepScreenOn(false);
                } else {
                    v.setKeepScreenOn(true);
                }
                PlayStopButton();
            }
        });

        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunCheckToStop();
                Increase();
            }
        });

        increaseButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                RunCheckToStop();
                IncreaseHold();
                return true;
            }
        });

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunCheckToStop();
                Decrease();
            }
        });

        decreaseButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                RunCheckToStop();
                DecreaseHold();
                return true;
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunCheckToStop();
                SettingsActive();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActive();
            }
        });
        //endregion

        timeSigGroup.check(R.id.rb_four_four);
        beatSoundGroup.check(R.id.rb_block);
    }


    /**
     * Sets the time signature depending on which RadioButton has been selected
     */
    public void TimeSigRadioButtonClick(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.rb_one:
                if (checked) {
                    time_signature = 1;
                }
                break;
            case R.id.rb_three_four:
                if (checked) {
                    time_signature = 3;
                }
                break;
            case R.id.rb_four_four:
                if (checked) {
                    time_signature = 4;
                }
                break;
            case R.id.rb_five_four:
                if (checked) {
                    time_signature = 5;
                }
                break;
        }
    }

    /**
     * Sets the sound for the beat depending on which RadioButton has been selected.
     */
    public void SoundRadioButtonClick(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.rb_beep:
                if (checked) {
                    setSound = beepSound;
                }
                break;
            case R.id.rb_block:
                if (checked) {
                    setSound = blockSound;
                }
                break;
            case R.id.rb_snare:
                if (checked) {
                    setSound = snareSound;
                }
                break;
        }
    }


    /**
     * Checks if the Metronome is running and stops if true.
     */
    private void RunCheckToStop() {
        if (running) {
            playButton.setText(R.string.start_button);
            handler.removeCallbacks(playRunner);
            Stop();
        }
    }

    /**
     * Checks if the input tempo_et contains a number lower than the maxTempo and increments the
     * input value if below maxTempo.
     */
    private void Increase() {
        if (!tempo_et.getText().toString().equals("") && Integer.parseInt(tempo_et.getText().toString()) < MAX_TEMPO) {

            int tempo = Integer.parseInt(tempo_et.getText().toString());

            if (tempo < MAX_TEMPO) {
                int newTempo = tempo + 1;
                tempo_et.setText(String.valueOf(newTempo));
            }
        } else {
            Toast.makeText(getApplicationContext(), "Maximum BPM reached", Toast.LENGTH_SHORT).show();
        }
        SetTempoIfExceedsLimit();
    }

    /**
     * Checks if the input tempo_et contains a number higher than the minTempo and decrements the
     * input value if above minTempo.
     */
    private void Decrease() {
        if (!tempo_et.getText().toString().equals("") && Integer.parseInt(tempo_et.getText().toString()) > MIN_TEMPO) {
            int tempo = Integer.parseInt(tempo_et.getText().toString());

            if (tempo > MIN_TEMPO) {
                int newTempo = tempo - 1;
                tempo_et.setText(String.valueOf(newTempo));
            }
        } else {
            Toast.makeText(getApplicationContext(), "Minimum BPM reached", Toast.LENGTH_SHORT).show();
        }
        SetTempoIfExceedsLimit();
    }

    /**
     * Calls the Increase() method repeatedly when the button is held and stops when maxTempo is reached.
     */
    private void IncreaseHold() {

        handler.post(new Runnable() {

            public void run() {
                if (Integer.parseInt(tempo_et.getText().toString()) < MAX_TEMPO && increaseButton.isPressed()) {

                    handler.postDelayed(this, 25);

                    Increase();
                }
            }
        });
        SetTempoIfExceedsLimit();
    }

    /**
     * Calls the Decrease() method repeatedly when the button is held and stops when minTempo is reached.
     */
    private void DecreaseHold() {

        handler.post(new Runnable() {

            public void run() {
                if (Integer.parseInt(tempo_et.getText().toString()) > MIN_TEMPO && decreaseButton.isPressed()) {

                    handler.postDelayed(this, 25);
                    Decrease();
                }
            }
        });
        SetTempoIfExceedsLimit();
    }

    /**
     * Ensures that if user inputs a number out of limits and tries presses the increase or decrease
     * buttons, the tempo is auto set to the closest limit.
     */
    private void SetTempoIfExceedsLimit() {
        if (Integer.parseInt(tempo_et.getText().toString()) > MAX_TEMPO) {
            tempo_et.setText(String.valueOf(MAX_TEMPO));
            Toast.makeText(getApplicationContext(), "Maximum Tempo is " + MAX_TEMPO, Toast.LENGTH_SHORT).show();
        } else if (Integer.parseInt(tempo_et.getText().toString()) < MIN_TEMPO) {
            tempo_et.setText(String.valueOf(MIN_TEMPO));
            Toast.makeText(getApplicationContext(), "Minimum Tempo is " + MIN_TEMPO, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calculates the frequency of run calls using the value of the input tempo_et. Each call
     * increments the int counter and plays a sound.
     */
    private void Play() {

        final float bpm = Float.parseFloat(tempo_et.getText().toString());
        final float beat = (60 / bpm);
        final long bpMillis = (long) (beat * 1000);

        running = true;

        playRunner = new Runnable() {

            public void run() {

                if (running) {

                    handler.postDelayed(this, bpMillis);

                    counter++;

                    switch (time_signature) {

                        case 5:
                            if (counter == 1) {
                                soundPool.play(setSound[0], 1, 1, 0, 0, 1);
                            } else if (counter == 2 || counter == 3 || counter == 4) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                            } else if (counter == 5) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                                counter = 0;
                            }
                            break;
                        case 1:
                            soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                            counter = 0;
                            break;
                        case 3:
                            if (counter == 1) {
                                soundPool.play(setSound[0], 1, 1, 0, 0, 1);
                            } else if (counter == 2) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                            } else if (counter == 3) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                                counter = 0;
                            }
                            break;
                        case 4:
                            if (counter == 1) {
                                soundPool.play(setSound[0], 1, 1, 0, 0, 1);
                            } else if (counter == 2 || counter == 3) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                            } else if (counter == 4) {
                                soundPool.play(setSound[1], 1, 1, 0, 0, 1);
                                counter = 0;
                            }
                            break;
                    }
                }
            }
        };
        handler.post(playRunner);
    }

    /**
     * Swaps Main and Settings Layout visibility.
     */
    private void SettingsActive() {
        settingsLayout.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * Swaps Main and Settings Layout visibility.
     */
    private void MainActive() {
        settingsLayout.setVisibility(View.INVISIBLE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Stops the soundpool, sets running to false, and resets the int counter.
     */
    private void Stop() {
        running = false;
        counter = 0;
        handler.removeCallbacks(playRunner);
    }

    /**
     * Checks if the Metronome is running - if it is not running the PLay method is called and the
     * Play button switched with the Stop button. If it is already the Stop button, the Stop method
     * is called and button switched back to PLay button. The boolean running is alternated each call.
     */
    private void PlayStopButton() {

        if (!running) {
            if (!tempo_et.getText().toString().equals("") && Integer.parseInt(tempo_et.getText().toString()) >= MIN_TEMPO && Integer.parseInt(tempo_et.getText().toString()) <= MAX_TEMPO) {
                playButton.setText(R.string.stop_button);
                Play();
            } else {
                Toast.makeText(getApplicationContext(), "Please enter BPM between " + MIN_TEMPO + " and " + MAX_TEMPO, Toast.LENGTH_SHORT).show();
            }
        } else {
            playButton.setText(R.string.start_button);
            handler.removeCallbacks(playRunner);
            Stop();
        }
    }

    //region On... Override Methods
    @Override
    public void onBackPressed() {
        RunCheckToStop();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        RunCheckToStop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        RunCheckToStop();
        soundPool.release();
        soundPool = null;
        super.onDestroy();
    }
    //endregion
}