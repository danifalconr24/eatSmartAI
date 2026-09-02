import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../image_utils.dart';
import '../shared_camera.dart';
import 'form_screen.dart';

enum ScanMode { ticket, product }

class ScanScreen extends StatefulWidget {
  const ScanScreen({super.key, required this.mode, this.embedded = false});

  final ScanMode mode;

  /// Cuando es true, la pantalla se renderiza sin Scaffold/AppBar propios
  /// (pensada para vivir dentro del PageView de HomeScreen).
  final bool embedded;

  @override
  State<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends State<ScanScreen>
    with AutomaticKeepAliveClientMixin {
  bool _busy = false;

  bool get _isTicket => widget.mode == ScanMode.ticket;

  ValueNotifier<CameraController?> get _sharedController =>
      SharedCamera.instance.controller;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    SharedCamera.instance.ensureInitialized();
  }

  Future<void> _takePhoto() async {
    final controller = _sharedController.value;
    if (controller == null || _busy) return;
    setState(() => _busy = true);
    try {
      final xfile = await controller.takePicture();
      await _previewAndContinue(File(xfile.path));
    } catch (_) {
      _showError('No se pudo tomar la foto. Inténtalo de nuevo.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _pickFromGallery() async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      final XFile? picked;
      try {
        picked = await ImagePicker().pickImage(
          source: ImageSource.gallery,
          // En iOS, la lectura de metadatos completos puede fallar con
          // PHPicker en simulador; no los necesitamos.
          requestFullMetadata: false,
        );
      } catch (e) {
        debugPrint('image_picker error: $e');
        _showError('No se pudo abrir la galería.');
        return;
      }
      if (picked != null) {
        await _previewAndContinue(File(picked.path));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _previewAndContinue(File file) async {
    if (!mounted) return;
    final confirmed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => PreviewScreen(imageFile: file)),
    );
    if (confirmed == true && mounted) {
      try {
        final compressed = await downscaleImage(file);
        if (!mounted) return;
        await Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => FormScreen(
              imageFile: compressed,
              mode: widget.mode,
            ),
          ),
        );
      } catch (e) {
        debugPrint('preview/continue error: $e');
        _showError('No se pudo procesar la imagen. Inténtalo con otra.');
      }
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  Widget _buildBody(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        Expanded(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(28),
              child: Container(
                color: theme.colorScheme.surfaceContainerLow,
                child: ValueListenableBuilder<CameraController?>(
                  valueListenable: _sharedController,
                  builder: (context, controller, _) {
                    if (controller == null) {
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(
                            _isTicket
                                ? 'Cámara no disponible.\nPuedes elegir una foto del ticket desde la galería.'
                                : 'Cámara no disponible.\nPuedes elegir una foto del producto desde la galería.',
                            textAlign: TextAlign.center,
                          ),
                        ),
                      );
                    }
                    return CameraPreview(controller);
                  },
                ),
              ),
            ),
          ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            // En modo incrustado, dejar hueco para la barra de navegación
            // flotante de HomeScreen (extendBody).
            padding: EdgeInsets.fromLTRB(
                16, 16, 16, widget.embedded ? 104 : 16),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _busy ? null : _pickFromGallery,
                    icon: const Icon(Icons.photo_library),
                    label: const Text('Galería'),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  flex: 2,
                  child: ValueListenableBuilder<CameraController?>(
                    valueListenable: _sharedController,
                    builder: (context, controller, _) {
                      return FilledButton.icon(
                        onPressed:
                            (controller != null && !_busy) ? _takePhoto : null,
                        icon: const Icon(Icons.camera_alt),
                        label: Text(
                          _isTicket
                              ? 'Fotografiar ticket'
                              : 'Fotografiar producto',
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    if (widget.embedded) {
      return _buildBody(context);
    }
    return Scaffold(
      appBar: AppBar(title: const Text('eatSmartAI')),
      body: _buildBody(context),
    );
  }
}

class PreviewScreen extends StatelessWidget {
  const PreviewScreen({super.key, required this.imageFile});

  final File imageFile;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('¿Usar esta foto?')),
      body: Column(
        children: [
          Expanded(child: InteractiveViewer(child: Image.file(imageFile))),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.of(context).pop(false),
                      child: const Text('Repetir'),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    flex: 2,
                    child: FilledButton(
                      onPressed: () => Navigator.of(context).pop(true),
                      child: const Text('Confirmar'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
