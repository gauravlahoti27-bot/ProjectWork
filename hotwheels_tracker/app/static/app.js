const $=s=>document.querySelector(s);
async function api(u,o={}){const r=await fetch(u,{headers:{'Content-Type':'application/json'},...o});const d=await r.json();if(!r.ok)throw Error(d.error||'Request failed');return d}
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
function card(p){let st=p.last_status||'UNKNOWN';return `<article class="car-card">
<div class="car-top"><span class="model">HOT WHEELS · PORSCHE</span><span class="badge ${st==='AVAILABLE'?'on':st==='UNAVAILABLE'?'off':'wait'}">${esc(st)}</span></div>
<div class="car-art">911</div><h3>${esc(p.name)}</h3><p class="muted">Blinkit Product ID: <b>1404276</b></p>
<p class="muted">Last checked: ${esc(p.last_checked_at||'Not checked')}</p>
<div class="actions"><a class="buy" href="${esc(p.product_url)}" target="_blank" rel="noopener">Open Blinkit →</a>
${p.provider==='demo'?`<button onclick="demo(${p.id},'available')">Test alert</button>`:''}
<button class="ghost" onclick="removeProduct(${p.id})">Remove</button></div></article>`}
async function load(){let ps=await api('/api/products');$('#products').innerHTML=ps.map(card).join('')||'<div class="empty">No product is being watched.</div>';$('#status').textContent=ps.some(p=>p.last_status==='AVAILABLE')?'AVAILABLE':'WAITING';$('#last').textContent=new Date().toLocaleTimeString()}
async function demo(id,s){await api(`/api/products/${id}/demo-${s}`,{method:'POST'});await api('/api/check-now',{method:'POST'});if('Notification'in window){try{if(Notification.permission==='default')await Notification.requestPermission();if(Notification.permission==='granted')new Notification('Hot Wheels alert',{body:'Porsche 911 is available — open Blinkit now.'})}catch(e){}}load()}
async function removeProduct(id){if(confirm('Remove this tracker?')){await api(`/api/products/${id}`,{method:'DELETE'});load()}}
async function checkNow(){await api('/api/check-now',{method:'POST'});load()}
$('#addForm').addEventListener('submit',async e=>{e.preventDefault();await api('/api/products',{method:'POST',body:JSON.stringify({name:$('#name').value,product_url:$('#url').value,provider:'demo'})});e.target.reset();load()});
load();setInterval(load,15000)
