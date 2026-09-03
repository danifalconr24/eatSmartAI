import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:image/image.dart' as img;
import 'package:path_provider/path_provider.dart';

/// Reduce la imagen a un máximo de [maxEdge] píxeles en su lado largo
/// y la re-codifica como JPEG para mantener el payload razonable.
///
/// Nunca lanza: ante cualquier fallo de decodificación/codificación
/// (p. ej. HEIC de la galería de iOS) devuelve el archivo original.
Future<File> downscaleImage(File file, {int maxEdge = 1600}) async {
  try {
    final bytes = await file.readAsBytes();
    final decoded = img.decodeImage(bytes);
    if (decoded == null) {
      debugPrint('downscaleImage: formato no soportado, se usa el original');
      return file;
    }
    img.Image processed = decoded;
    if (decoded.width > maxEdge || decoded.height > maxEdge) {
      processed = img.copyResize(
        decoded,
        width: decoded.width >= decoded.height ? maxEdge : null,
        height: decoded.height > decoded.width ? maxEdge : null,
      );
    }
    final dir = await getTemporaryDirectory();
    final out = File(
        '${dir.path}/ticket_${DateTime.now().millisecondsSinceEpoch}.jpg');
    await out.writeAsBytes(img.encodeJpg(processed, quality: 85));
    return out;
  } catch (e) {
    debugPrint('downscaleImage: fallo al procesar ($e), se usa el original');
    return file;
  }
}
