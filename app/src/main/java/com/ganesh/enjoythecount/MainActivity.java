package com.ganesh.enjoythecount;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

/** Main Activity. Inflates main activity xml. */
public class MainActivity extends AppCompatActivity {
  private static final String AD_UNIT_ID = "ca-app-pub-1798935924860177/4272249901";
  private static final long COUNTER_TIME = 10;
  private static final int GAME_OVER_REWARD = 1;
    private static final String TAG = "MainActivity";
    private int coinCount = 0;
  private TextView coinCountText;
  private CountDownTimer countDownTimer;
  private boolean gameOver;
  private boolean gamePaused;

  private RewardedAd rewardedAd;
  private Button retryButton;
  private Button showVideoButton;
  private long timeRemaining;
  boolean isLoading;
  private AdView adView,adView2,adView3;
  private boolean firstTime = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          // Create channel to show notifications.
          String channelId  = "default_channel_id";
          String channelName = "default_channel_name";
          NotificationManager notificationManager =
                  getSystemService(NotificationManager.class);
          notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                  channelName, NotificationManager.IMPORTANCE_LOW));
      }

      FirebaseMessaging.getInstance().getToken()
              .addOnCompleteListener(new OnCompleteListener<String>() {
                  @Override
                  public void onComplete(@NonNull Task<String> task) {
                      if (!task.isSuccessful()) {
                          Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                          return;
                      }

                      // Get new FCM registration token
                      String token = task.getResult();

                      // Log and toast
                      //String msg = getString(R.string.msg_token_fmt, token);
                      Log.d(TAG, "Token recieved: "+token);
                    //  Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                  }
              });




      MobileAds.initialize(this, new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {
      }
    });

    loadRewardedAd();

    // Create the "retry" button, which tries to show a rewarded ad between game plays.
    retryButton = findViewById(R.id.retry_button);
    retryButton.setVisibility(View.INVISIBLE);
    retryButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startGame();
          }
        });

    // Create the "show" button, which shows a rewarded video if one is loaded.
    showVideoButton = findViewById(R.id.show_video_button);
    showVideoButton.setVisibility(View.INVISIBLE);
    showVideoButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showRewardedVideo();
          }
        });

    // Display current coin count to user.
    coinCountText = findViewById(R.id.coin_count_text);
    coinCount = 0;
    coinCountText.setText(getString(R.string.default_coin_text) + coinCount);

    findViewById(R.id.show_adv_button).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(MainActivity.this,NativeAdvancedActivity.class);
            startActivity(i);
        }
    });

      findViewById(R.id.show_int_button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              Intent i = new Intent(MainActivity.this,IntrestitialActivity.class);
              startActivity(i);
          }
      });

      findViewById(R.id.show_banner_button).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              Intent i = new Intent(MainActivity.this,BannerActivity.class);
              startActivity(i);
          }
      });

      adView = findViewById(R.id.ad_view);
      adView2 = findViewById(R.id.ad_view2);
      adView3 = findViewById(R.id.ad_view3);

      // Create an ad request.
      AdRequest adRequest = new AdRequest.Builder().build();

      // Start loading the ad in the background.
      adView.loadAd(adRequest);
      adView2.loadAd(new AdRequest.Builder().build());
      adView3.loadAd(new AdRequest.Builder().build());

    startGame();
  }

  @Override
  public void onPause() {
    super.onPause();
      onPauseAds();
    pauseGame();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!gameOver && gamePaused) {
      resumeGame();
    }

      onResumeAds();
  }

    @Override
    protected void onDestroy() {
      onDestroyAds();
      super.onDestroy();
    }

    private void pauseGame() {
    countDownTimer.cancel();
    gamePaused = true;
  }

  private void resumeGame() {
    createTimer(timeRemaining);
    gamePaused = false;
  }

  private void loadRewardedAd() {
    if (rewardedAd == null || !rewardedAd.isLoaded()) {
      rewardedAd = new RewardedAd(this, AD_UNIT_ID);
      isLoading = true;
      rewardedAd.loadAd(
          new AdRequest.Builder().build(),
          new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
              // Ad successfully loaded.
              MainActivity.this.isLoading = false;
            //  Toast.makeText(MainActivity.this, "onRewardedAdLoaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
              // Ad failed to load.
              MainActivity.this.isLoading = false;
                Log.e("loadAdError : ",loadAdError.getMessage());
              //Toast.makeText(MainActivity.this, "onRewardedAdFailedToLoad", Toast.LENGTH_SHORT)
               //   .show();
            }
          });
    }
  }

  private void addCoins(int coins) {
    coinCount += coins;
    coinCountText.setText(getString(R.string.default_coin_text) + coinCount);
  }

  private void startGame() {
    // Hide the retry button, load the ad, and start the timer.
    retryButton.setVisibility(View.INVISIBLE);
    showVideoButton.setVisibility(View.INVISIBLE);
    if (!rewardedAd.isLoaded() && !isLoading) {
      loadRewardedAd();
    }
    createTimer(COUNTER_TIME);
    gamePaused = false;
    gameOver = false;
  }

  // Create the game timer, which counts down to the end of the level
  // and shows the "retry" button.
  private void createTimer(long time) {
    final TextView textView = findViewById(R.id.timer);
    if (countDownTimer != null) {
      countDownTimer.cancel();
    }
    countDownTimer =
        new CountDownTimer(time * 1000, 50) {
          @Override
          public void onTick(long millisUnitFinished) {
            timeRemaining = ((millisUnitFinished / 1000) + 1);
            textView.setText("seconds remaining: " + timeRemaining);
          }

          @Override
          public void onFinish() {
            if (rewardedAd.isLoaded()) {
              showVideoButton.setVisibility(View.VISIBLE);
              if(firstTime) {
                  showVideoButton.performClick();
              }
            }
            textView.setText("Click Video Ad for more coins!");
            addCoins(GAME_OVER_REWARD);
            retryButton.setVisibility(View.VISIBLE);
            gameOver = true;
          }
        };
    countDownTimer.start();
  }

  private void showRewardedVideo() {
    showVideoButton.setVisibility(View.INVISIBLE);
    if (rewardedAd.isLoaded()) {
      RewardedAdCallback adCallback =
          new RewardedAdCallback() {
            @Override
            public void onRewardedAdOpened() {
              // Ad opened.
             // Toast.makeText(MainActivity.this, "onRewardedAdOpened", Toast.LENGTH_SHORT).show();
                firstTime = false;
            }

            @Override
            public void onRewardedAdClosed() {
              // Ad closed.
           //   Toast.makeText(MainActivity.this, "onRewardedAdClosed", Toast.LENGTH_SHORT).show();
              // Preload the next video ad.
              MainActivity.this.loadRewardedAd();
                firstTime = false;
                startGame();
            }

            @Override
            public void onUserEarnedReward(RewardItem rewardItem) {
              // User earned reward.
         //     Toast.makeText(MainActivity.this, "onUserEarnedReward", Toast.LENGTH_SHORT).show();
              addCoins(rewardItem.getAmount());
                firstTime = false;
                startGame();
            }

            @Override
            public void onRewardedAdFailedToShow(AdError adError) {
             Log.e("adderr : ",adError.getMessage());
       //       Toast.makeText(MainActivity.this, "onRewardedAdFailedToShow", Toast.LENGTH_SHORT)
       //           .show();
            }
          };
      rewardedAd.show(this, adCallback);
    }
  }

  private void onPauseAds() {
      if (adView != null) {
          adView.pause();
      }
      if(adView2 != null) {
          adView2.pause();
      }if(adView3 != null) {
          adView3.pause();
      }
  }

    private void onResumeAds() {
        if (adView != null) {
            adView.resume();
        }
        if(adView2 != null) {
            adView2.resume();
        }if(adView3 != null) {
            adView3.resume();
        }
    }

    private void onDestroyAds() {
        if (adView != null) {
            adView.destroy();
        }
        if(adView2 != null) {
            adView2.destroy();
        }if(adView3 != null) {
            adView3.destroy();
        }
    }
}
