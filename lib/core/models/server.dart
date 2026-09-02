class VpnServer {
  const VpnServer({
    required this.id,
    required this.country,
    required this.city,
    required this.flag,
    required this.ping,
    this.isPremium = false,
  });

  final String id;
  final String country;
  final String city;
  final String flag;
  final int ping;
  final bool isPremium;

  String get title => '$country · $city';
}

const demoServers = [
  VpnServer(id: 'nl-ams', country: 'Нидерланды', city: 'Амстердам', flag: '🇳🇱', ping: 24),
  VpnServer(id: 'de-fra', country: 'Германия', city: 'Франкфурт', flag: '🇩🇪', ping: 31),
  VpnServer(id: 'fi-hel', country: 'Финляндия', city: 'Хельсинки', flag: '🇫🇮', ping: 38),
  VpnServer(id: 'us-nyc', country: 'США', city: 'Нью-Йорк', flag: '🇺🇸', ping: 92, isPremium: true),
  VpnServer(id: 'jp-tyo', country: 'Япония', city: 'Токио', flag: '🇯🇵', ping: 148, isPremium: true),
];

