package de.pattaku.otakupulse.app

import de.pattaku.otakupulse.app.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun `nackte IP mit Port wird unverschluesselt angenommen`() {
        // Im LAN gibt es kein Zertifikat — https würde hier immer scheitern.
        assertEquals("http://192.168.0.161:3005/", SettingsStore.normalize("192.168.0.161:3005"))
    }

    @Test
    fun `Domain ohne Schema bekommt https`() {
        assertEquals("https://app.otakupulse.de/", SettingsStore.normalize("app.otakupulse.de"))
    }

    @Test
    fun `vorhandenes Schema bleibt unangetastet`() {
        assertEquals("http://app.otakupulse.de/", SettingsStore.normalize("http://app.otakupulse.de"))
    }

    @Test
    fun `abschliessender Schraegstrich wird ergaenzt`() {
        // Retrofit verlangt eine Basis-URL mit Schrägstrich am Ende.
        assertEquals("https://app.otakupulse.de/", SettingsStore.normalize("https://app.otakupulse.de"))
        assertEquals("https://app.otakupulse.de/", SettingsStore.normalize("https://app.otakupulse.de/"))
    }

    @Test
    fun `Leerzeichen werden abgeschnitten`() {
        assertEquals("https://app.otakupulse.de/", SettingsStore.normalize("  app.otakupulse.de  "))
    }

    @Test
    fun `Tailscale-Adresse gilt als IP`() {
        assertEquals("http://100.87.113.82:3005/", SettingsStore.normalize("100.87.113.82:3005"))
    }
}
