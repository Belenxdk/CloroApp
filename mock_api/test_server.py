import unittest, tempfile, threading, json, urllib.request, urllib.error
from pathlib import Path
import server
class ApiTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();server.STATE=Path(self.tmp.name)/'state.json'
  self.http=server.ThreadingHTTPServer(('127.0.0.1',0),server.Handler);self.thread=threading.Thread(target=self.http.serve_forever,daemon=True);self.thread.start();self.url=f'http://127.0.0.1:{self.http.server_port}'
 def tearDown(self):self.http.shutdown();self.http.server_close();self.thread.join();self.tmp.cleanup()
 def get(self):return json.load(urllib.request.urlopen(self.url+'/snapshot'))
 def post(self,event):
  return json.load(urllib.request.urlopen(urllib.request.Request(self.url+'/events',data=json.dumps(event).encode(),headers={'Content-Type':'application/json'})))
 def test_snapshot(self):
  s=self.get();self.assertEqual(len(s['points']),4);self.assertEqual(len(s['readings']),52)
 def test_idempotency_and_persistence(self):
  event={'id':'e1','payload':{'actions':[{'id':'a1','point':'P1','incident':'','text':'Control demo','author':'OP01','time':1}]}}
  self.post(event);self.post(event);self.assertEqual(len(self.get()['actions']),1);self.assertTrue(server.STATE.exists())
 def test_invalid_measurement_is_rejected(self):
  with self.assertRaises(urllib.error.HTTPError):self.post({'id':'e2','payload':{'readings':[{'id':'bad','point':'P1','value':-1}]}})
 def test_immutable_reading(self):
  reading=self.get()['readings'][0];reading['value']=99
  with self.assertRaises(urllib.error.HTTPError):self.post({'id':'e3','payload':{'readings':[reading]}})
 def test_closed_incident_cannot_reopen(self):
  i={'id':'i1','point':'P1','opened':1,'level':'CRITICAL','closed':True}
  self.post({'id':'e4','payload':{'incidents':[i]}});i['closed']=False
  self.post({'id':'e5','payload':{'incidents':[i]}});self.assertTrue(self.get()['incidents'][0]['closed'])
if __name__=='__main__':unittest.main()
