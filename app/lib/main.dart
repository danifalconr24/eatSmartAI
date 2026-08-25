import 'package:flutter/material.dart';

import 'screens/scan_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const EatSmartAiApp());
}

class EatSmartAiApp extends StatelessWidget {
  const EatSmartAiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'eatSmartAI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      home: const ScanScreen(),
    );
  }
}
