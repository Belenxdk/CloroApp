package com.example.cloroapp
import com.example.cloroapp.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class ConfigTest {
 private fun config(): CompanyConfig { val f = if (File("src/main/assets/company_config.json").exists()) File("src/main/assets/company_config.json") else File("app/src/main/assets/company_config.json"); return CompanyConfig(JSONObject(f.readText())) }
 @Test fun exactBoundaries() {
  val c=config();c.validate()
  listOf(1.99 to Level.CRITICAL,2.0 to Level.WARNING,2.99 to Level.WARNING,3.0 to Level.NORMAL,5.0 to Level.NORMAL,5.01 to Level.WARNING,6.0 to Level.WARNING,6.01 to Level.CRITICAL).forEach { (v,l)->assertEquals(l,c.level(v,"P01")) }
 }
 @Test fun invalidValues() { val c=config();assertEquals(Level.INVALID,c.level(Double.NaN,"P01"));assertEquals(Level.INVALID,c.level(-1.0,"P01")) }
 @Test fun rolePermissions() { val c=config();assertTrue(c.allowed("Operario","measure"));assertFalse(c.allowed("Operario","close"));assertTrue(c.allowed("Supervisor","close"));assertFalse(c.allowed("Jefatura","measure")) }
 @Test fun users() { val c=config();assertEquals(15,c.users.size);assertTrue(c.users.all { it.roles.size==1 });assertEquals(15,c.users.map { it.email }.distinct().size) }
 @Test fun directions() { val c=config();assertEquals("Cloro insuficiente",c.direction(1.0,"P01"));assertEquals("Cloro excesivo",c.direction(7.0,"P01")) }
 @Test fun pointOverrides() { val c=config();c.json.getJSONObject("pointRanges").put("P01",JSONObject().put("criticalLow",.1).put("normalMin",.2).put("normalMax",.8).put("criticalHigh",1.0));c.validate();assertEquals(Level.WARNING,c.level(.9,"P01"));assertEquals(Level.CRITICAL,c.level(.9,"P02")) }
 @Test(expected=IllegalArgumentException::class) fun reversedRange() { val c=config();c.json.put("normalMin",8);c.validate() }
 @Test fun persistenceRoundTrip() {
  val s=Snapshot(points=listOf(Point("P01","F1","Norte","G1","Bebedero","Entrada","Bebedero")),readings=listOf(Reading("r","P01",3.5,123,replaces="old",reason="Error de digitación",status="NORMAL",direction="Dentro de rango",limits="2/3/5/6")),incidents=listOf(Incident("i","P01",120,"CRITICAL",true,true,"U10","old","Cloro insuficiente",200)),actions=listOf(Action("a","i","P01","Acción","U01",124,"old","Validada")))
  assertEquals(s,Codec.decode(Codec.encode(s)))
 }
}
