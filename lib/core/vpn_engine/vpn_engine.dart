enum VpnStatus { disconnected, connecting, connected, disconnecting, error }

class VpnSnapshot {
  const VpnSnapshot({
    required this.status,
    this.downloadMbps = 0,
    this.uploadMbps = 0,
    this.sessionBytes = 0,
    this.sessionDuration = Duration.zero,
    this.errorMessage,
    this.coreVersion,
  });

  final VpnStatus status;
  final double downloadMbps;
  final double uploadMbps;
  final int sessionBytes;
  final Duration sessionDuration;
  final String? errorMessage;
  final String? coreVersion;
}

abstract interface class VpnEngine {
  Stream<VpnSnapshot> get snapshots;
  Future<void> initialize();
  Future<void> connect();
  Future<void> disconnect();
  Future<void> dispose();
}

class VpnConfigurationMissingException implements Exception {
  const VpnConfigurationMissingException();

  @override
  String toString() => 'Сначала добавьте подписку или конфигурацию';
}
