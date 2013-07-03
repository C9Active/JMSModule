package com.c9.security;

import java.security.Key;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.vertx.java.core.json.JsonObject;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class AESencrp {
	
	private static final String ALGO = "AES";

	private static ConcurrentHashMap<String, Key> keys = new ConcurrentHashMap<String, Key>();
	
	public static String encrypt(String Data, String privateKey) {
		try
		{
			Key key = generateKey(privateKey);
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.ENCRYPT_MODE, key);
			byte[] encVal = c.doFinal(Data.getBytes());
			String encryptedValue = new BASE64Encoder().encode(encVal);
			return encryptedValue;
	
		} catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
	}

	public static String decrypt(String encryptedData, String privateKey) {
		try
		{
			Key key = generateKey(privateKey);
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
			byte[] decValue = c.doFinal(decordedValue);
			String decryptedValue = new String(decValue);
			return decryptedValue;
		} catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static Key generateKey(String privateKey) throws Exception 
	{
		if(keys.contains(privateKey))
		{
			return keys.get(privateKey);
		}
		
		Key key = new SecretKeySpec(privateKey.getBytes(), ALGO);
		
		keys.put(privateKey, key);
		
		return key;
	}
	
	public static void main(String... args) throws Exception
	{
		JsonObject jo = new JsonObject("{\"name\":\"test\"}");
		
		String eData = AESencrp.encrypt(jo.toString(), "1234567890123456");
		
		System.out.println(eData);
		
		System.out.println(AESencrp.decrypt(eData, "1234567890123456"));
		
		eData = AESencrp.encrypt(jo.toString(), "1234567890123457");
		
		System.out.println(eData);
		
		System.out.println(AESencrp.decrypt(eData, "1234567890123457"));
	}
}
