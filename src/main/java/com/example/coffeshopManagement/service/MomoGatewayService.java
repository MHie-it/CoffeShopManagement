package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoGatewayService {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Logger log = LoggerFactory.getLogger(MomoGatewayService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String endpoint;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String returnUrl;
    private final String notifyUrl;
    private final String requestType;
    private final boolean skipSignatureValidation;

    public MomoGatewayService(
            ObjectMapper objectMapper,
            @Value("${app.momo.enabled:false}") boolean enabled,
            @Value("${app.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}") String endpoint,
            @Value("${app.momo.partner-code:}") String partnerCode,
            @Value("${app.momo.access-key:}") String accessKey,
            @Value("${app.momo.secret-key:}") String secretKey,
            @Value("${app.momo.return-url:${app.momo.redirect-url:http://localhost:3000/payment/momo/return}}") String returnUrl,
            @Value("${app.momo.notify-url:${app.momo.ipn-url:http://localhost:8080/api/payments/momo/callback}}") String notifyUrl,
            @Value("${app.momo.request-type:captureMoMoWallet}") String requestType,
            @Value("${app.momo.skip-signature-validation:false}") boolean skipSignatureValidation) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.returnUrl = returnUrl;
        this.notifyUrl = notifyUrl;
        this.requestType = requestType;
        this.skipSignatureValidation = skipSignatureValidation;
    }

    public MomoCreateResult createPayment(String momoOrderId, BigDecimal amount, String orderInfo) {
        if (!enabled) {
            log.warn("MoMo create payment rejected because feature is disabled");
            throw new BadRequestException("MoMo payment is disabled");
        }
        if (isBlank(partnerCode) || isBlank(accessKey) || isBlank(secretKey)) {
            log.warn("MoMo create payment rejected because configuration is incomplete");
            throw new BadRequestException("MoMo configuration is incomplete");
        }

        String requestId = partnerCode + "-" + System.currentTimeMillis();
        String amountText = amount.toPlainString();
        String extraData = "";
        String normalizedOrderInfo = isBlank(orderInfo) ? "Thanh toan don hang" : orderInfo.trim();
        String effectiveRequestType = resolveRequestType();
        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + amountText
                + "&extraData=" + extraData
                + "&ipnUrl=" + notifyUrl
                + "&orderId=" + momoOrderId
                + "&orderInfo=" + normalizedOrderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + returnUrl
                + "&requestId=" + requestId
                + "&requestType=" + effectiveRequestType;
        String signature = hmacSha256(secretKey, rawSignature);

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", partnerCode);
        payload.put("partnerName", "Coffee Shop");
        payload.put("storeId", "CoffeeShop");
        payload.put("requestId", requestId);
        payload.put("amount", Long.parseLong(amountText));
        payload.put("orderId", momoOrderId);
        payload.put("orderInfo", normalizedOrderInfo);
        payload.put("redirectUrl", returnUrl);
        payload.put("ipnUrl", notifyUrl);
        payload.put("lang", "vi");
        payload.put("extraData", extraData);
        payload.put("requestType", effectiveRequestType);
        payload.put("autoCapture", true);
        payload.put("signature", signature);

        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = objectMapper.readTree(response.body());
            int resultCode = node.path("resultCode").asInt(-1);
            log.info(
                    "MoMo create payment response: httpStatus={}, resultCode={}, orderId={}, requestId={}, message={}",
                    response.statusCode(),
                    resultCode,
                    momoOrderId,
                    requestId,
                    node.path("message").asText(""));
            if (resultCode != 0) {
                String msg = node.path("message").asText("MoMo create payment failed");
                throw new BadRequestException("MoMo create payment failed: " + msg);
            }

            MomoCreateResult result = new MomoCreateResult();
            result.requestId = requestId;
            result.momoOrderId = momoOrderId;
            result.payUrl = node.path("payUrl").asText(null);
            result.deeplink = node.path("deeplink").asText(null);
            result.qrCodeUrl = node.path("qrCodeUrl").asText(null);
            log.info(
                    "MoMo create payment success: orderId={}, requestId={}, hasPayUrl={}, hasDeeplink={}, hasQrCode={}",
                    momoOrderId,
                    requestId,
                    result.payUrl != null && !result.payUrl.isBlank(),
                    result.deeplink != null && !result.deeplink.isBlank(),
                    result.qrCodeUrl != null && !result.qrCodeUrl.isBlank());
            return result;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("MoMo create payment failed: orderId={}, requestId={}", momoOrderId, requestId, ex);
            throw new BadRequestException("Cannot connect to MoMo gateway");
        }
    }

    public boolean verifyCallbackSignature(Map<String, String> params) {
        if (skipSignatureValidation) {
            log.warn("MoMo callback signature validation is skipped by configuration (for local testing only)");
            return true;
        }

        String provided = params.getOrDefault("signature", "");
        if (provided.isBlank()) {
            log.warn("MoMo callback missing signature: orderId={}", params.getOrDefault("orderId", ""));
            return false;
        }

        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + params.getOrDefault("amount", "")
                + "&extraData=" + params.getOrDefault("extraData", "")
                + "&message=" + params.getOrDefault("message", "")
                + "&orderId=" + params.getOrDefault("orderId", "")
                + "&orderInfo=" + params.getOrDefault("orderInfo", "")
                + "&orderType=" + params.getOrDefault("orderType", "")
                + "&partnerCode=" + params.getOrDefault("partnerCode", "")
                + "&payType=" + params.getOrDefault("payType", "")
                + "&requestId=" + params.getOrDefault("requestId", "")
                + "&responseTime=" + params.getOrDefault("responseTime", "")
                + "&resultCode=" + params.getOrDefault("resultCode", "")
                + "&transId=" + params.getOrDefault("transId", "");
        String expected = hmacSha256(secretKey, rawSignature);
        boolean matched = expected.equals(provided);
        String providedTail = provided.length() > 8 ? provided.substring(provided.length() - 8) : provided;
        String expectedTail = expected.length() > 8 ? expected.substring(expected.length() - 8) : expected;
        log.info(
                "MoMo callback signature check: orderId={}, requestId={}, matched={}, providedTail={}, expectedTail={}",
                params.getOrDefault("orderId", ""),
                params.getOrDefault("requestId", ""),
                matched,
                providedTail,
                expectedTail);
        return matched;
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create HMAC signature", ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveRequestType() {
        if (endpoint != null
                && endpoint.contains("/v2/")
                && "captureMoMoWallet".equalsIgnoreCase(requestType)) {
            log.warn("MoMo requestType '{}' is incompatible with v2 endpoint, auto-switching to 'captureWallet'",
                    requestType);
            return "captureWallet";
        }
        return requestType;
    }

    public static class MomoCreateResult {
        private String requestId;
        private String momoOrderId;
        private String payUrl;
        private String deeplink;
        private String qrCodeUrl;

        public String getRequestId() {
            return requestId;
        }

        public String getMomoOrderId() {
            return momoOrderId;
        }

        public String getPayUrl() {
            return payUrl;
        }

        public String getDeeplink() {
            return deeplink;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }
    }
}
