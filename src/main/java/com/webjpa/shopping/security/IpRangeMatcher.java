package com.webjpa.shopping.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public final class IpRangeMatcher {

    private final List<CidrRange> ranges;

    public IpRangeMatcher(String csvRanges) {
        this.ranges = parseRanges(csvRanges);
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public boolean contains(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        byte[] candidate = parseAddress(ipAddress.trim());
        if (candidate == null) {
            return false;
        }
        return ranges.stream().anyMatch(range -> range.contains(candidate));
    }

    private static List<CidrRange> parseRanges(String csvRanges) {
        if (csvRanges == null || csvRanges.isBlank()) {
            return List.of();
        }
        return csvRanges.lines()
                .flatMap(line -> List.of(line.split(",")).stream())
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(IpRangeMatcher::parseRange)
                .toList();
    }

    private static CidrRange parseRange(String rawRange) {
        String[] parts = rawRange.split("/", 2);
        byte[] address = parseAddress(parts[0].trim());
        if (address == null) {
            throw new IllegalArgumentException("Invalid IP range: " + rawRange);
        }

        int maxPrefixLength = address.length * 8;
        int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1].trim()) : maxPrefixLength;
        if (prefixLength < 0 || prefixLength > maxPrefixLength) {
            throw new IllegalArgumentException("Invalid IP range prefix: " + rawRange);
        }

        return new CidrRange(address, prefixLength);
    }

    private static byte[] parseAddress(String value) {
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private record CidrRange(byte[] network, int prefixLength) {

        boolean contains(byte[] candidate) {
            if (candidate.length != network.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != network[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);
            return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
