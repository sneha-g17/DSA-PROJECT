export const labels = { fastest: 'Fastest', cheapest: 'Lowest demo fare', fewest_transfers: 'Fewest transfers', balanced: 'Balanced' };
export const minutes = seconds => Math.ceil(seconds / 60);
export const money = paise => (paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const shortName = name => name.replace('Puratchi Thalaivar Dr. M.G. Ramachandran Central Metro', 'Central Metro').replace('MGR Chennai Central Suburban', 'Central Suburban').replace('Chennai International Airport Metro', 'Airport Metro');
export const getService = (network, id) => id === 0 ? { name: 'Walk', color: '#85978b' } : network.services.find(s => s.id === id);
export function stationId(network, value) {
  const key = value.trim().toLowerCase();
  if (!key) throw new Error('Please enter both stations.');
  const exact = network.stations.find(s => s.name.toLowerCase() === key);
  if (exact) return exact.id;
  const matches = network.stations.filter(s => s.name.toLowerCase().includes(key));
  if (matches.length === 1) return matches[0].id;
  throw new Error(matches.length ? 'Several stations match. Select the full name from the suggestions.' : 'Station not found. Select a station from the suggestions.');
}
export function readFavorites(network) {
  try {
    const items = JSON.parse(localStorage.getItem('oneroute.react.favorites') || '[]');
    return Array.isArray(items) ? items.filter(f => f.revision === network.revision && Number.isInteger(f.source) && network.stations[f.source] && Number.isInteger(f.destination) && network.stations[f.destination]).slice(0, 8) : [];
  } catch { return []; }
}
