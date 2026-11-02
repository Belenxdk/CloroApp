"""API académica CloroApp 2.0. Solo datos ficticios; Python 3.10+, sin dependencias."""
import argparse, copy, hashlib, hmac, json, math, secrets, sqlite3, threading, time
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
CONFIG=Path(__file__).resolve().parents[1]/'app/src/main/assets/company_config.json'
def now():return int(time.time()*1000)
def limits(c,p):return [c.get('pointRanges',{}).get(p,{}).get(k,c[k]) for k in ('criticalLow','normalMin','normalMax','criticalHigh')]
def classify(c,p,v):
 if isinstance(v,bool) or not isinstance(v,(int,float)) or not math.isfinite(v) or v<0:raise ValueError('Cloro debe ser un número finito no negativo')
 a,b,d,e=limits(c,p)
 return ('CRITICAL' if v<a or v>e else 'WARNING' if v<b or v>d else 'NORMAL','Cloro insuficiente' if v<b else 'Cloro excesivo' if v>d else 'Dentro de rango')
def detect(s,c):
 for p in s['points']:
  rows=[r for r in s['readings'] if r['point']==p['id']]
  if not rows:continue
  r=max(rows,key=lambda x:x['time']);level,direction=classify(c,p['id'],r['value'])
  if level=='NORMAL':continue
  i=next((i for i in s['incidents'] if i['point']==p['id'] and not i['closed']),None)
  if i is None:
   i=dict(id='alert-'+r['id'],point=p['id'],opened=r['time'],acknowledged=False,closed=False,closedBy='',closedAt=0);s['incidents'].append(i)
  i.update(level=level,direction=direction,reading=r['id'])
