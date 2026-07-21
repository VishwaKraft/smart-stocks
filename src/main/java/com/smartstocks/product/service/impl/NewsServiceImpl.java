package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.NewsDto;
import com.smartstocks.product.models.NewsArticle;
import com.smartstocks.product.repository.NewsArticleRepository;
import com.smartstocks.product.service.INewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fetches Indian news from free Google News RSS feeds and major Indian publications.
 * No API keys required. Inspired by https://github.com/balakrishnanbsk/newsapp
 *
 * Performance optimizations:
 *  1. In-memory cache with 5-minute TTL — repeat requests served instantly
 *  2. Parallel feed fetching — all RSS feeds fetched concurrently via thread pool
 *  3. Per-feed timeout — slow feeds are abandoned after 5 seconds
 *  4. Pre-compiled regex patterns — no recompilation per call
 */
@Service
public class NewsServiceImpl implements INewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    private final RestTemplate restTemplate;
    private final HttpEntity<?> headers;
    private final NewsArticleRepository newsArticleRepository;

    // ── Performance: Thread pool for parallel feed fetching ──────────────
    private static final ExecutorService RSS_EXECUTOR = new ThreadPoolExecutor(
            4, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            r -> {
                Thread t = new Thread(r, "rss-fetch");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /** Timeout per individual RSS feed fetch (seconds). */
    private static final int FEED_TIMEOUT_SECONDS = 5;

    // ── Performance: In-memory cache ────────────────────────────────────
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private static final ConcurrentHashMap<String, CacheEntry> NEWS_CACHE = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final List<NewsDto> articles;
        final long timestamp;

        CacheEntry(List<NewsDto> articles) {
            this.articles = articles;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /** Clears the in-memory news cache. Package-private for test access. */
    void clearCache() {
        NEWS_CACHE.clear();
    }

    // ── Performance: Pre-compiled regex patterns ────────────────────────
    private static final Pattern CDATA_PATTERN = Pattern.compile("<!\\[CDATA\\[([\\s\\S]*?)]]>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern NUM_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");
    private static final Pattern HEX_ENTITY_PATTERN = Pattern.compile("&#x([0-9a-fA-F]+);", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z]+;");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern ITEM_PATTERN = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCLOSURE_URL_PATTERN = Pattern.compile("<enclosure[^>]+url=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEDIA_CONTENT_URL_PATTERN = Pattern.compile("<media:content[^>]+url=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEDIA_THUMB_URL_PATTERN = Pattern.compile("<media:thumbnail[^>]+url=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    // ── RSS Feed configuration ──────────────────────────────────────────

    private static class RssFeed {
        final String name;
        final String url;

        RssFeed(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private static RssFeed googleNewsTopHeadlines() {
        return new RssFeed("Google News",
                "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en");
    }

    private static RssFeed googleNewsTopic(String topicId) {
        return new RssFeed("Google News " + topicId,
                "https://news.google.com/rss/headlines/section/topic/" + topicId
                        + "?hl=en-IN&gl=IN&ceid=IN:en");
    }

    private static final Map<String, List<RssFeed>> CATEGORY_FEEDS = new LinkedHashMap<>();

    static {
        CATEGORY_FEEDS.put("general", Arrays.asList(
                googleNewsTopHeadlines(),
                new RssFeed("NDTV", "https://feeds.feedburner.com/ndtvnews-top-stories"),
                new RssFeed("NDTV Latest", "https://feeds.feedburner.com/ndtvnews-latest"),
                new RssFeed("NDTV India", "https://feeds.feedburner.com/ndtvnews-india-news"),
                new RssFeed("The Hindu", "https://www.thehindu.com/news/national/feeder/default.rss"),
                new RssFeed("Times of India", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
                new RssFeed("Indian Express", "https://indianexpress.com/section/india/feed/"),
                new RssFeed("India Today", "https://www.indiatoday.in/rss/home"),
                new RssFeed("Hindustan Times", "https://www.hindustantimes.com/feeds/rss/india-news/rssfeed.xml"),
                new RssFeed("Mint", "https://www.livemint.com/rss/news"),
                new RssFeed("Economic Times", "https://economictimes.indiatimes.com/rssfeedstopstories.cms"),
                new RssFeed("News18", "https://www.news18.com/rss/india.xml"),
                new RssFeed("Zee News", "https://zeenews.india.com/rss/india-national-news.xml"),
                new RssFeed("Business Standard", "https://www.business-standard.com/rss/home_page_top_stories.rss")
        ));

        CATEGORY_FEEDS.put("business", Arrays.asList(
                new RssFeed("NDTV Profit", "https://feeds.feedburner.com/ndtvprofit-latest"),
                new RssFeed("Indian Express Business", "https://indianexpress.com/section/business/feed/"),
                new RssFeed("HT Business", "https://www.hindustantimes.com/feeds/rss/business/rssfeed.xml"),
                new RssFeed("TOI Business", "https://timesofindia.indiatimes.com/rss/feedstopstories/business.cms"),
                googleNewsTopic("BUSINESS")
        ));

        CATEGORY_FEEDS.put("technology", Arrays.asList(
                new RssFeed("Gadgets 360", "https://feeds.feedburner.com/gadgets360-latest"),
                new RssFeed("Indian Express Tech", "https://indianexpress.com/section/technology/feed/"),
                new RssFeed("HT Tech", "https://www.hindustantimes.com/feeds/rss/technology/rssfeed.xml"),
                new RssFeed("TOI Tech", "https://timesofindia.indiatimes.com/rss/feedstopstories/technology.cms"),
                googleNewsTopic("TECHNOLOGY")
        ));

        CATEGORY_FEEDS.put("sports", Arrays.asList(
                new RssFeed("NDTV Sports", "https://feeds.feedburner.com/ndtvsports-latest"),
                new RssFeed("Indian Express Sports", "https://indianexpress.com/section/sports/feed/"),
                new RssFeed("HT Sports", "https://www.hindustantimes.com/feeds/rss/sports/rssfeed.xml"),
                new RssFeed("TOI Sports", "https://timesofindia.indiatimes.com/rss/feedstopstories/sports.cms"),
                googleNewsTopic("SPORTS")
        ));

        CATEGORY_FEEDS.put("entertainment", Arrays.asList(
                new RssFeed("NDTV Movies", "https://feeds.feedburner.com/ndtvmovies-latest"),
                new RssFeed("Indian Express Entertainment", "https://indianexpress.com/section/entertainment/feed/"),
                new RssFeed("HT Entertainment", "https://www.hindustantimes.com/feeds/rss/entertainment/rssfeed.xml"),
                new RssFeed("TOI Entertainment", "https://timesofindia.indiatimes.com/rss/feedstopstories/entertainment.cms"),
                new RssFeed("India Today Entertainment", "https://www.indiatoday.in/rss/entertainment"),
                googleNewsTopic("ENTERTAINMENT")
        ));

        CATEGORY_FEEDS.put("health", Arrays.asList(
                new RssFeed("Indian Express Health", "https://indianexpress.com/section/lifestyle/health/feed/"),
                new RssFeed("HT Health", "https://www.hindustantimes.com/feeds/rss/lifestyle/health/rssfeed.xml"),
                new RssFeed("TOI Health", "https://timesofindia.indiatimes.com/rss/feedstopstories/health.cms"),
                googleNewsTopic("HEALTH")
        ));

        CATEGORY_FEEDS.put("science", CATEGORY_FEEDS.get("technology"));
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public NewsServiceImpl(
            RestTemplate restTemplate,
            HttpEntity<?> headers,
            NewsArticleRepository newsArticleRepository) {
        this.restTemplate = restTemplate;
        this.headers = headers;
        this.newsArticleRepository = newsArticleRepository;
    }

    // ── Public API ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<NewsDto> fetchAndStoreNews(String category) {
        // ── Optimization 1: Return cached data if fresh ─────────────────
        CacheEntry cached = NEWS_CACHE.get(category);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for category '{}' ({} articles)", category, cached.articles.size());
            return cached.articles;
        }

        List<RssFeed> feeds = CATEGORY_FEEDS.getOrDefault(category,
                CATEGORY_FEEDS.get("general"));

        // ── Optimization 2: Fetch all feeds in parallel ─────────────────
        List<CompletableFuture<List<NewsDto>>> futures = feeds.stream()
                .map(feed -> CompletableFuture.supplyAsync(() -> fetchRssFeed(feed), RSS_EXECUTOR)
                        // Optimization 3: Per-feed timeout — abandon slow feeds
                        .completeOnTimeout(Collections.emptyList(), FEED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("Failed to fetch RSS feed '{}': {}", feed.name, ex.getMessage());
                            return Collections.emptyList();
                        }))
                .collect(Collectors.toList());

        // Wait for all feeds (bounded by the per-feed timeout)
        List<NewsDto> allArticles = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Deduplicate by URL
        Set<String> seenUrls = new LinkedHashSet<>();
        allArticles = allArticles.stream()
                .filter(a -> a.getUrl() != null && !a.getUrl().isBlank())
                .filter(a -> seenUrls.add(a.getUrl()))
                .collect(Collectors.toList());

        // Persist new articles
        for (NewsDto article : allArticles) {
            persistIfNew(article, category);
        }

        // ── Cache the result ────────────────────────────────────────────
        NEWS_CACHE.put(category, new CacheEntry(Collections.unmodifiableList(allArticles)));
        log.info("Fetched {} articles for category '{}' from {} feeds in parallel",
                allArticles.size(), category, feeds.size());

        return allArticles;
    }

    // ── RSS Fetching & Parsing ──────────────────────────────────────────

    private List<NewsDto> fetchRssFeed(RssFeed feed) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    feed.url, HttpMethod.GET, headers, String.class);

            String xml = response.getBody();
            if (xml == null || xml.isBlank()) {
                return Collections.emptyList();
            }

            return parseRssXml(xml, feed.name);
        } catch (Exception e) {
            log.warn("HTTP error fetching '{}' ({}): {}", feed.name, feed.url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<NewsDto> parseRssXml(String xml, String sourceName) {
        List<NewsDto> articles = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);

                String title = stripHtml(getTagContent(item, "title"));
                String link = getTagContent(item, "link");
                String description = stripHtml(getTagContent(item, "description"));
                String pubDate = getTagContent(item, "pubDate");
                String imageUrl = extractImage(item);

                if (title.isBlank() || link.isBlank()) {
                    continue;
                }

                NewsDto dto = new NewsDto();
                dto.setTitle(title);
                dto.setUrl(link);
                dto.setDescription(description.isBlank() ? title : description);
                dto.setAuthor(sourceName);
                dto.setContent(description);
                dto.setUrlToImage(imageUrl);
                dto.setPublishedAt(pubDate);
                dto.setSource(sourceName);

                articles.add(dto);
            }
        } catch (Exception e) {
            log.warn("XML parser failed for source '{}', trying regex fallback: {}",
                    sourceName, e.getMessage());
            articles.addAll(parseRssWithRegex(xml, sourceName));
        }

        return articles;
    }

    private List<NewsDto> parseRssWithRegex(String xml, String sourceName) {
        List<NewsDto> articles = new ArrayList<>();
        Matcher matcher = ITEM_PATTERN.matcher(xml);

        while (matcher.find()) {
            String block = matcher.group(1);

            String title = stripHtml(extractTagRegex(block, "title"));
            String link = extractTagRegex(block, "link");
            String description = stripHtml(extractTagRegex(block, "description"));
            String pubDate = extractTagRegex(block, "pubDate");
            String imageUrl = extractImageRegex(block);

            if (title.isBlank() || link.isBlank()) {
                continue;
            }

            NewsDto dto = new NewsDto();
            dto.setTitle(title);
            dto.setUrl(link);
            dto.setDescription(description.isBlank() ? title : description);
            dto.setAuthor(sourceName);
            dto.setContent(description);
            dto.setUrlToImage(imageUrl);
            dto.setPublishedAt(pubDate);
            dto.setSource(sourceName);

            articles.add(dto);
        }

        return articles;
    }

    // ── XML Helpers ─────────────────────────────────────────────────────

    private String getTagContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    private String extractImage(Element item) {
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enclosures.getLength(); i++) {
            Element enc = (Element) enclosures.item(i);
            String url = enc.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        NodeList mediaContent = item.getElementsByTagName("media:content");
        for (int i = 0; i < mediaContent.getLength(); i++) {
            Element mc = (Element) mediaContent.item(i);
            String url = mc.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        NodeList mediaThumbnail = item.getElementsByTagName("media:thumbnail");
        for (int i = 0; i < mediaThumbnail.getLength(); i++) {
            Element mt = (Element) mediaThumbnail.item(i);
            String url = mt.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        String desc = getTagContent(item, "description");
        if (desc != null) {
            Matcher imgMatcher = IMG_SRC_PATTERN.matcher(desc);
            if (imgMatcher.find()) {
                return imgMatcher.group(1);
            }
        }

        return null;
    }

    // ── Regex Helpers (using pre-compiled patterns) ─────────────────────

    private String extractTagRegex(String block, String tag) {
        // Try CDATA first
        Pattern cdataPattern = Pattern.compile(
                "<" + tag + "[^>]*><!\\[CDATA\\[([\\s\\S]*?)]]></" + tag + ">",
                Pattern.CASE_INSENSITIVE);
        Matcher m = cdataPattern.matcher(block);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Plain content
        Pattern plainPattern = Pattern.compile(
                "<" + tag + "[^>]*>([\\s\\S]*?)</" + tag + ">",
                Pattern.CASE_INSENSITIVE);
        m = plainPattern.matcher(block);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    private String extractImageRegex(String block) {
        Matcher m = ENCLOSURE_URL_PATTERN.matcher(block);
        if (m.find()) return m.group(1);

        m = MEDIA_CONTENT_URL_PATTERN.matcher(block);
        if (m.find()) return m.group(1);

        m = MEDIA_THUMB_URL_PATTERN.matcher(block);
        if (m.find()) return m.group(1);

        m = IMG_SRC_PATTERN.matcher(block);
        if (m.find()) return m.group(1);

        return null;
    }

    // ── HTML Stripping (pre-compiled patterns) ──────────────────────────

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String text = html;

        // Remove CDATA wrappers
        text = CDATA_PATTERN.matcher(text).replaceAll("$1");

        // Strip HTML tags (first pass)
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&#x27;", "'")
                .replace("&#8217;", "\u2019")
                .replace("&#8216;", "\u2018")
                .replace("&#8220;", "\u201C")
                .replace("&#8221;", "\u201D")
                .replace("&#8230;", "\u2026")
                .replace("&#8211;", "\u2013")
                .replace("&#8212;", "\u2014");

        // Decode remaining numeric character references &#NNN;
        Matcher numMatcher = NUM_ENTITY_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (numMatcher.find()) {
            try {
                int code = Integer.parseInt(numMatcher.group(1));
                numMatcher.appendReplacement(sb,
                        Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (NumberFormatException e) {
                numMatcher.appendReplacement(sb, " ");
            }
        }
        numMatcher.appendTail(sb);
        text = sb.toString();

        // Decode hex character references &#xHHH;
        Matcher hexMatcher = HEX_ENTITY_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (hexMatcher.find()) {
            try {
                int code = Integer.parseInt(hexMatcher.group(1), 16);
                hexMatcher.appendReplacement(sb,
                        Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (NumberFormatException e) {
                hexMatcher.appendReplacement(sb, " ");
            }
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        // Remove any remaining named entities
        text = NAMED_ENTITY_PATTERN.matcher(text).replaceAll(" ");

        // Strip tags again (catches encoded tags that were decoded above)
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // Collapse whitespace and trim
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();

        // Limit to 300 characters
        if (text.length() > 300) {
            text = text.substring(0, 300);
        }

        return text;
    }

    // ── Persistence ─────────────────────────────────────────────────────

    private void persistIfNew(NewsDto dto, String category) {
        if (dto.getUrl() == null || dto.getUrl().isBlank()) {
            return;
        }
        if (newsArticleRepository.existsByUrl(dto.getUrl())) {
            return;
        }

        NewsArticle entity = new NewsArticle();
        entity.setUrl(dto.getUrl());
        entity.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        entity.setDescription(dto.getDescription());
        entity.setAuthor(dto.getAuthor());
        entity.setContent(dto.getContent());
        entity.setUrlToImage(dto.getUrlToImage());
        entity.setCategory(category);
        entity.setPublishedAt(dto.getPublishedAt());
        entity.setSource(dto.getSource());

        try {
            newsArticleRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // Another request stored the same URL concurrently.
        }
    }
}
