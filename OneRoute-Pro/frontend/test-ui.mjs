import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { createServer } from 'node:net';
import { fileURLToPath } from 'node:url';
import { JSDOM, VirtualConsole } from 'jsdom';
const root=fileURLToPath(new URL('../',import.meta.url));
const probe=createServer();probe.listen(0,'127.0.0.1');await once(probe,'listening');const port=probe.address().port;await new Promise(resolve=>probe.close(resolve));
const base=`http://localhost:${port}`;
const child=spawn('java',['-jar','backend/target/oneroute-api.jar',`--server.port=${port}`],{cwd:root,stdio:['ignore','pipe','pipe']});
let output='';child.stdout.on('data',data=>output+=data);child.stderr.on('data',data=>output+=data);
let dom;
async function until(test,message){const deadline=Date.now()+30000;while(Date.now()<deadline){if(await test())return;await new Promise(r=>setTimeout(r,30));}throw Error(message+'\n'+output);}
try{
  await until(async()=>{try{return(await fetch(base+'/api/health')).ok;}catch{return false;}},'Java server did not start');
  const errors=[];const console=new VirtualConsole();console.on('jsdomError',e=>errors.push(e.message));console.on('error',e=>errors.push(String(e)));
  dom=await JSDOM.fromURL(base,{runScripts:'dangerously',resources:'usable',pretendToBeVisual:true,virtualConsole:console,beforeParse(window){
    window.fetch=(path,options)=>fetch(new URL(path,base),options);
    window.AbortController=globalThis.AbortController;
    window.URL.createObjectURL=()=> 'blob:oneroute-test';window.URL.revokeObjectURL=()=>{};
    window.addEventListener('click',e=>{if(e.target.closest('a[download]'))e.preventDefault();});
  }});
  const document=dom.window.document;
  await until(()=>document.querySelectorAll('#stations option').length===135,'React station loading failed');
  document.querySelector('.examples button').click();
  await until(()=>document.querySelectorAll('.route-card').length>0,'React route search failed');
  assert.equal(document.querySelector('#alternatives').value,'3');
  assert.ok(document.querySelector('a[href="/swagger-ui.html"]'));
  assert.match(document.querySelector('.route-stats').textContent,/km/);
  assert.match(document.querySelector('.station-details').textContent,/boarding wait/);
  document.querySelectorAll('.route-actions button')[1].click();
  assert.equal(JSON.parse(dom.window.localStorage.getItem('oneroute.react.favorites')).length,1);
  await until(()=>document.querySelector('.favorite'),'React favorite did not render');
  document.querySelector('button[aria-label="Remove saved journey"]').click();
  assert.equal(JSON.parse(dom.window.localStorage.getItem('oneroute.react.favorites')).length,0);
  const service=document.querySelector('#closedService');service.value='6';service.dispatchEvent(new dom.window.Event('change',{bubbles:true}));
  document.querySelector('#search').dispatchEvent(new dom.window.Event('submit',{bubbles:true,cancelable:true}));
  await until(()=>document.querySelector('#results').textContent.includes('No route found'),'React closure scenario failed');
  service.value='';service.dispatchEvent(new dom.window.Event('change',{bubbles:true}));
  document.querySelector('#search').dispatchEvent(new dom.window.Event('submit',{bubbles:true,cancelable:true}));
  await until(()=>document.querySelectorAll('.route-card').length>0,'Clearing closure failed');
  assert.match(document.querySelector('.metric-grid').textContent,/Hit/);
  const oldView=document.querySelector('#map').getAttribute('viewBox');document.querySelector('button[aria-label="Zoom in"]').click();
  await until(()=>document.querySelector('#map').getAttribute('viewBox')!==oldView,'Map zoom did not update');
  assert.deepEqual(errors,[]);
  process.stdout.write('React + live Java/C++ integration passed: loading, routing, estimates, favorites, closure, reset, cache, zoom; no console errors.\n');
}finally{
  dom?.window.close();child.kill();await Promise.race([once(child,'exit'),new Promise(resolve=>setTimeout(resolve,3000))]);
}
