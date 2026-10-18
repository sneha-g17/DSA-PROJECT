import React, { useState, useMemo } from 'react';
import { getService, shortName } from './helpers.js';

export default function NetworkMap({ network, route }) {
  const [zoom, setZoom] = useState(1);
  const [tip, setTip] = useState('Hover over a station to explore');
  const chosen = useMemo(() => new Set(route?.edges || []), [route]);
  const nodes = useMemo(() => new Set(route?.stations || []), [route]);
  const edges = useMemo(() => [...(network?.edges || [])].sort((a,b) => Number(chosen.has(a.id)) - Number(chosen.has(b.id))), [network, chosen]);
  const important = network ? [...new Set(route ? [route.stations[0], route.stations.at(-1)] : network.services.flatMap(s => [s.stops[0], s.stops.at(-1)]))] : [];
  const width = 1050 / zoom, height = 680 / zoom;
  return <div className="panel network-panel">
    <div className="map-heading"><div><p className="eyebrow">EXPLORE THE NETWORK</p><h2>Chennai, in connection.</h2></div><span className="pill">{network ? `${network.stations.length} stations · ${network.services.length} services` : 'Loading network…'}</span></div>
    <div className="map-container">
      <svg id="map" viewBox={`${575-width/2} ${340-height/2} ${width} ${height}`} role="img" aria-label="Schematic transit network, not a geographical map">
        {edges.map(e => { const a = network.stations[e.from], b = network.stations[e.to]; return <line key={e.id} x1={a.x} y1={a.y} x2={b.x} y2={b.y} stroke={getService(network,e.service).color} strokeWidth={chosen.has(e.id)?5:e.service?2.8:1.5} strokeDasharray={e.service?undefined:'5 5'} opacity={route&&!chosen.has(e.id)?.13:.8} strokeLinecap="round"/>; })}
        {network?.stations.map(s => <circle key={s.id} cx={s.x} cy={s.y} r={nodes.has(s.id)?4.5:3.1} fill="#fff" stroke={nodes.has(s.id)?'#153e34':'#8ba094'} strokeWidth={nodes.has(s.id)?2.5:1.2} opacity={route&&!nodes.has(s.id)?.3:1} onMouseEnter={() => setTip(s.name)}><title>{s.name}</title></circle>)}
        {important.map(id => {const s=network.stations[id]; return <text key={id} x={s.x+10} y={s.y-8} fill="#274e41" fontSize="11" fontFamily="Arial" paintOrder="stroke" stroke="#fff" strokeWidth="3" strokeLinejoin="round">{shortName(s.name)}</text>;})}
      </svg>
      <div className="map-controls"><button onClick={() => setZoom(z => Math.min(z*1.25,3))} aria-label="Zoom in">+</button><button onClick={() => setZoom(z => Math.max(z/1.25,.7))} aria-label="Zoom out">−</button><button onClick={() => setZoom(1)} aria-label="Reset map">↺</button></div>
      <div className="map-caption">SCHEMATIC · NOT TO GEOGRAPHIC SCALE</div><div id="mapTip">{tip}</div>
    </div>
    <div className="legend">{network?.services.map(s => <span key={s.id}><i style={{background:s.color}}/>{s.name}</span>)}</div>
  </div>;
}
