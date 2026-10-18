import React from 'react';
import { getService, labels, minutes, money, shortName } from './helpers.js';

function Itinerary({ route, network }) {
  const groups = [];
  for (const leg of route.legs) {
    const previous = groups.at(-1);
    if (previous && previous[0].service === leg.service && leg.wait_seconds === 0) previous.push(leg);
    else groups.push([leg]);
  }
  const name = id => shortName(network.stations[id].name);
  if (!groups.length) return <p>You are already at your destination.</p>;
  return groups.map((legs,index) => {
    const first=legs[0],last=legs.at(-1),service=getService(network,first.service);
    return <div key={index} className="leg" style={{'--line':service.color}}><strong>{service.name} · {minutes(legs.reduce((s,l)=>s+l.seconds,0))} min</strong><div>{name(first.from)} → {name(last.to)}</div>
      <p>{first.wait_seconds > 0 && `Allow ${minutes(first.wait_seconds)} min estimated boarding wait. `}{legs.length} {first.service?'rail connections':'walking connections'}.</p>
      <details><summary>Show per-station estimates</summary><ol className="station-details">{legs.map((leg,i)=><li key={i}>{name(leg.from)} → {name(leg.to)}<br/>{minutes(leg.seconds)} min · {(leg.distance_m/1000).toFixed(2)} km · ₹{money(leg.fare_paise)} · {minutes(leg.wait_seconds)} min boarding wait</li>)}</ol></details>
    </div>;
  });
}
export default function JourneyCards({ network, response, selected, onSelect, onSave }) {
  return response.routes.map((route,index) => {
    const services = route.legs.map(l=>l.service).filter((s,i,a)=>i===0||s!==a[i-1]);
    return <article key={route.edges.join(',') || 'same-station'} className={`route-card ${selected===index?'selected':''}`}>
      <div className="route-header"><div><span className="route-number">JOURNEY {String(index+1).padStart(2,'0')}</span><h3>{index===0?'Your preferred journey':'Another way to go'}</h3><div className="route-tags">{route.rank>1&&<span>Yen #{route.rank} · {labels[route.rank_objective]}</span>}{route.optimal_for.map(o=><span key={o}>{labels[o]} optimum</span>)}</div></div><div className="route-actions"><button className="secondary" onClick={()=>onSelect(index)}>{selected===index?'On map':'Show on map'}</button><button className="secondary" onClick={onSave}>Save ☆</button></div></div>
      <div className="route-stats"><div><b>{minutes(route.seconds)}<small> min</small></b><span>Total estimate · {(route.distance_m/1000).toFixed(1)} km</span></div><div><b>₹{money(route.fare_paise)}</b><span>Demo fare</span></div><div><b>{route.transfers}</b><span>Transfers</span></div><div><b>{minutes(route.walk_seconds)}<small> min</small></b><span>Walking</span></div></div>
      <div className="route-services">{services.length ? services.map((id,i)=><span key={i} className="service-tag" style={{color:getService(network,id).color}}>{getService(network,id).name}</span>) : 'No travel needed'}</div>
      <details className="itinerary" key={`${index}-${selected}`} open={selected===index}><summary>Journey details · {minutes(route.ride_seconds)} min ride + {minutes(route.walk_seconds)} min walk + {minutes(route.wait_seconds)} min wait</summary><Itinerary route={route} network={network}/></details>
    </article>;
  });
}
