package com.usblocal.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `isValidHost accepts valid IPs`() {
        assertTrue(NetworkUtils.isValidHost("192.168.1.1"))
        assertTrue(NetworkUtils.isValidHost("10.0.0.1"))
        assertTrue(NetworkUtils.isValidHost("172.16.0.1"))
        assertTrue(NetworkUtils.isValidHost("0.0.0.0"))
        assertTrue(NetworkUtils.isValidHost("255.255.255.255"))
    }

    @Test
    fun `isValidHost rejects invalid IPs`() {
        assertFalse(NetworkUtils.isValidHost(""))
        assertFalse(NetworkUtils.isValidHost("256.1.1.1"))
        assertFalse(NetworkUtils.isValidHost("192.168.1"))
        assertFalse(NetworkUtils.isValidHost("not an ip"))
    }

    @Test
    fun `isValidHost accepts valid hostnames`() {
        assertTrue(NetworkUtils.isValidHost("router"))
        assertTrue(NetworkUtils.isValidHost("my-nas"))
        assertTrue(NetworkUtils.isValidHost("server.local"))
    }

    @Test
    fun `isValidPort validates port range`() {
        assertTrue(NetworkUtils.isValidPort(445))
        assertTrue(NetworkUtils.isValidPort(1))
        assertTrue(NetworkUtils.isValidPort(65535))
        assertFalse(NetworkUtils.isValidPort(0))
        assertFalse(NetworkUtils.isValidPort(65536))
        assertFalse(NetworkUtils.isValidPort(-1))
    }

    @Test
    fun `buildSmbUrl formats correctly`() {
        assertEquals("smb://192.168.1.1/USB", NetworkUtils.buildSmbUrl("192.168.1.1", 445, "USB"))
        assertEquals("smb://10.0.0.1:4455/share", NetworkUtils.buildSmbUrl("10.0.0.1", 4455, "share"))
    }

    @Test
    fun `isPrivateIpRange detects private IPs`() {
        assertTrue(NetworkUtils.isLocalAddress("192.168.1.1"))
        assertTrue(NetworkUtils.isLocalAddress("10.0.0.1"))
        assertTrue(NetworkUtils.isLocalAddress("172.16.0.1"))
        assertTrue(NetworkUtils.isLocalAddress("127.0.0.1"))
    }
}
