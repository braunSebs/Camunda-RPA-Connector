package com.braunSebs.rpaetc.vendors.uiPath.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.braunSebs.rpaetc.vendors.uiPath.config.UiPathWebhookVerifier;
import com.braunSebs.rpaetc.vendors.uiPath.service.UiPathWebhookService;

/**
 * This class defines the webhook endpoint for UiPath, allowing it to receive
 * notifications for various events.
 */
@RestController
@CrossOrigin
@RequestMapping("/api")
public class UiPathWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiPathWebhookController.class);

    @Autowired
    private UiPathWebhookService uiPathWebhookService;

    @Autowired
    private UiPathWebhookVerifier uiPathWebhookVerifier;

    @Value("${bridge.uipath.cloud.webhook}")
    private boolean webhookEnabled;

    @Value("${bridge.uipath.cloud.webhook-secret}")
    private String webhookSecret;

    /**
     * Handles incoming webhook requests from UiPath. The method processes the
     * payload and delegates further processing to the UiPathWebhookService.
     *
     * @param payload The JSON payload received from UiPath webhook.
     */
    @RequestMapping(path = "uipath/webhook", method = RequestMethod.POST)
    public void handleWebhook(@RequestBody String payload, @RequestHeader("X-UiPath-Signature") String signature) {
        if (webhookEnabled) {
            if (!uiPathWebhookVerifier.verifyPayload(signature, webhookSecret, payload)) {
                LOGGER.error("Invalid signature for UiPath webhook payload: {}", payload);
                throw new IllegalStateException("Invalid signature for UiPath webhook payload");
            }

            LOGGER.debug("Received UiPath webhook payload: {}", payload);
            uiPathWebhookService.handlePayload(payload);
        }
    }

}