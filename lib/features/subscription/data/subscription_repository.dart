import 'package:dio/dio.dart';
import 'package:pulse_vpn/core/network/pulse_api_client.dart';
import 'package:pulse_vpn/features/subscription/domain/pulse_subscription.dart';

class SubscriptionRepository {
  SubscriptionRepository(this._api);
  final PulseApiClient _api;

  Future<PulseSubscription> current() async {
    final response = await _api.get<Map<String, dynamic>>('/api/subscription');
    return PulseSubscription.fromJson(
      Map<String, dynamic>.from(response.data!['subscription'] as Map),
    );
  }

  Future<PulseCatalog> plans() async {
    final response = await _api.get<Map<String, dynamic>>('/api/plans');
    final plans = (response.data!['plans'] as List)
        .map((item) => PulsePlan.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList(growable: false);
    final packs = (response.data!['trafficPacks'] as List)
        .map((item) => TrafficPack.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList(growable: false);
    return PulseCatalog(plans: plans, trafficPacks: packs);
  }

  Future<List<PulseDevice>> devices() async {
    final response = await _api.get<Map<String, dynamic>>('/api/subscription/devices');
    return (response.data!['devices'] as List)
        .map((item) => PulseDevice.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList(growable: false);
  }

  Future<String> configPayload() async {
    final response = await _api.get<String>(
      '/api/subscription/config',
      responseType: ResponseType.plain,
    );
    return response.data!;
  }

  Future<PurchaseQuote> renewQuote() => _quote('/api/subscription/renew/quote');
  Future<PurchaseQuote> purchaseQuote(String planId) =>
      _quote('/api/subscription/purchase/quote', data: {'planId': planId});
  Future<PurchaseQuote> upgradeQuote(String planId) =>
      _quote('/api/subscription/upgrade/quote', data: {'planId': planId});
  Future<PurchaseQuote> trafficQuote(int gb) =>
      _quote('/api/subscription/traffic/quote', data: {'gb': gb});

  Future<void> confirm(PurchaseQuote quote) async {
    await _api.post<void>('/api/subscription/actions/confirm', data: {'quoteId': quote.id});
  }

  Future<void> setFrozen(bool frozen) async {
    await _api.post<void>(
      frozen ? '/api/subscription/freeze' : '/api/subscription/unfreeze',
    );
  }

  Future<void> removeDevice(int id) async {
    await _api.delete<void>('/api/subscription/devices/$id');
  }

  Future<PurchaseQuote> _quote(String path, {Object? data}) async {
    final response = await _api.post<Map<String, dynamic>>(path, data: data);
    return PurchaseQuote.fromJson(response.data!);
  }
}
