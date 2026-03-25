package chat.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void testValidPort() {
        assertTrue(ValidationUtils.isValidPort(1024));
        assertTrue(ValidationUtils.isValidPort(5000));
        assertTrue(ValidationUtils.isValidPort(65535));
    }

    @Test
    void testInvalidPort() {
        assertFalse(ValidationUtils.isValidPort(1023));
        assertFalse(ValidationUtils.isValidPort(65536));
        assertFalse(ValidationUtils.isValidPort(0));
        assertFalse(ValidationUtils.isValidPort(-1));
    }

    @Test
    void testValidPortString() {
        assertTrue(ValidationUtils.isValidPort("5000"));
        assertTrue(ValidationUtils.isValidPort("1024"));
    }

    @Test
    void testInvalidPortString() {
        assertFalse(ValidationUtils.isValidPort("abc"));
        assertFalse(ValidationUtils.isValidPort(""));
        assertFalse(ValidationUtils.isValidPort(null));
        assertFalse(ValidationUtils.isValidPort("65536"));
    }

    @Test
    void testValidIp() {
        assertTrue(ValidationUtils.isValidIp("224.0.0.1"));
        assertTrue(ValidationUtils.isValidIp("239.255.255.255"));
        assertTrue(ValidationUtils.isValidIp("230.0.0.0"));
    }

    @Test
    void testInvalidIp() {
        assertFalse(ValidationUtils.isValidIp("192.168.1.1"));
        assertFalse(ValidationUtils.isValidIp("10.0.0.1"));
        assertFalse(ValidationUtils.isValidIp(""));
        assertFalse(ValidationUtils.isValidIp(null));
    }

    @Test
    void testValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("alice"));
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("user_name"));
        assertTrue(ValidationUtils.isValidUsername("ABCdefGhi"));
    }

    @Test
    void testInvalidUsername() {
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername("user-name"));
        assertFalse(ValidationUtils.isValidUsername("user name"));
    }

    @Test
    void testValidTtl() {
        assertTrue(ValidationUtils.isValidTtl(1));
        assertTrue(ValidationUtils.isValidTtl(128));
        assertTrue(ValidationUtils.isValidTtl(255));
    }

    @Test
    void testInvalidTtl() {
        assertFalse(ValidationUtils.isValidTtl(0));
        assertFalse(ValidationUtils.isValidTtl(256));
        assertFalse(ValidationUtils.isValidTtl(-1));
    }

    @Test
    void testValidTtlString() {
        assertTrue(ValidationUtils.isValidTtl("1"));
        assertTrue(ValidationUtils.isValidTtl("255"));
    }

    @Test
    void testInvalidTtlString() {
        assertFalse(ValidationUtils.isValidTtl("abc"));
        assertFalse(ValidationUtils.isValidTtl(""));
        assertFalse(ValidationUtils.isValidTtl("256"));
    }

    @Test
    void testErrorMessages() {
        assertNotNull(ValidationUtils.getPortErrorMessage());
        assertNotNull(ValidationUtils.getIpErrorMessage());
        assertNotNull(ValidationUtils.getUsernameErrorMessage());
        assertNotNull(ValidationUtils.getTtlErrorMessage());
    }
}