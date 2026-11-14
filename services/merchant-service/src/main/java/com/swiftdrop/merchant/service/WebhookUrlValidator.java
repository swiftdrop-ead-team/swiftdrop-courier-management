package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.exception.MerchantConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class WebhookUrlValidator {

    private final boolean allowLocalhost;

    public WebhookUrlValidator(@Value("${merchant.webhook.allow-localhost:false}") boolean allowLocalhost) {
        this.allowLocalhost = allowLocalhost;
    }

    public URI validate(String value) {
        final URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new MerchantConflictException("Webhook URL is invalid");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost();
        if (!uri.isAbsolute() || host == null || uri.getRawUserInfo() != null
                || uri.getFragment() != null || uri.getQuery() != null
                || !(scheme.equals("https") || (allowLocalhost && scheme.equals("http")))) {
            throw new MerchantConflictException("Webhook URL must be an absolute HTTPS URL without credentials or query");
        }
        if (allowLocalhost && isLocalhost(host)) {
            return uri;
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new MerchantConflictException("Webhook URL uses a disallowed port");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new MerchantConflictException("Webhook URL host did not resolve");
            }
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    throw new MerchantConflictException("Webhook URL must resolve only to public IP addresses");
                }
            }
        } catch (UnknownHostException ex) {
            throw new MerchantConflictException("Webhook URL host could not be resolved");
        }
        return uri;
    }

    private boolean isLocalhost(String host) {
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return !(first == 0 || first == 10 || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && (second == 0 || second == 168))
                    || (first == 198 && (second == 18 || second == 19 || second == 51))
                    || (first == 203 && second == 0 && third == 113)
                    || first >= 224);
        }
        if (address instanceof Inet6Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            int fourth = Byte.toUnsignedInt(bytes[3]);
            boolean globalUnicast = (first & 0xE0) == 0x20;
            boolean documentation = first == 0x20 && second == 0x01 && third == 0x0D && fourth == 0xB8;
            boolean transitionAddress = first == 0x20 && second == 0x01 && third == 0x00
                    || first == 0x20 && second == 0x02
                    || first == 0x3F && second == 0xFF;
            return globalUnicast && !documentation && !transitionAddress;
        }
        return false;
    }
}
