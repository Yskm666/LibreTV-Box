package com.github.tvbox.osc.util;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * TV-box 广告域名拦截器
 * 支持三大规则源融合（uBlock Origin / Pi-hole(StevenBlack hosts) / Cats-Team AdRules）
 * 匹配策略：域名精确 + 子域后缀链匹配（O(1) HashSet），本地代理与纯 IP 永不拦截
 */
public class AdBlocker {
    private static volatile Set<String> AD_HOSTS = new HashSet<>();

    public static void clear() {
        AD_HOSTS = new HashSet<>();
    }

    public static boolean isEmpty() {
        return AD_HOSTS.isEmpty();
    }

    public static void addAdHost(String host) {
        String h = normalize(host);
        if (h != null) {
            Set<String> merged = new HashSet<>(AD_HOSTS);
            merged.add(h);
            AD_HOSTS = merged;
        }
    }

    public static void addAdHosts(Collection<String> hosts) {
        if (hosts == null || hosts.isEmpty()) return;
        Set<String> merged = new HashSet<>(AD_HOSTS);
        for (String h : hosts) {
            String n = normalize(h);
            if (n != null) merged.add(n);
        }
        AD_HOSTS = merged;
    }

    public static boolean hasHost(String host) {
        String h = normalize(host);
        return h != null && AD_HOSTS.contains(h);
    }

    public static boolean isAd(String url) {
        if (url == null) return false;
        String host = extractHost(url);
        if (host == null || host.isEmpty()) return false;
        if (isIp(host)) return false;               // 本地代理/内网 IP/源站 IP 一律放行
        Set<String> hosts = AD_HOSTS;
        if (hosts.contains(host)) return true;
        int dot = host.indexOf('.');
        while (dot >= 0) {                          // 子域链匹配：x.ad.com → ad.com
            host = host.substring(dot + 1);
            if (hosts.contains(host)) return true;
            dot = host.indexOf('.');
        }
        return false;
    }

    private static boolean isIp(String host) {
        int dots = 0;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c == '.') dots++;
            else if (c < '0' || c > '9') return false;
        }
        return dots == 3;
    }

    static String extractHost(String url) {
        try {
            if (url.startsWith("data:")) return null;
            int scheme = url.indexOf("://");
            String rest = scheme >= 0 ? url.substring(scheme + 3) : url;
            int slash = rest.indexOf('/');
            if (slash >= 0) rest = rest.substring(0, slash);
            int colon = rest.indexOf(':');
            if (colon >= 0) rest = rest.substring(0, colon);
            int at = rest.indexOf('@');
            if (at >= 0) rest = rest.substring(at + 1);
            rest = rest.trim();
            return rest.isEmpty() ? null : rest.toLowerCase();
        } catch (Throwable th) {
            return null;
        }
    }

    static String normalize(String host) {
        if (host == null) return null;
        String h = host.trim().toLowerCase();
        if (h.isEmpty()) return null;
        if (h.startsWith("||")) h = h.substring(2);
        if (h.startsWith("*.")) h = h.substring(2);
        if (h.endsWith("^")) h = h.substring(0, h.length() - 1);
        if (h.indexOf('.') < 0) return null;        // 必须含点（域名），滤掉顶级域/单段
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-')) {
                return null;                        // 含特殊字符（/ * $ , ^ ! : # 空格等）一律拒绝
            }
        }
        return h;
    }

    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
}
