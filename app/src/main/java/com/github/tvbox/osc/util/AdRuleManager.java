package com.github.tvbox.osc.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

/**
 * TV-box 广告规则管理器
 * 融合三大开源规则源：
 *  1. uBlock Origin (gorhill/uBlock → uAssets filters)
 *  2. Pi-hole 生态域名黑名单 (StevenBlack/hosts)
 *  3. Cats-Team/AdRules（中文区域广告，主源）
 * 流程：启动先加载本地缓存（离线可用）→ 每 72h 后台拉取三源 → 解析合并（hosts/adblock/纯域名三种格式）
 *       → 原子写入 AdBlocker 内存表 + 落盘缓存。拉取走 OkGo 全信任 SSL client（Android 6 旧 CA 兼容）。
 */
public class AdRuleManager {

    private static final String TAG = "AdRuleManager";
    private static final long REFRESH_INTERVAL_MS = 72L * 3600 * 1000; // 3 天刷新一次
    private static final int MAX_DOMAINS = 350000; // 内存保护上限（32 位进程，防 OOM）

    // 三源 × 三级镜像（ghfast → gh-proxy → 直连）
    private static final String[][] SOURCES = {
            {
                    "https://ghfast.top/https://raw.githubusercontent.com/Cats-Team/AdRules/main/dns.txt",
                    "https://gh-proxy.org/https://raw.githubusercontent.com/Cats-Team/AdRules/main/dns.txt",
                    "https://raw.githubusercontent.com/Cats-Team/AdRules/main/dns.txt",
            },
            {
                    "https://ghfast.top/https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                    "https://gh-proxy.org/https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                    "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            },
            {
                    "https://ghfast.top/https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                    "https://gh-proxy.org/https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                    "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
            },
    };

    private static volatile boolean started = false;
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

    public static void init(Context ctx) {
        if (started) return;
        started = true;
        pool.execute(() -> {
            try {
                // 1) 先加载本地缓存（合并进 AdBlocker，保证离线/弱网可用）
                File cache = getCacheFile(ctx);
                if (cache.exists() && cache.length() > 0) {
                    Set<String> cached = readDomainLines(cache);
                    if (!cached.isEmpty()) {
                        AdBlocker.addAdHosts(cached);
                        LOG.i("echo-adrules cache:" + cached.size());
                    }
                }
                // 2) 刷新节流：72h 内不重复拉取
                File meta = getMetaFile(ctx);
                long last = 0;
                if (meta.exists()) {
                    try {
                        last = Long.parseLong(new String(readBytes(meta), StandardCharsets.UTF_8).trim());
                    } catch (Throwable ignore) {
                    }
                }
                if (System.currentTimeMillis() - last < REFRESH_INTERVAL_MS) return;

                // 3) 拉取三源并解析合并（按源优先级：AdRules → StevenBlack → uAssets，超上限停止合并）
                Set<String> merged = new HashSet<>();
                for (String[] mirrors : SOURCES) {
                    if (merged.size() >= MAX_DOMAINS) break;
                    String text = fetchFirstAvailable(mirrors);
                    if (text == null || text.isEmpty()) continue;
                    Set<String> doms = parseDomains(text);
                    merged.addAll(doms);
                    LOG.i("echo-adrules source:" + mirrors[0].replace("https://", "") + " -> " + doms.size() + ", total:" + merged.size());
                }
                if (merged.isEmpty()) return; // 全部源失败，保留缓存不破坏现有拦截

                // 4) 落盘缓存 + 原子更新内存（保留内置默认/配置列表，增量合并）
                writeDomainLines(cache, merged);
                AdBlocker.addAdHosts(merged);
                writeBytes(meta, String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
                LOG.i("echo-adrules total:" + merged.size());
            } catch (Throwable th) {
                LOG.e("echo-adrules error:" + th.getMessage());
            }
        });
    }

    // ---------- 拉取 ----------

    private static String fetchFirstAvailable(String[] mirrors) {
        for (String url : mirrors) {
            try {
                Request request = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (TVBox)").build();
                Response resp = OkGoHelper.getDefaultClient().newCall(request).execute();
                try {
                    if (resp.isSuccessful()) {
                        String body = resp.body().string();
                        if (body != null && body.length() > 1000) return body;
                    }
                } finally {
                    if (resp.body() != null) resp.body().close();
                }
            } catch (Throwable th) {
                LOG.i("echo-adrules mirror fail:" + url.substring(0, Math.min(url.length(), 60)) + " " + th.getMessage());
            }
        }
        return null;
    }

    // ---------- 解析：hosts / adblock(||domain^) / 纯域名 三种格式 ----------

    static Set<String> parseDomains(String text) {
        Set<String> out = new HashSet<>();
        String[] lines = text.split("\\n");
        for (String raw : lines) {
            String s = raw.trim().toLowerCase();
            if (s.isEmpty()) continue;
            char c0 = s.charAt(0);
            if (c0 == '#' || c0 == '!' || c0 == '[' || c0 == '@') continue; // 注释/例外(@@)跳过
            if (s.contains("##") || s.contains("#@#") || s.contains("#?#")) continue; // 元素隐藏规则跳过
            if (s.contains("+js(")) continue;      // uBlock 脚本注入规则跳过
            if (s.contains("/")) continue;         // 正则/路径规则跳过
            if (s.contains("*") && !s.startsWith("||*.")) continue; // 通配规则仅放行 ||*.domain^
            String domain = null;
            if (s.startsWith("0.0.0.0 ") || s.startsWith("127.0.0.1 ") || s.startsWith("::1 ") || s.startsWith(":: ")) {
                String[] parts = s.split("\\s+");
                if (parts.length >= 2) domain = parts[1]; // hosts 格式取第二列
            } else if (s.startsWith("||")) {
                domain = s.substring(2);                 // adblock ||domain^
                if (domain.endsWith("^")) domain = domain.substring(0, domain.length() - 1);
                if (domain.startsWith("*.")) domain = domain.substring(2);
            } else if (s.indexOf(' ') < 0 && s.indexOf('$') < 0 && s.indexOf(',') < 0 && s.indexOf('=') < 0) {
                domain = s;                              // 纯域名
            }
            if (domain != null && AdBlocker.normalize(domain) != null) {
                out.add(AdBlocker.normalize(domain));
            }
        }
        return out;
    }

    // ---------- 缓存持久化 ----------

    private static File getCacheFile(Context ctx) {
        return new File(ctx.getFilesDir(), "adblock_rules.txt");
    }

    private static File getMetaFile(Context ctx) {
        return new File(ctx.getFilesDir(), "adblock_meta.txt");
    }

    private static Set<String> readDomainLines(File f) {
        Set<String> out = new HashSet<>();
        try {
            for (String line : new String(readBytes(f), StandardCharsets.UTF_8).split("\\n")) {
                String d = AdBlocker.normalize(line.trim());
                if (d != null) out.add(d);
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static void writeDomainLines(File f, Set<String> doms) {
        StringBuilder sb = new StringBuilder(doms.size() * 24);
        for (String d : doms) sb.append(d).append('\n');
        writeBytes(f, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] readBytes(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                byte[] buf = new byte[(int) f.length()];
                int off = 0;
                while (off < buf.length) {
                    int n = in.read(buf, off, buf.length - off);
                    if (n < 0) break;
                    off += n;
                }
                return buf;
            } finally {
                try { in.close(); } catch (Throwable ignore) { }
            }
        } catch (Throwable ignore) {
            return new byte[0];
        }
    }

    private static void writeBytes(File f, byte[] data) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(data);
                out.flush();
            } finally {
                try { out.close(); } catch (Throwable ignore) { }
            }
        } catch (Throwable ignore) {
        }
    }
}