def seed(c):
 stamp=now();s=dict(points=[],readings=[],incidents=[],actions=[],synced=stamp)
 values=[4,2.5,1.5,5.5,3.5,6.5,4.2,3.8,4.6,2.8,5,3]
 for i in range(12):
  p=dict(id=f'P{i+1:02}',farm=f'F{i//4+1}',farmName=['Granja Norte','Granja Centro','Granja Sur'][i//4],barn='Galpón 2' if i%4==3 else 'Galpón 1',name=['Tanque principal','Línea de distribución','Bebedero galpón 1','Bebedero galpón 2'][i%4],location=f'Sector {i//4+1} · acceso {i%4+1}',type=['Tanque','Línea','Bebedero','Bebedero'][i%4]);s['points'].append(p)
  for n,age in enumerate([h*3600000 for h in range(24)]+[d*86400000 for d in range(1,31)]):
   v=values[i] if n==0 else round(3.5+((i+n)%12)*.1,2);status,direction=classify(c,p['id'],v)
   s['readings'].append(dict(id=f"seed-{p['id']}-{n}",point=p['id'],value=v,time=stamp-age,author='Simulador',extras='',replaces='',reason='',status=status,direction=direction,limits='/'.join(map(str,limits(c,p['id'])))))
 detect(s,c);return s
class Store:
 def __init__(self,path,config=CONFIG):
  self.config=json.loads(Path(config).read_text(encoding='utf-8'))
  for p in ['']+list(self.config.get('pointRanges',{})):
   a,b,d,e=limits(self.config,p)
   if not(0<=a<b<d<e):raise ValueError('Rangos inválidos')
  self.lock=threading.RLock();self.sessions={};self.db=sqlite3.connect(str(path),check_same_thread=False)
  self.db.execute('CREATE TABLE IF NOT EXISTS snapshot(id INTEGER PRIMARY KEY CHECK(id=1),data TEXT NOT NULL)');self.db.execute('CREATE TABLE IF NOT EXISTS events(id TEXT PRIMARY KEY,owner TEXT NOT NULL,body TEXT NOT NULL)')
  if self.db.execute('SELECT data FROM snapshot WHERE id=1').fetchone() is None:self.save(seed(self.config));self.db.commit()
 def load(self):return json.loads(self.db.execute('SELECT data FROM snapshot WHERE id=1').fetchone()[0])
 def save(self,s):self.db.execute('INSERT OR REPLACE INTO snapshot VALUES(1,?)',(json.dumps(s,ensure_ascii=False,allow_nan=False),))
 def login(self,email,password):
  u=next((x for x in self.config['users'] if x['email'].lower()==str(email).strip().lower()),None)
  if not u or not hmac.compare_digest(hashlib.sha256((u['id']+':'+str(password)).encode()).hexdigest(),u['passwordHash']):raise PermissionError('Correo o contraseña incorrectos')
  token=secrets.token_urlsafe(32);self.sessions[token]=(u,time.time()+28800);return token
 def user(self,token):
  session=self.sessions.get(token)
  if not session or session[1]<=time.time():raise PermissionError('Sesión vencida. Inicie sesión otra vez.')
  return session[0]
 def ids(self,u,s):return {p['id'] for p in s['points'] if p['farm'] in u['farms']}
 def snapshot(self,u):
  with self.lock:
   s=self.load();ids=self.ids(u,s)
   for key in ('points','readings','incidents','actions'):s[key]=[x for x in s[key] if x['id' if key=='points' else 'point'] in ids]
   s['synced']=now();return s
 def rules(self):return {k:self.config[k] for k in ('normalMin','normalMax','criticalLow','criticalHigh','pointRanges')}
 def apply(self,u,e):
  with self.lock,self.db:
   eid=e.get('id');body=json.dumps(e,sort_keys=True,allow_nan=False)
   if not isinstance(eid,str) or not eid or len(eid)>200:raise ValueError('Evento sin ID válido')
   old=self.db.execute('SELECT owner,body FROM events WHERE id=?',(eid,)).fetchone()
   if old:
    if old!=(u['id'],body):raise ValueError('Identificador usado con otro contenido')
    return dict(accepted=True,duplicate=True)
   s=self.load();kind=e.get('type');key={'reading':'readings','action':'actions','incident':'incidents'}.get(kind)
   if not key or len(e.get('payload',{}).get(key,[]))!=1:raise ValueError('Evento inválido')
   o=copy.deepcopy(e['payload'][key][0]);permissions=self.config['roles'][u['roles'][0]]
   if o.get('point') not in self.ids(u,s):raise PermissionError('Punto no asignado')
   if not isinstance(o.get('id'),str) or not o['id']:raise ValueError('Registro sin ID')
   def require(p):
    if p not in permissions:raise PermissionError('Perfil sin permiso')
   def validtime(t):
    if isinstance(t,bool) or not isinstance(t,(int,float)) or not math.isfinite(t) or not 0<t<=now()+300000:raise ValueError('Fecha inválida')
   if kind=='reading':
    require('correct' if o.get('replaces') else 'measure');validtime(o.get('time'))
    if any(r['id']==o['id'] for r in s['readings']):raise ValueError('Medición original inalterable')
    if o.get('replaces') and (len(o.get('reason','').strip())<5 or not any(r['id']==o['replaces'] and r['point']==o['point'] for r in s['readings'])):raise ValueError('Corrección sin motivo u original válido')
    level,direction=classify(self.config,o['point'],o.get('value'));o.update(author=u['id'],status=level,direction=direction,limits='/'.join(map(str,limits(self.config,o['point']))));s['readings'].append(o);detect(s,self.config)
   elif kind=='action':
    require('action');validtime(o.get('time'))
    i=next((i for i in s['incidents'] if i['id']==o.get('incident') and i['point']==o['point'] and not i['closed']),None)
    if not i or len(o.get('text','').strip())<5 or o['time']<i['opened']:raise ValueError('Acción requiere alerta abierta, fecha y descripción válidas')
    if any(a['id']==o['id'] for a in s['actions']):raise ValueError('Acción original inalterable')
    o.update(author=u['id'],reading=i['reading'],resolution='Pendiente de validación');s['actions'].append(o)
   else:
    close=bool(o.get('closed'));require('close' if close else 'ack');i=next((i for i in s['incidents'] if i['id']==o['id'] and i['point']==o['point']),None)
    if not i or i['closed']:raise ValueError('Alerta inexistente o cerrada')
    if close:
     actions=[a for a in s['actions'] if a['incident']==i['id']];r=max((r for r in s['readings'] if r['point']==i['point']),key=lambda x:x['time'])
     if not actions or classify(self.config,i['point'],r['value'])[0]!='NORMAL' or now()-r['time']>self.config['staleMinutes']*60000 or r['time']<max(a['time'] for a in actions):raise ValueError('Falta acción y medición normal reciente posterior')
     for a in actions:a['resolution']='Validada por '+u['id']
    i.update(acknowledged=True,closed=close,closedBy=u['id'] if close else '',closedAt=now() if close else 0)
   self.save(s);self.db.execute('INSERT INTO events VALUES(?,?,?)',(eid,u['id'],body));return dict(accepted=True,duplicate=False)
 def simulate(self,u=None):
  with self.lock,self.db:
   if u and u['roles'][0]!='Supervisor':raise PermissionError('Solo supervisor puede generar rondas')
   s=self.load();ids=self.ids(u,s) if u else {p['id'] for p in s['points']}
   for p in s['points']:
    if p['id'] not in ids:continue
    v=secrets.choice([1.8,2.5,3.5,4.2,5.5,6.3]);status,direction=classify(self.config,p['id'],v)
    s['readings'].append(dict(id='sim-'+secrets.token_hex(12),point=p['id'],value=v,time=now(),author='Simulador',extras='',replaces='',reason='',status=status,direction=direction,limits='/'.join(map(str,limits(self.config,p['id'])))))
   detect(s,self.config);self.save(s)
def handler(store):
 class Handler(BaseHTTPRequestHandler):
  def log_message(self,*args):pass
  def answer(self,status,data):
   b=json.dumps(data,ensure_ascii=False,allow_nan=False).encode();self.send_response(status);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Content-Length',str(len(b)));self.end_headers();self.wfile.write(b)
  def dispatch(self,post=False):
   try:
    payload={}
    if post:
     n=int(self.headers.get('Content-Length','0'))
     if not 0<n<=1000000:raise ValueError('Tamaño inválido')
     payload=json.loads(self.rfile.read(n))
    if self.path=='/login' and post:return self.answer(200,{'token':store.login(payload.get('email',''),payload.get('password',''))})
    u=store.user(self.headers.get('Authorization','').removeprefix('Bearer '))
    if self.path=='/snapshot' and not post:result=store.snapshot(u)
    elif self.path=='/config' and not post:result=store.rules()
    elif self.path=='/events' and post:result=store.apply(u,payload)
    elif self.path=='/simulate' and post:store.simulate(u);result={'generated':True}
    else:return self.answer(404,{'error':'Ruta no encontrada'})
    self.answer(200,result)
   except PermissionError as e:self.answer(403,{'error':str(e)})
   except (ValueError,TypeError,KeyError,StopIteration) as e:self.answer(400,{'error':str(e)})
  def do_GET(self):self.dispatch()
  def do_POST(self):self.dispatch(True)
 return Handler
def main():
 p=argparse.ArgumentParser();p.add_argument('--host',default='127.0.0.1');p.add_argument('--port',type=int,default=8080);p.add_argument('--db',default=str(Path(__file__).with_name('cloro_v2.sqlite')));p.add_argument('--auto',action='store_true');p.add_argument('--config',default=str(CONFIG));a=p.parse_args();store=Store(a.db,a.config)
 if a.auto:
  def tick():
   while True:time.sleep(store.config.get('simulationSeconds',300));store.simulate()
  threading.Thread(target=tick,daemon=True).start()
 print(f'API CloroApp: http://{a.host}:{a.port} | rondas automáticas: {a.auto}',flush=True)
 ThreadingHTTPServer((a.host,a.port),handler(store)).serve_forever()
if __name__=='__main__':main()
