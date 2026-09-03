import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

import '../ads/ad_service.dart';

/// Muestra un banner de placeholder (etiqueta "Ad") mientras el anuncio real
/// no está disponible (cargando o falló). Sobreescribible con:
/// flutter run --dart-define=SHOW_AD_PLACEHOLDER=false
const bool kShowAdPlaceholder = bool.fromEnvironment(
  'SHOW_AD_PLACEHOLDER',
  defaultValue: true,
);

/// Banner de AdMob reutilizable. Muestra un placeholder hasta que el anuncio
/// se carga; si la carga falla (p. ej. sin conexión), mantiene el placeholder
/// para previsualizar el diseño.
class BannerAdWidget extends StatefulWidget {
  const BannerAdWidget({super.key});

  @override
  State<BannerAdWidget> createState() => _BannerAdWidgetState();
}

class _BannerAdWidgetState extends State<BannerAdWidget> {
  BannerAd? _banner;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final banner = await AdService.instance.loadBanner();
    if (!mounted) {
      banner?.dispose();
      return;
    }
    setState(() {
      _banner = banner;
      _failed = banner == null;
    });
  }

  @override
  void dispose() {
    _banner?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final banner = _banner;
    if (banner != null) {
      return SizedBox(
        width: banner.size.width.toDouble(),
        height: banner.size.height.toDouble(),
        child: AdWidget(ad: banner),
      );
    }
    // Placeholder visible mientras carga o si falló, para previsualizar el
    // diseño. En builds de release no se muestra nada si el anuncio falla.
    if (!kShowAdPlaceholder || (kReleaseMode && _failed)) {
      return const SizedBox.shrink();
    }
    return const _AdPlaceholder();
  }
}

class _AdPlaceholder extends StatelessWidget {
  const _AdPlaceholder();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // Tamaño estándar de banner 320x50, acotado al ancho disponible para
    // no desbordar en pantallas muy estrechas.
    final maxWidth = MediaQuery.sizeOf(context).width - 24;
    return Container(
      width: maxWidth < 320 ? maxWidth : 320,
      height: 50,
      margin: const EdgeInsets.symmetric(vertical: 4),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: theme.colorScheme.outlineVariant),
      ),
      child: Center(
        child: Text(
          'Ad',
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
            letterSpacing: 2,
          ),
        ),
      ),
    );
  }
}
