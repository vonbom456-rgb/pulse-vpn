import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pulse_vpn/app.dart';

void main() {
  testWidgets('onboarding Pulse VPN открывается', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: PulseApp()));
    await tester.pumpAndSettle();

    expect(find.text('Живой интернет'), findsOneWidget);
    expect(find.text('Дальше'), findsOneWidget);
  });
}
