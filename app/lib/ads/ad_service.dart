import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

/// Central place for AdMob configuration and rewarded-ad orchestration.
///
/// All ad unit IDs (banner + rewarded, Android + iOS) are real production
/// units from the AdMob console.
class AdService {
  AdService._();

  static final AdService instance = AdService._();

  static String get bannerAdUnitId => Platform.isAndroid
      ? 'ca-app-pub-9121163197250140/2673289971'
      : 'ca-app-pub-9121163197250140/8023970375';

  static String get rewardedAdUnitId => Platform.isAndroid
      ? 'ca-app-pub-9121163197250140/5774096033'
      : 'ca-app-pub-9121163197250140/7734044964';

  bool _initialized = false;
  RewardedAd? _rewardedAd;
  bool _loading = false;

  /// Initializes the Mobile Ads SDK and preloads the first rewarded ad.
  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;
    try {
      await MobileAds.instance.initialize();
    } catch (e) {
      debugPrint('AdService: MobileAds init failed: $e');
    }
    preloadRewarded();
  }

  /// Preloads a rewarded ad so it is ready before the next analysis.
  void preloadRewarded() {
    if (_loading || _rewardedAd != null) return;
    _loading = true;
    RewardedAd.load(
      adUnitId: rewardedAdUnitId,
      request: const AdRequest(),
      rewardedAdLoadCallback: RewardedAdLoadCallback(
        onAdLoaded: (ad) {
          _rewardedAd = ad;
          _loading = false;
        },
        onAdFailedToLoad: (error) {
          debugPrint('AdService: rewarded failed to load: $error');
          _rewardedAd = null;
          _loading = false;
        },
      ),
    );
  }

  /// Shows a rewarded ad and waits until it is closed or the reward is earned.
  ///
  /// Returns `true` when the user earned the reward (or the ad could not be
  /// shown, so the flow is not blocked) and `false` only if the user closed
  /// the ad before earning the reward.
  Future<bool> showRewarded() async {
    final ad = _rewardedAd;
    if (ad == null) {
      // No ad available (offline, no fill...): don't block the user.
      preloadRewarded();
      return true;
    }
    _rewardedAd = null;

    final completer = Completer<bool>();
    var earned = false;

    ad.fullScreenContentCallback = FullScreenContentCallback(
      onAdDismissedFullScreenContent: (ad) {
        ad.dispose();
        preloadRewarded();
        if (!completer.isCompleted) completer.complete(earned);
      },
      onAdFailedToShowFullScreenContent: (ad, error) {
        debugPrint('AdService: rewarded failed to show: $error');
        ad.dispose();
        preloadRewarded();
        if (!completer.isCompleted) completer.complete(true);
      },
    );

    try {
      await ad.show(
        onUserEarnedReward: (ad, reward) {
          earned = true;
        },
      );
    } catch (e) {
      debugPrint('AdService: rewarded show threw: $e');
      if (!completer.isCompleted) completer.complete(true);
    }

    return completer.future;
  }

  /// Creates a loaded anchored banner, or `null` if it fails to load.
  Future<BannerAd?> loadBanner() async {
    final completer = Completer<BannerAd?>();
    final banner = BannerAd(
      adUnitId: bannerAdUnitId,
      size: AdSize.banner,
      request: const AdRequest(),
      listener: BannerAdListener(
        onAdLoaded: (ad) {
          if (!completer.isCompleted) completer.complete(ad as BannerAd);
        },
        onAdFailedToLoad: (ad, error) {
          debugPrint('AdService: banner failed to load: $error');
          ad.dispose();
          if (!completer.isCompleted) completer.complete(null);
        },
      ),
    );
    try {
      await banner.load();
    } catch (e) {
      debugPrint('AdService: banner load threw: $e');
      if (!completer.isCompleted) completer.complete(null);
    }
    return completer.future;
  }
}
