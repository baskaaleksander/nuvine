package com.baskaaleksander.nuvine.integration.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

public class StripeTestUtils {

    public static final String TEST_WEBHOOK_SECRET = "whsec_test_secret";

    /**
     * Generate valid Stripe webhook signature using HMAC-SHA256.
     * Format: t={timestamp},v1={signature}
     */
    public static String generateWebhookSignature(String payload, String secret) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signedPayload = timestamp + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return "t=" + timestamp + ",v1=" + hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }

    public static String checkoutSessionCompletedEvent(String sessionId, UUID workspaceId, UUID planId) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "checkout.session.completed",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "checkout.session",
                        "status": "complete",
                        "payment_status": "paid",
                        "metadata": {
                            "workspace_id": "%s",
                            "plan_id": "%s"
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), sessionId, workspaceId, planId);
    }

    public static String checkoutSessionExpiredEvent(String sessionId) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "checkout.session.expired",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "checkout.session",
                        "status": "expired"
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), sessionId);
    }

    public static String subscriptionCreatedEvent(String subscriptionId, String customerId,
            UUID workspaceId, UUID planId, long periodStart, long periodEnd) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "customer.subscription.created",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "subscription",
                        "customer": "%s",
                        "status": "active",
                        "cancel_at_period_end": false,
                        "metadata": {
                            "workspace_id": "%s",
                            "plan_id": "%s"
                        },
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "items": {
                            "data": [
                                {
                                    "current_period_start": %d,
                                    "current_period_end": %d,
                                    "price": {
                                        "id": "price_test",
                                        "object": "price"
                                    }
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), subscriptionId, customerId,
            workspaceId, planId, periodStart, periodEnd, periodStart, periodEnd);
    }

    public static String subscriptionUpdatedEvent(String subscriptionId, String status,
            UUID workspaceId, UUID planId, long periodStart, long periodEnd, boolean cancelAtPeriodEnd) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "customer.subscription.updated",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "subscription",
                        "status": "%s",
                        "cancel_at_period_end": %s,
                        "metadata": {
                            "workspace_id": "%s",
                            "plan_id": "%s"
                        },
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "items": {
                            "data": [
                                {
                                    "current_period_start": %d,
                                    "current_period_end": %d,
                                    "price": {
                                        "id": "price_test",
                                        "object": "price"
                                    }
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), subscriptionId, status,
            cancelAtPeriodEnd, workspaceId, planId, periodStart, periodEnd, periodStart, periodEnd);
    }

    public static String subscriptionDeletedEvent(String subscriptionId, UUID workspaceId, UUID planId,
            long periodStart, long periodEnd) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "customer.subscription.deleted",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "subscription",
                        "status": "canceled",
                        "cancel_at_period_end": false,
                        "metadata": {
                            "workspace_id": "%s",
                            "plan_id": "%s"
                        },
                        "current_period_start": %d,
                        "current_period_end": %d,
                        "items": {
                            "data": [
                                {
                                    "current_period_start": %d,
                                    "current_period_end": %d,
                                    "price": {
                                        "id": "price_test",
                                        "object": "price"
                                    }
                                }
                            ]
                        }
                    }
                }
            }
            """,
            UUID.randomUUID().toString().substring(0, 8),
            subscriptionId,
            workspaceId,
            planId,
            periodStart,
            periodEnd,
            periodStart,
            periodEnd
        );
    }

    public static String invoicePaidEvent(String invoiceId, String subscriptionId,
            long amountPaid, String currency, long periodStart, long periodEnd) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "invoice.paid",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "invoice",
                        "status": "paid",
                        "amount_due": %d,
                        "amount_paid": %d,
                        "currency": "%s",
                        "period_start": %d,
                        "period_end": %d,
                        "parent": {
                            "subscription_details": {
                                "subscription": "%s"
                            }
                        },
                        "payments": {
                            "data": [
                                {
                                    "payment": {
                                        "payment_intent": "pi_test_123"
                                    }
                                }
                            ]
                        },
                        "payment_intent": "pi_test_123",
                        "lines": {
                            "data": [
                                {
                                    "description": "Subscription payment"
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), invoiceId,
            amountPaid, amountPaid, currency, periodStart, periodEnd, subscriptionId);
    }

    public static String invoicePaymentFailedEvent(String invoiceId, String subscriptionId,
            long amountDue, String currency) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "invoice.payment_failed",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "invoice",
                        "status": "open",
                        "amount_due": %d,
                        "amount_paid": 0,
                        "currency": "%s",
                        "period_start": %d,
                        "period_end": %d,
                        "subscription": "%s",
                        "parent": {
                            "subscription_details": {
                                "subscription": "%s"
                            }
                        },
                        "payments": {
                            "data": [
                                {
                                    "payment": {
                                        "payment_intent": "pi_test_123"
                                    }
                                }
                            ]
                        },
                        "payment_intent": "pi_test_123",
                        "lines": {
                            "data": [
                                {
                                    "description": "Subscription payment"
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), invoiceId,
            amountDue, currency, Instant.now().getEpochSecond(),
            Instant.now().plusSeconds(2592000).getEpochSecond(), subscriptionId, subscriptionId);
    }

    public static String invoicePaymentActionRequiredEvent(String invoiceId, String subscriptionId,
            long amountDue, String currency, String hostedInvoiceUrl) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "invoice.payment_action_required",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "invoice",
                        "status": "open",
                        "amount_due": %d,
                        "amount_paid": 0,
                        "currency": "%s",
                        "hosted_invoice_url": "%s",
                        "period_start": %d,
                        "period_end": %d,
                        "parent": {
                            "subscription_details": {
                                "subscription": "%s"
                            }
                        },
                        "payments": {
                            "data": [
                                {
                                    "payment": {
                                        "payment_intent": "pi_test_123"
                                    }
                                }
                            ]
                        },
                        "payment_intent": "pi_test_123",
                        "lines": {
                            "data": [
                                {
                                    "description": "Subscription payment"
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), invoiceId,
            amountDue, currency, hostedInvoiceUrl, Instant.now().getEpochSecond(),
            Instant.now().plusSeconds(2592000).getEpochSecond(), subscriptionId);
    }

    public static String invoiceFinalizedEvent(String invoiceId, String subscriptionId,
            long amountDue, String currency) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "invoice.finalized",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "invoice",
                        "status": "open",
                        "amount_due": %d,
                        "amount_paid": 0,
                        "currency": "%s",
                        "period_start": %d,
                        "period_end": %d,
                        "subscription": "%s",
                        "parent": {
                            "subscription_details": {
                                "subscription": "%s"
                            }
                        },
                        "payments": {
                            "data": [
                                {
                                    "payment": {
                                        "payment_intent": "pi_test_123"
                                    }
                                }
                            ]
                        },
                        "payment_intent": "pi_test_123",
                        "lines": {
                            "data": [
                                {
                                    "description": "Subscription payment"
                                }
                            ]
                        }
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), invoiceId,
            amountDue, currency, Instant.now().getEpochSecond(),
            Instant.now().plusSeconds(2592000).getEpochSecond(), subscriptionId, subscriptionId);
    }

    public static String paymentIntentSucceededEvent(String paymentIntentId, long amountReceived) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "payment_intent.succeeded",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "payment_intent",
                        "status": "succeeded",
                        "amount_received": %d
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), paymentIntentId, amountReceived);
    }

    public static String paymentIntentFailedEvent(String paymentIntentId) {
        return String.format("""
            {
                "id": "evt_%s",
                "object": "event",
                "type": "payment_intent.payment_failed",
                "data": {
                    "object": {
                        "id": "%s",
                        "object": "payment_intent",
                        "status": "requires_payment_method"
                    }
                }
            }
            """, UUID.randomUUID().toString().substring(0, 8), paymentIntentId);
    }
}
