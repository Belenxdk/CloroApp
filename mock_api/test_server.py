import json,tempfile,threading,unittest,urllib.request,urllib.error
from pathlib import Path
from http.server import ThreadingHTTPServer
from server import Store,handler,now,classify
class ApiTest(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.store=Store(Path(self.tmp.name)/'test.sqlite');self.http=ThreadingHTTPServer(('127.0.0.1',0),handler(self.store));threading.Thread(target=self.http.serve_forever,daemon=True).start();self.url=f'http://127.0.0.1:{self.http.server_port}'
  self.tokens={r:self.req('/login',{'email':r+'1@cloro.demo','password':'Cloro2026!'})['token'] for r in ('operario','supervisor','jefatura')}
 def tearDown(self):self.http.shutdown();self.http.server_close();self.store.db.close();self.tmp.cleanup()
 def req(self,path,data=None,role=None):
  h={'Content-Type':'application/json'}
  if role:h['Authorization']='Bearer '+self.tokens[role]
  with urllib.request.urlopen(urllib.request.Request(self.url+path,data=json.dumps(data).encode() if data is not None else None,headers=h)) as r:return json.load(r)
 def reading(self,id='r1',point='P01',value=2):return dict(id=id,point=point,value=value,time=now(),author='forged',extras='',replaces='',reason='')
 def send(self,kind,o,role='supervisor',eid=None):return self.req('/events',dict(id=eid or kind+'-'+o['id'],type=kind,payload={{'reading':'readings','action':'actions','incident':'incidents'}[kind]:[o]}),role)
 def test_boundaries(self):
  for v,status in [(1.99,'CRITICAL'),(2,'WARNING'),(2.99,'WARNING'),(3,'NORMAL'),(5,'NORMAL'),(5.01,'WARNING'),(6,'WARNING'),(6.01,'CRITICAL')]:self.assertEqual(classify(self.store.config,'P01',v)[0],status)
  for v in [-1,float('nan'),float('inf'),True]:
   with self.assertRaises(ValueError):classify(self.store.config,'P01',v)
 def test_auth_scope(self):
  with self.assertRaises(urllib.error.HTTPError):self.req('/snapshot')
  with self.assertRaises(urllib.error.HTTPError):self.req('/login',{'email':'operario1@cloro.demo','password':'bad'})
  self.assertEqual(len(self.req('/snapshot',role='operario')['points']),4);self.assertEqual(len(self.req('/snapshot',role='jefatura')['points']),12);self.assertEqual(len(self.store.config['users']),15)
 def test_refresh_preserves(self):
  self.send('reading',self.reading(value=.2),'operario')
  for _ in range(3):
   s=self.req('/snapshot',role='operario');r=max((r for r in s['readings'] if r['point']=='P01'),key=lambda r:r['time']);self.assertEqual(r['value'],.2);self.assertEqual(r['author'],'U01')
 def test_points_permissions(self):
  self.send('reading',self.reading('p1','P01',3.2),'operario');self.send('reading',self.reading('p2','P02',4.2),'operario');s=self.req('/snapshot',role='operario');self.assertEqual(next(r for r in s['readings'] if r['id']=='p2')['point'],'P02')
  with self.assertRaises(urllib.error.HTTPError):self.send('reading',self.reading('p5','P05'),'operario')
  with self.assertRaises(urllib.error.HTTPError):self.send('reading',self.reading('boss'),'jefatura')
 def test_idempotency(self):
  r=self.reading();self.send('reading',r);self.assertTrue(self.send('reading',r)['duplicate'])
  with self.assertRaises(urllib.error.HTTPError):self.send('reading',dict(r,value=9),eid='other')
 def test_correction(self):
  r=self.reading();self.send('reading',r);new=self.reading('corrected',value=4);new.update(replaces=r['id'],reason='Error de digitación')
  with self.assertRaises(urllib.error.HTTPError):self.send('reading',new,'operario')
  self.send('reading',new);s=self.req('/snapshot',role='supervisor');self.assertTrue(any(x['id']==r['id'] for x in s['readings']));self.assertEqual(next(x for x in s['readings'] if x['id']=='corrected')['reason'],'Error de digitación')
 def test_resolution(self):
  self.send('reading',self.reading(value=6.3),'operario');s=self.req('/snapshot',role='supervisor');i=next(x for x in s['incidents'] if x['point']=='P01' and not x['closed']);self.assertEqual(i['direction'],'Cloro excesivo');close=dict(i,closed=True)
  with self.assertRaises(urllib.error.HTTPError):self.send('incident',close,eid='early')
  a=dict(id='act',point='P01',incident=i['id'],text='Verificación y acción académica registrada',time=now());self.send('action',a,'operario');self.send('reading',self.reading('normal',value=4))
  with self.assertRaises(urllib.error.HTTPError):self.send('incident',close,'operario',eid='forbidden')
  self.send('incident',close,eid='valid');s=self.req('/snapshot',role='supervisor');done=next(x for x in s['incidents'] if x['id']==i['id']);self.assertTrue(done['closed']);self.assertGreater(done['closedAt'],0);self.assertEqual(done['closedBy'],'U10');self.assertIn('Validada',s['actions'][0]['resolution'])
  self.send('reading',self.reading('again',value=1.5));s=self.req('/snapshot',role='supervisor');self.assertTrue(any(x['point']=='P01' and not x['closed'] and x['direction']=='Cloro insuficiente' for x in s['incidents']))
 def test_simulation(self):
  before=self.req('/snapshot',role='supervisor');self.req('/simulate',{},'supervisor');after=self.req('/snapshot',role='supervisor');self.assertEqual(len(after['readings'])-len(before['readings']),4)
  with self.assertRaises(urllib.error.HTTPError):self.req('/simulate',{},'operario')
 def test_persistence(self):
  self.send('reading',self.reading('persistent',value=4));other=Store(Path(self.tmp.name)/'test.sqlite');self.assertTrue(any(x['id']=='persistent' for x in other.load()['readings']));other.db.close()
if __name__=='__main__':unittest.main()
