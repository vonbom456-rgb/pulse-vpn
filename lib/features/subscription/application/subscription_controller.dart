import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pulse_vpn/core/providers/app_providers.dart';
import 'package:pulse_vpn/features/subscription/data/subscription_repository.dart';
import 'package:pulse_vpn/features/subscription/domain/pulse_subscription.dart';

final subscriptionRepositoryProvider = Provider<SubscriptionRepository>(
  (ref) => SubscriptionRepository(ref.watch(pulseApiClientProvider)),
);
final pulseSubscriptionProvider =
    AsyncNotifierProvider<SubscriptionController, PulseSubscription?>(
  SubscriptionController.new,
);
final pulsePlansProvider = FutureProvider<PulseCatalog>(
  (ref) => ref.watch(subscriptionRepositoryProvider).plans(),
);
final pulseDevicesProvider = FutureProvider<List<PulseDevice>>(
  (ref) => ref.watch(subscriptionRepositoryProvider).devices(),
);

class SubscriptionController extends AsyncNotifier<PulseSubscription?> {
  SubscriptionRepository get _repository => ref.read(subscriptionRepositoryProvider);

  @override
  Future<PulseSubscription?> build() async {
    try {
      return await _repository.current();
    } catch (_) {
      rethrow;
    }
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(_repository.current);
  }

  Future<void> confirm(PurchaseQuote quote) async {
    await _repository.confirm(quote);
    await refresh();
  }

  Future<void> setFrozen(bool frozen) async {
    await _repository.setFrozen(frozen);
    await refresh();
  }
}
