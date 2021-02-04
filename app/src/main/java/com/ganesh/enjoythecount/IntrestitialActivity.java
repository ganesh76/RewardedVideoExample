/*
 * Copyright (C) 2013 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ganesh.enjoythecount;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

/**
 * Main Activity. Inflates main activity xml.
 */
public class IntrestitialActivity extends AppCompatActivity {

    private static final long GAME_LENGTH_MILLISECONDS = 5000;
    private static final String AD_UNIT_ID = "ca-app-pub-1798935924860177/4273918204";

    private InterstitialAd interstitialAd;
    private CountDownTimer countDownTimer;
    private Button retryButton;
    private boolean gameIsInProgress;
    private long timerMilliseconds;
    private AdView adView,adView2,adView3,adView4,adView5;
    private boolean firstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        // Create the InterstitialAd and set the adUnitId.
        interstitialAd = new InterstitialAd(this);
        // Defined in res/values/strings.xml
        interstitialAd.setAdUnitId(AD_UNIT_ID);

    interstitialAd.setAdListener(
        new AdListener() {
          @Override
          public void onAdLoaded() {
           // Toast.makeText(IntrestitialActivity.this, "onAdLoaded()", Toast.LENGTH_SHORT).show();
          }

          @Override
          public void onAdFailedToLoad(LoadAdError loadAdError) {
            String error =
                String.format(
                    "domain: %s, code: %d, message: %s",
                    loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
          //  Toast.makeText(
          //          IntrestitialActivity.this, "onAdFailedToLoad() with error: " + error, Toast.LENGTH_SHORT)
          //      .show();
          }

          @Override
          public void onAdClosed() {
              firstTime = false;
            startGame();
          }
        });

        // Create the "retry" button, which tries to show an interstitial between game plays.
        retryButton = findViewById(R.id.retry_button);
        retryButton.setVisibility(View.INVISIBLE);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInterstitial();
            }
        });

        adView = findViewById(R.id.ad_view);
        adView2 = findViewById(R.id.ad_view2);
        adView3 = findViewById(R.id.ad_view3);
        adView4 = findViewById(R.id.ad_view4);
        adView5 = findViewById(R.id.ad_view5);

        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
        adView2.loadAd(new AdRequest.Builder().build());
        adView3.loadAd(new AdRequest.Builder().build());
        adView4.loadAd(new AdRequest.Builder().build());
        adView5.loadAd(new AdRequest.Builder().build());



        startGame();
    }

    private void createTimer(final long milliseconds) {
        // Create the game timer, which counts down to the end of the level
        // and shows the "retry" button.
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        final TextView textView = findViewById(R.id.timer);

        countDownTimer = new CountDownTimer(milliseconds, 50) {
            @Override
            public void onTick(long millisUnitFinished) {
                timerMilliseconds = millisUnitFinished;
                textView.setText("seconds remaining: " + ((millisUnitFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                gameIsInProgress = false;
                textView.setText("Click Retry to Load Ad!");
                retryButton.setVisibility(View.VISIBLE);
                if(firstTime)
                showInterstitial();
            }
        };
    }

    @Override
    public void onResume() {
        // Start or resume the game.
        super.onResume();

        onResumeAds();

        if (gameIsInProgress) {
            resumeGame(timerMilliseconds);
        }
    }

    @Override
    public void onPause() {
        // Cancel the timer if the game is paused.
        countDownTimer.cancel();
        super.onPause();

        onPauseAds();
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (interstitialAd != null && interstitialAd.isLoaded()) {
            interstitialAd.show();
        } else {
       //     Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
            startGame();
        }
    }

    private void startGame() {
        // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
        if (!interstitialAd.isLoading() && !interstitialAd.isLoaded()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            interstitialAd.loadAd(adRequest);
        }

        retryButton.setVisibility(View.INVISIBLE);
        resumeGame(GAME_LENGTH_MILLISECONDS);
    }

    private void resumeGame(long milliseconds) {
        // Create a new timer for the correct length and start it.
        gameIsInProgress = true;
        timerMilliseconds = milliseconds;
        createTimer(milliseconds);
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        onDestroyAds();
        super.onDestroy();
    }

    private void onPauseAds() {
        if (adView != null) {
            adView.pause();
        }
        if(adView2 != null) {
            adView2.pause();
        }
        if(adView3 != null) {
            adView3.pause();
        }
        if(adView4 != null) {
            adView4.pause();
        }
        if(adView5 != null) {
            adView5.pause();
        }
    }

    private void onResumeAds() {
        if (adView != null) {
            adView.resume();
        }
        if(adView2 != null) {
            adView2.resume();
        }
        if(adView3 != null) {
            adView3.resume();
        }
        if(adView4 != null) {
            adView4.resume();
        }
        if(adView5 != null) {
            adView5.resume();
        }
    }

    private void onDestroyAds() {
        if (adView != null) {
            adView.destroy();
        }
        if(adView2 != null) {
            adView2.destroy();
        }
        if(adView3 != null) {
            adView3.destroy();
        }
        if(adView4 != null) {
            adView4.destroy();
        }
        if(adView5 != null) {
            adView5.destroy();
        }
    }
}
