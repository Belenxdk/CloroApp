"""API académica sin dependencias externas. Solo datos ficticios en red de prueba."""
import argparse, json, time, math, threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
LOCK=threading.RLock()
STATE=Path(__file__).with_name('state.json')
POINTS=[dict(id='P1',farm='F1',farmName='Granja Norte Demo',barn='Galpón 1',name='Tanque principal'),dict(id='P2',farm='F1',farmName='Granja Norte Demo',barn='Galpón 2',name='Línea de agua'),dict(id='P3',farm='F2',farmName='Granja Sur Demo',barn='Galpón 1',name='Bebedero'),dict(id='P4',farm='F2',farmName='Granja Sur Demo',barn='',name='Reserva exterior')]
def initial():
 now=int(time.time()*1000)
 return dict(points=POINTS,readings=[dict(id=f'api-{p["id"]}-{h}',point=p['id'],value=[.9,.3,2.3,1.1][i],time=now-h*3600000,author='Sensor simulado',extras='',replaces='') for i,p in enumerate(POINTS) for h in range(13)],incidents=[],actions=[],events=[],synced=now)
def load():return json.loads(STATE.read_text()) if STATE.exists() else initial()
def persist(state):
 temp=STATE.with_suffix('.tmp');temp.write_text(json.dumps(state,ensure_ascii=False));temp.replace(STATE)
def apply_event(state,event):
 if not isinstance(event.get('id'),str) or not isinstance(event.get('payload'),dict):raise ValueError('Evento inválido')
 if event['id'] in state['events']:return state
 payload=event['payload'];ids={p['id'] for p in state['points']}
 for key in ['readings','incidents','actions']:
  for item in payload.get(key,[]):
   if not isinstance(item.get('id'),str) or item.get('point') not in ids:raise ValueError('Punto o identificador inválido')
   if key=='readings' and (not isinstance(item.get('value'),(int,float)) or not math.isfinite(item['value']) or item['value']<0):raise ValueError('Medición inválida')
   old=next((x for x in state[key] if x['id']==item['id']),None)
   if old and key in ['readings','actions']:
    if old!=item:raise ValueError('Registro inmutable; cree una corrección con nuevo id')
    continue
   # El cierre es terminal: un evento retrasado no reabre el caso.
   if old and old.get('closed'):continue
   state[key]=[x for x in state[key] if x['id']!=item['id']]+[item]
 state['events'].append(event['id']);return state
class Handler(BaseHTTPRequestHandler):
 def send(self,status,data):
  raw=json.dumps(data,ensure_ascii=False,allow_nan=False).encode();self.send_response(status);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Content-Length',str(len(raw)));self.end_headers();self.wfile.write(raw)
 def do_GET(self):
  if self.path!='/snapshot':return self.send(404,{'error':'Ruta inexistente'})
  with LOCK:
   s=load();persist(s);self.send(200,{k:v for k,v in s.items() if k!='events'})
 def do_POST(self):
  if self.path!='/events':return self.send(404,{'error':'Ruta inexistente'})
  try:
   size=int(self.headers.get('Content-Length','0'))
   if not 0<size<=1048576:raise ValueError('Tamaño inválido')
   event=json.loads(self.rfile.read(size))
   with LOCK:s=apply_event(load(),event);persist(s)
   self.send(200,{'accepted':event['id']})
  except (ValueError,KeyError,TypeError) as e:self.send(400,{'error':str(e)})
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--host',default='127.0.0.1');parser.add_argument('--port',type=int,default=8080);args=parser.parse_args()
 print(f'API de prueba: http://{args.host}:{args.port}/snapshot',flush=True)
 ThreadingHTTPServer((args.host,args.port),Handler).serve_forever()
