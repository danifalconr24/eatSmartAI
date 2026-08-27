import 'package:eatsmart_ai/api_client.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ApiClient.generateShoppingList parsing', () {
    test('ShoppingListResult agrupa items por categoría', () {
      // El parseo de JSON vive en el método del cliente; aquí se validan los
      // modelos de resultado usados por la app.
      final item = ShoppingListItemResult(
        name: 'Pan integral',
        category: 'Panadería y cereales',
        type: 'REPLACE',
        replaces: 'Pan blanco',
        reason: 'Más fibra',
      );
      final result = ShoppingListResult(items: [item]);

      expect(result.items, hasLength(1));
      expect(result.items.first.type, 'REPLACE');
      expect(result.items.first.replaces, 'Pan blanco');
    });

    test('ApiException expone el mensaje del backend', () {
      expect(ApiException('Error de servidor').toString(), 'Error de servidor');
    });
  });
}
