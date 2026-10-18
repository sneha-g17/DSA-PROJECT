import React, { useEffect, useRef, useState } from 'react';
import JourneyForm from './JourneyForm.jsx';
import JourneyCards from './JourneyCards.jsx';
import NetworkMap from './NetworkMap.jsx';
import { readFavorites, shortName, stationId } from './helpers.js';
import { jsPDF } from 'jspdf';

const initialForm = { source:'', destination:'', objective:'fastest', mode:'all', compare:true, alternatives:3, closedStation:'', closedService:'', closedEdge:'' };
const examples = [['Velachery','Avadi'],['Chennai International Airport Metro','CMBT Metro'],['Chromepet','Wimco Nagar Metro']];

export default function App() {
  const [network,setNetwork]=useState(null), [form,setForm]=useState(initialForm);
  const [response,setResponse]=useState(null), [selected,setSelected]=useState(0);
  const [favorites,setFavorites]=useState([]), [message,setMessage]=useState('Loading the demo network…');
  const [busy,setBusy]=useState(false);
  const dialog=useRef(null), active=useRef(null), sequence=useRef(0);
  useEffect(()=>{
    const controller=new AbortController();
    fetch('/api/network',{signal:controller.signal}).then(async res=>{
      if(!res.ok) throw new Error('Network unavailable. Start the Java server and reload.');
      const data=await res.json();
      setNetwork(data);setFavorites(readFavorites(data));setMessage('Choose your stations. All operational figures are demo estimates.');
    }).catch(error=>{if(error.name!=='AbortError')setMessage(error.message);});
    return ()=>{controller.abort();active.current?.abort();};
  },[]);
  async function search(values) {
    if(!network)return;
    let query;
    try {
      query={source:stationId(network,values.source),destination:stationId(network,values.destination),objective:values.objective,mode:values.mode,compare:values.compare,alternatives:Number(values.alternatives),
        closed_stations:values.closedStation.trim()?[stationId(network,values.closedStation)]:[],closed_services:values.closedService?[Number(values.closedService)]:[],closed_edges:values.closedEdge?[Number(values.closedEdge)]:[]};
    } catch(error){setMessage(error.message);return;}
    const id=++sequence.current;
    active.current?.abort();active.current=new AbortController();setBusy(true);setMessage('Finding your connections…');
    try {
      const res=await fetch('/api/routes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(query),signal:active.current.signal});
      const data=await res.json();if(!res.ok)throw new Error(data.error||'Unable to plan this journey.');
      if(id!==sequence.current)return;
      setResponse(data);setSelected(0);
      setMessage(query.closed_stations.length||query.closed_services.length||query.closed_edges.length?'Results include your simulated closures.':'Ready. Times, distances and fares are demonstration estimates.');
    } catch(error){if(id===sequence.current&&error.name!=='AbortError'){setMessage(error.message);setResponse(null);}}
    finally{if(id===sequence.current)setBusy(false);}
  }
  function useTrip(source,destination) {
    const values={...initialForm,source,destination};setForm(values);search(values);
  }
  function persist(items) {
    try{localStorage.setItem('oneroute.react.favorites',JSON.stringify(items));setFavorites(items);return true;}
    catch{setMessage('Browser storage is unavailable.');return false;}
  }
  function save() {
    const q=response.query;
    const items=favorites.filter(f=>f.source!==q.source||f.destination!==q.destination);
    if(persist([{source:q.source,destination:q.destination,revision:network.revision},...items].slice(0,8)))setMessage('Journey saved on this device.');
  }
   
function exportJourney() {
  const route = response?.routes[selected];
  if (!route || !network) return;

  const pdf = new jsPDF();
  const margin = 20;
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  let y = 20;

  function addLine(text, size = 11, bold = false) {
    pdf.setFontSize(size);
    pdf.setFont('helvetica', bold ? 'bold' : 'normal');

    const lines = pdf.splitTextToSize(
      String(text),
      pageWidth - margin * 2
    );

    for (const line of lines) {
      if (y > pageHeight - 20) {
        pdf.addPage();
        y = 20;
      }
      pdf.text(line, margin, y);
      y += size * 0.5 + 3;
    }
  }

  // PDF heading
  pdf.setFillColor(17, 90, 85);
  pdf.rect(0, 0, pageWidth, 35, 'F');

  pdf.setTextColor(255, 255, 255);
  pdf.setFontSize(20);
  pdf.setFont('helvetica', 'bold');
  pdf.text('OneRoute PRO', margin, 19);

  pdf.setFontSize(10);
  pdf.text('Metro & Transit Journey Report', margin, 27);

  pdf.setTextColor(30, 40, 45);
  y = 48;

  // Get station names from existing network data
  const stationNames = route.stations.map(id =>
    network.stations[id]?.name || String(id)
  );

  addLine('JOURNEY SUMMARY', 14, true);
  y += 4;

  addLine(`From: ${stationNames[0]}`);
  addLine(`To: ${stationNames[stationNames.length - 1]}`);
  addLine(`Priority: ${response.query?.objective || 'fastest'}`);
  addLine(`Route option: ${selected + 1}`);

  y += 10;
  addLine('ROUTE INFORMATION', 14, true);
  y += 4;

  // Display available route properties
  
const fields = [
  ['Duration', (route.seconds / 60).toFixed(0), 'minutes'],
  ['Distance', (route.distance_m / 1000).toFixed(2), 'km'],
  ['Estimated demo fare', (route.fare_paise / 100).toFixed(2), 'INR'],
  ['Transfers', route.transfers, ''],
  ['Walking time', (route.walk_seconds / 60).toFixed(0), 'minutes'],
  ['Waiting time', (route.wait_seconds / 60).toFixed(0), 'minutes']
];


  fields.forEach(([label, value, unit]) => {
    if (value !== undefined && value !== null) {
      addLine(`${label}: ${value} ${unit}`);
    }
  });

  y += 10;
  addLine('STATIONS IN YOUR ROUTE', 14, true);
  y += 4;

  stationNames.forEach((station, index) => {
    addLine(`${index + 1}. ${station}`);
  });

  y += 10;
  addLine('IMPORTANT NOTICE', 12, true);
  addLine(
    'This project uses demonstration data. Travel times, distances and fares are estimates.',
    9
  );

  pdf.save('OneRoute_Journey_Report.pdf');
}

  const route=response?.routes[selected];
  return <>
    <header className="topbar"><a className="brand" href="/" aria-label="OneRoute home"><span className="logo">↗</span> OneRoute<span className="city">PRO</span></a><nav><a href="#planner">Journey planner</a><a href="#saved">Saved journeys</a><a href="/swagger-ui.html" target="_blank" rel="noreferrer">API docs ↗</a><button className="text-button" onClick={()=>dialog.current.showModal()}>About the data</button></nav><span className="demo">DEMO NETWORK</span></header>
    <main id="planner">
      <section className="intro"><div><p className="eyebrow">ONE CITY. SIX CONNECTIONS.</p><h1>Your city.<br/><em>Better connected.</em></h1><p className="subtitle">Metro, suburban rail and MRTS.<br/>Find the journey that works for you.</p></div><div className="intro-note"><span className="line-art">↗</span><p>Less guesswork.<br/>More possibilities.</p></div></section>
      <div className="workspace"><aside>
        <JourneyForm network={network} form={form} onChange={setForm} onSubmit={e=>{e.preventDefault();search(form);}} busy={busy} message={message}/>
        <section className="panel saved-panel" id="saved"><div className="panel-heading"><h2>Saved journeys</h2><span>ON THIS DEVICE</span></div><div>{favorites.length?favorites.map((f,index)=><div className="favorite" key={`${f.source}-${f.destination}`}><button onClick={()=>useTrip(network.stations[f.source].name,network.stations[f.destination].name)}>{shortName(network.stations[f.source].name)} → {shortName(network.stations[f.destination].name)}</button><button aria-label="Remove saved journey" onClick={()=>persist(favorites.filter((_,i)=>i!==index))}>×</button></div>):<p>Save a journey after searching. Your favourites stay in this browser.</p>}</div></section>
        <div className="data-note"><span>ⓘ</span><p>Travel times, distances, waits and fares are illustrative. This demo has no live train feed.</p></div>
      </aside><section className="canvas-area">
        <NetworkMap network={network} route={route}/>
        <div className="results-heading"><div><p className="eyebrow">YOUR NEXT CONNECTION</p><h2>{response ? response.routes.length ? `${response.routes.length} journey ${response.routes.length===1?'option':'options'} to consider.`:'No journey for these constraints.':'A good journey starts here.'}</h2></div><button className="secondary" disabled={!route} onClick={exportJourney}>Download Journey PDF ↓</button></div>
        <div id="results" aria-live="polite">
          {!response ? <div className="empty-state"><span>↗</span><h3>Where shall we go?</h3><p>Choose your stations, or explore one of these journeys.</p><div className="examples">{examples.map(([a,b])=><button key={a} disabled={!network} onClick={()=>useTrip(a,b)}>{shortName(a)} → {shortName(b)}</button>)}</div></div> : !response.routes.length ? <div className="empty-state"><h3>No route found</h3><p>Try all services, or remove a closure from the disruption scenario.</p></div> : <JourneyCards network={network} response={response} selected={selected} onSelect={setSelected} onSave={save}/>}
        </div>
        <details className="performance"><summary>Routing insights</summary>{response ? <><div className="metric-grid"><div><b>{response.request_ms.toFixed(2)} ms</b>Java API processing time</div><div><b>{(response.total_engine_us/1000).toFixed(3)} ms</b>{response.cache_hit?'Original cached':'Total C++'} search time</div><div><b>{response.cache_hit?'Hit':'Miss'}</b>256-entry LRU cache</div><div><b>{response.searches}</b>Objective searches {response.cache_hit?'in cached result':''}</div></div><p>Search state: station + previous service + walking status. Cache keys include every constraint and the network revision.</p></>:<p>Search to see measured routing statistics.</p>}</details>
      </section></div>
      <footer><strong>OneRoute Pro</strong><p>A Chennai transit planning portfolio project.</p><span>C++ Dijkstra + Yen · Spring Boot · React</span></footer>
    </main>
    <dialog ref={dialog}><button className="close" aria-label="Close" onClick={()=>dialog.current.close()}>×</button><p className="eyebrow">TRANSPARENT BY DESIGN</p><h2>A working application.<br/>A demonstration dataset.</h2><p>This project is not an official CMRL or Southern Railway journey planner.</p><ul>{network?.limitations.map(item=><li key={item}>{item}</li>)}</ul><p>Yen ranks up to three station-simple routes for your selected priority. Comparison optionally adds the best route for each other priority; duplicate paths are merged.</p><p>Dataset: {network?.version} · Revision: {network?.revision}</p></dialog>
  </>;
}
