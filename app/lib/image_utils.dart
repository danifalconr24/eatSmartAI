import 'dart:io';

import 'package:image/image.dart' as img;
import 'package:path_provider/path_provider.dart';

/// Reduce la imagen a un máximo de [maxEdge] píxeles en su lado largo
/// y la re-codifica como JPEG para mantener el payload razonable.
Future<File> downscaleImage(File file, {int maxEdge = 1600}) async {
  final bytes = await file.readAsBytes();
  final decoded = img.decodeImage(bytes);
  if (decoded == null) {
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
}
