package com.example.cloroapp
import com.example.cloroapp.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class ConfigTest {
 private fun config()=CompanyConfig(JSONObject(File("src/main/assets/company_config.json").readText()))
 @Test fun exactBoundaries() { val c=config();c.validate();assertEquals(Level.CRITICAL,c.level(.199,"P1"));assertEquals(Level.WARNING,c.level(.2,"P1"));assertEquals(Level.NORMAL,c.level(.5,"P1"));assertEquals(Level.NORMAL,c.level(1.5,"P1"));assertEquals(Level.WARNING,c.level(2.0,"P1"));assertEquals(Level.CRITICAL,c.level(2.001,"P1")) }
 @Test fun invalidValues() { val c=config();assertEquals(Level.INVALID,c.level(Double.NaN,"P1"));assertEquals(Level.INVALID,c.level(-1.0,"P1")) }
 @Test fun rolePermissions() { val c=config();assertTrue(c.allowed("Operario","measure"));assertFalse(c.allowed("Operario","close"));assertTrue(c.allowed("Supervisor","close"));assertFalse(c.allowed("Jefatura","measure")) }
 @Test fun pointOverrides() { val c=config();c.json.getJSONObject("pointRanges").put("P1",JSONObject().put("criticalLow",.1).put("normalMin",.2).put("normalMax",.8).put("criticalHigh",1.0));c.validate();assertEquals(Level.WARNING,c.level(.9,"P1"));assertEquals(Level.NORMAL,c.level(.9,"P2")) }
 @Test(expected=IllegalArgumentException::class) fun rejectsReversedRange() { val c=config();c.json.put("normalMin",3);c.validate() }
 @Test fun persistenceRoundTrip() { val s=Snapshot(readings=listOf(Reading("r","P1",.8,123)),actions=listOf(Action("a","i","P1","Acción","OP01",124)));assertEquals(s,Codec.decode(Codec.encode(s))) }
}
