package com.braunSebs.rpaetc.vendors.uiPath.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * This class verifies the payload received from uipath
 */
@Component
public class UiPathWebhookVerifier {

  public boolean verifyPayload(String signature, String webhookSecret, String payload) {
    try {
      // Decode signature from Base64
      byte[] signatureBytes = Base64.getDecoder().decode(signature);

      // Calculate signature
      SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(secretKeySpec);
      byte[] calculatedSignature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

      // Compare signatures
      return MessageDigest.isEqual(signatureBytes, calculatedSignature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      // Handle exceptions
      e.printStackTrace();
      return false;
    }
  }
}