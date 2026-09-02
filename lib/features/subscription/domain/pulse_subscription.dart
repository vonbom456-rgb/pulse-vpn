enum PulseSubscriptionStatus { active, frozen, expired }

class PulseSubscription {
  const PulseSubscription({
    required this.id,
    required this.status,
    required this.planId,
    required this.planName,
    required this.isTrial,
    required this.daysLeft,
    required this.endDate,
    required this.deviceLimit,
    this.connectedDevices,
    this.trafficLimitBytes,
    this.trafficUsedBytes,
    this.trafficRemainingBytes,
  });

  factory PulseSubscription.fromJson(Map<String, dynamic> json) {
    return PulseSubscription(
      id: json['id'] as String,
      status: PulseSubscriptionStatus.values.byName(json['status'] as String),
      planId: json['planId'] as String,
      planName: json['planName'] as String,
      isTrial: json['isTrial'] as bool,
      daysLeft: json['daysLeft'] as int,
      endDate: DateTime.parse(json['endDate'] as String),
      deviceLimit: json['deviceLimit'] as int,
      connectedDevices: json['connectedDevices'] as int?,
      trafficLimitBytes: json['trafficLimitBytes'] as int?,
      trafficUsedBytes: json['trafficUsedBytes'] as int?,
      trafficRemainingBytes: json['trafficRemainingBytes'] as int?,
    );
  }

  final String id;
  final PulseSubscriptionStatus status;
  final String planId;
  final String planName;
  final bool isTrial;
  final int daysLeft;
  final DateTime endDate;
  final int deviceLimit;
  final int? connectedDevices;
  final int? trafficLimitBytes;
  final int? trafficUsedBytes;
  final int? trafficRemainingBytes;
}

class PulsePlan {
  const PulsePlan({required this.id, required this.name, required this.days, required this.deviceLimit, required this.priceUsd});
  factory PulsePlan.fromJson(Map<String, dynamic> json) => PulsePlan(
    id: json['id'] as String,
    name: json['name'] as String,
    days: json['days'] as int,
    deviceLimit: json['deviceLimit'] as int,
    priceUsd: json['priceUsd'] as String,
  );
  final String id;
  final String name;
  final int days;
  final int deviceLimit;
  final String priceUsd;
}

class TrafficPack {
  const TrafficPack({required this.gb, required this.priceUsd});
  factory TrafficPack.fromJson(Map<String, dynamic> json) => TrafficPack(
    gb: json['gb'] as int,
    priceUsd: json['priceUsd'] as String,
  );
  final int gb;
  final String priceUsd;
}

class PulseCatalog {
  const PulseCatalog({required this.plans, required this.trafficPacks});
  final List<PulsePlan> plans;
  final List<TrafficPack> trafficPacks;
}

class PulseDevice {
  const PulseDevice({required this.id, required this.firstSeen, required this.lastSeen, this.name, this.os, this.model, this.osVersion});
  factory PulseDevice.fromJson(Map<String, dynamic> json) => PulseDevice(
    id: json['id'] as int,
    name: json['name'] as String?,
    os: json['os'] as String?,
    model: json['model'] as String?,
    osVersion: json['osVersion'] as String?,
    firstSeen: DateTime.parse(json['firstSeen'] as String),
    lastSeen: DateTime.parse(json['lastSeen'] as String),
  );
  final int id;
  final String? name;
  final String? os;
  final String? model;
  final String? osVersion;
  final DateTime firstSeen;
  final DateTime lastSeen;
}

class PurchaseQuote {
  const PurchaseQuote({required this.id, required this.action, required this.chargeUsd, required this.expiresAt});
  factory PurchaseQuote.fromJson(Map<String, dynamic> json) => PurchaseQuote(
    id: json['quoteId'] as String,
    action: json['action'] as String,
    chargeUsd: json['chargeUsd'] as String,
    expiresAt: DateTime.parse(json['expiresAt'] as String),
  );
  final String id;
  final String action;
  final String chargeUsd;
  final DateTime expiresAt;
}
