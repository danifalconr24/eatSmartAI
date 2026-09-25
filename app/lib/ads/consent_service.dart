import 'dart:async';
import 'dart:io';

import 'package:app_tracking_transparency/app_tracking_transparency.dart';
import 'package:flutter/foundation.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

import 'ad_service.dart';

/// Orchestrates GDPR/EEA consent (via AdMob UMP) and iOS App Tracking
/// Transparency before any ads are requested.
class ConsentService {
  ConsentService._();

  static bool _completed = false;

  /// Requests consent and ATT, then initializes the ad SDK.
  ///
  /// Call after the first frame is rendered (e.g. inside a post-frame
  /// callback) so the ATT dialog has a live UI window to attach to. Safe to
  /// call again; subsequent calls no-op.
  ///
  /// On iOS the native App Tracking Transparency prompt is requested first so
  /// no consent message (which may carry a "Consent"-style button) precedes it.
  /// UMP (GDPR/EEA) consent is requested afterwards. On Android the ATT call
  /// is a no-op, so the flow stays UMP-only.
  static Future<void> initialize() async {
    if (_completed) return;

    await _requestTrackingTransparency();
    await _updateConsentInfo();
    await _showFormIfRequired();

    AdService.instance.initialize();
    _completed = true;
  }

  static Future<void> _updateConsentInfo() {
    final completer = Completer<void>();
    ConsentInformation.instance.requestConsentInfoUpdate(
      ConsentRequestParameters(),
      () => completer.complete(),
      (error) {
        debugPrint(
          'ConsentService: consent info update failed: ${error.errorCode} ${error.message}',
        );
        completer.complete();
      },
    );
    return completer.future;
  }

  static Future<void> _showFormIfRequired() async {
    try {
      if (await ConsentInformation.instance.isConsentFormAvailable()) {
        await ConsentForm.loadAndShowConsentFormIfRequired((formError) {
          if (formError != null) {
            debugPrint(
              'ConsentService: form error: ${formError.errorCode} ${formError.message}',
            );
          }
        });
      }
    } catch (e, s) {
      debugPrint('ConsentService: failed to show form: $e\n$s');
    }
  }

  static Future<void> _requestTrackingTransparency() async {
    if (!Platform.isIOS) return;
    try {
      final status = await AppTrackingTransparency.trackingAuthorizationStatus;
      if (status == TrackingStatus.notDetermined) {
        final result = await AppTrackingTransparency.requestTrackingAuthorization();
        debugPrint('ConsentService: ATT result = $result');
      } else {
        debugPrint('ConsentService: ATT status = $status');
      }
    } catch (e, s) {
      debugPrint('ConsentService: ATT failed: $e\n$s');
    }
  }
}
