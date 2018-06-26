package net.pocrd.core.test;

import net.pocrd.define.ConstField;
import net.pocrd.define.SecurityType;
import net.pocrd.entity.CallerInfo;
import net.pocrd.util.AESTokenHelper;
import net.pocrd.util.AesHelper;
import net.pocrd.util.Base64Util;
import net.pocrd.util.HexStringUtil;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AESTokenHelperTest {

    @Test
    public void testTokenHelper() {
        String tokenPwd = Base64Util.encodeToString(AesHelper.randomKey(256));
        AESTokenHelper th = new AESTokenHelper(tokenPwd);
        CallerInfo ci = new CallerInfo();
        ci.expire = 987654321;
        ci.subSystemId = 333;
        ci.subSystemRole = "TEST";
        ci.subSystemMainId = 123456789012345L;
        ci.key = "1111111".getBytes(ConstField.UTF8);
        ci.securityLevel = 63;
        ci.deviceId = 22222222222222L;
        ci.appid = 321;
        String token = th.generateToken(ci);
        System.out.println(token);
        CallerInfo caller = th.parseToken(token);
        assertEquals(ci.expire, caller.expire);
        assertEquals(ci.subSystemId, caller.subSystemId);
        assertEquals(ci.subSystemRole, caller.subSystemRole);
        assertEquals(ci.subSystemMainId, caller.subSystemMainId);
        assertTrue(Arrays.equals(ci.key, caller.key));
        assertEquals(ci.securityLevel, caller.securityLevel);
        assertEquals(ci.deviceId, caller.deviceId);
        assertEquals(ci.appid, caller.appid);
    }

    @Test
    public void testMultithread() {
        String tokenPwd = Base64Util.encodeToString(AesHelper.randomKey(256));
        final AESTokenHelper th = new AESTokenHelper(tokenPwd);
        final CallerInfo ci = new CallerInfo();
        ci.expire = 987654321;
        ci.subSystemId = 333;
        ci.subSystemRole = "TEST";
        ci.subSystemMainId = 123456789012345L;
        ci.key = "1111111".getBytes(ConstField.UTF8);
        ci.securityLevel = 9;
        ci.deviceId = 22222222222222L;
        //        ci.uid = 33333333333333L;
        ci.appid = 102;

        MultithreadTestHelper.runInMultithread(5, 10000, new Runnable() {

            @Override
            public void run() {
                String token = th.generateToken(ci);
                CallerInfo caller = th.parseToken(token);
                assertEquals(ci.expire, caller.expire);
                assertEquals(ci.subSystemId, caller.subSystemId);
                assertEquals(ci.subSystemRole, caller.subSystemRole);
                assertEquals(ci.subSystemMainId, caller.subSystemMainId);
                assertTrue(Arrays.equals(ci.key, caller.key));
                assertTrue(caller.securityLevel > 0);
                assertTrue(caller.deviceId > 0);
                //                assertTrue(caller.uid > 0);
                assertTrue(caller.appid > 0);
            }
        });
    }

    @Test
    public void generateTokenTest() {
        CallerInfo callerInfo = new CallerInfo();
        callerInfo.uid = 123456789L;
        callerInfo.appid = 1;
        callerInfo.deviceId = 123456789L;
        callerInfo.expire = System.currentTimeMillis() + 10000000000L;
        callerInfo.key = "demo key".getBytes(ConstField.UTF8);
        callerInfo.subSystemId = 555;
        callerInfo.subSystemRole = "TEST";
        callerInfo.subSystemMainId = 123456789012345L;
        callerInfo.securityLevel = SecurityType.RegisteredDevice.authorize(0);
        AESTokenHelper aesTokenHelper = new AESTokenHelper("eqHSs48SCL2VoGsW1lWvDWKQ8Vu71UZJyS7Dbf/e4zo=");
        String tk = aesTokenHelper.generateToken(callerInfo);
        System.out.println("tk:" + tk);
        CallerInfo callerInfo1 = aesTokenHelper.parseToken(tk);
        assertEquals(callerInfo.uid, callerInfo1.uid);
        assertEquals(callerInfo.subSystemId, callerInfo.subSystemId);
        assertEquals(callerInfo.subSystemRole, callerInfo.subSystemRole);
        assertEquals(callerInfo.subSystemMainId, callerInfo.subSystemMainId);
        assertEquals(callerInfo.appid, callerInfo1.appid);
        assertEquals(callerInfo.deviceId, callerInfo1.deviceId);
        assertEquals(callerInfo.expire, callerInfo1.expire);
        assertArrayEquals(callerInfo.key, callerInfo1.key);
        assertEquals(callerInfo.securityLevel, callerInfo1.securityLevel);
    }

    @Test
    public void generateTestToken() {
        byte[] key = HexStringUtil.toByteArray("581bb3c7f2d09e4d2f07f69706fff13f261f4cfa2038cd2ab7bb46040ca2d568");

        CallerInfo deviceToken = new CallerInfo();
        deviceToken.appid = 1;
        deviceToken.deviceId = 1414807058834L;
        deviceToken.expire = System.currentTimeMillis() + 10000000000L;
        deviceToken.key = key;
        deviceToken.securityLevel = SecurityType.RegisteredDevice.authorize(0);

        CallerInfo userToken = new CallerInfo();
        userToken.uid = 22L;
        userToken.appid = 1;
        userToken.deviceId = 1414807058834L;
        userToken.expire = System.currentTimeMillis() + 10000000000L;
        userToken.key = key;
        userToken.securityLevel = SecurityType.UserLogin.authorize(SecurityType.RegisteredDevice.authorize(0));

        CallerInfo subSystemUserToken = new CallerInfo();
        subSystemUserToken.uid = 22L;
        subSystemUserToken.appid = 1;
        subSystemUserToken.deviceId = 1414807058834L;
        subSystemUserToken.expire = System.currentTimeMillis() + 10000000000L;
        subSystemUserToken.key = key;
        subSystemUserToken.subSystemId = 555;
        subSystemUserToken.subSystemRole = "TEST";
        subSystemUserToken.subSystemMainId = 123456789012345L;
        subSystemUserToken.securityLevel = SecurityType.SubSystemUser
                .authorize(SecurityType.UserLogin.authorize(SecurityType.RegisteredDevice.authorize(0)));

        AESTokenHelper aesTokenHelper = new AESTokenHelper("Q/KFvy6blE0vbKkzXoPwpzUrNN538GK90M+kfM2mCBM=");
        System.out.println("deviceToken:" + aesTokenHelper.generateToken(deviceToken));
        System.out.println("userToken:" + aesTokenHelper.generateToken(userToken));
        System.out.println("subSystemUserToken:" + aesTokenHelper.generateToken(subSystemUserToken));
    }
}
