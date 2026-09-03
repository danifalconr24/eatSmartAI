import 'package:flutter/material.dart';

import '../widgets/credits_chip.dart';
import '../widgets/floating_nav_space.dart';
import 'scan_screen.dart';
import 'shopping_lists_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final PageController _pageController = PageController();
  final GlobalKey<ShoppingListsScreenState> _listsKey =
      GlobalKey<ShoppingListsScreenState>();
  final GlobalKey _navBarKey = GlobalKey();
  final ValueNotifier<double> _navBarHeight = ValueNotifier(0);
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _measureNavBar());
  }

  /// Mide la altura real de la barra flotante (SafeArea + margen + barra) y
  /// la publica para que las pantallas incrustadas dejen el hueco exacto.
  void _measureNavBar() {
    final box = _navBarKey.currentContext?.findRenderObject() as RenderBox?;
    if (box == null || !mounted) return;
    final height = box.size.height;
    if (height > 0 && height != _navBarHeight.value) {
      _navBarHeight.value = height;
    }
  }

  @override
  void dispose() {
    _navBarHeight.dispose();
    _pageController.dispose();
    super.dispose();
  }

  void _onDestinationSelected(int index) {
    _pageController.animateToPage(
      index,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOutCubic,
    );
  }

  void _onPageChanged(int index) {
    setState(() => _currentIndex = index);
    if (index == 2) {
      _listsKey.currentState?.reload();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('eatSmartAI'),
        actions: const [
          CreditsChip(),
          SizedBox(width: 8),
        ],
      ),
      extendBody: true,
      body: FloatingNavSpace(
        height: _navBarHeight,
        child: PageView(
          controller: _pageController,
          onPageChanged: _onPageChanged,
          children: [
            const ScanScreen(mode: ScanMode.ticket, embedded: true),
            const ScanScreen(mode: ScanMode.product, embedded: true),
            ShoppingListsScreen(key: _listsKey, embedded: true),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        key: _navBarKey,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: theme.colorScheme.surfaceContainer,
              borderRadius: BorderRadius.circular(32),
              boxShadow: [
                BoxShadow(
                  color: theme.colorScheme.shadow.withValues(alpha: 0.15),
                  blurRadius: 16,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(32),
              child: NavigationBar(
                backgroundColor: Colors.transparent,
                elevation: 0,
                selectedIndex: _currentIndex,
                onDestinationSelected: _onDestinationSelected,
                destinations: const [
                  NavigationDestination(
                    icon: Icon(Icons.receipt_long_outlined),
                    selectedIcon: Icon(Icons.receipt_long),
                    label: 'Ticket',
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.inventory_2_outlined),
                    selectedIcon: Icon(Icons.inventory_2),
                    label: 'Producto',
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.shopping_cart_outlined),
                    selectedIcon: Icon(Icons.shopping_cart),
                    label: 'Listas',
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
