package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.NewsDto;
import com.smartstocks.product.models.NewsArticle;
import com.smartstocks.product.repository.NewsArticleRepository;
import com.smartstocks.product.service.INewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import java.util.stream.Collectors;

/**
 * Fetches Indian news from free Google News RSS feeds and major Indian publications.
 * No API keys required. Inspired by https://github.com/balakrishnanbsk/newsapp
 */
@Service
public class NewsServiceImpl implements INewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    private final RestTemplate restTemplate;
    private final HttpEntity<?> headers;
    private final NewsArticleRepository newsArticleRepository;

    // ── RSS Feed configuration ──────────────────────────────────────────

    /**
     * A simple holder for feed name + URL.
     */
    private static class RssFeed {
        final String name;
        final String url;

        RssFeed(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    /**
     * Google News RSS for Indian locale (English).
     */
    private static RssFeed googleNewsTopHeadlines() {
        return new RssFeed("Google News",
                "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en");
    }

    /**
     * Google News RSS for a specific topic section.
     */
    private static RssFeed googleNewsTopic(String topicId) {
        return new RssFeed("Google News " + topicId,
                "https://news.google.com/rss/headlines/section/topic/" + topicId
                        + "?hl=en-IN&gl=IN&ceid=IN:en");
    }

    /**
     * Category → list of RSS feeds.  Publication feeds (which include images) come first,
     * Google News topic section is appended as a reliable fallback.
     */
    private static final Map<String, List<RssFeed>> CATEGORY_FEEDS = new LinkedHashMap<>();

    static {
        // ── General / Top Headlines ─────────────────────────────────────
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

        // ── Business ────────────────────────────────────────────────────
        CATEGORY_FEEDS.put("business", Arrays.asList(
                new RssFeed("NDTV Profit", "https://feeds.feedburner.com/ndtvprofit-latest"),
                new RssFeed("Indian Express Business", "https://indianexpress.com/section/business/feed/"),
                new RssFeed("HT Business", "https://www.hindustantimes.com/feeds/rss/business/rssfeed.xml"),
                new RssFeed("TOI Business", "https://timesofindia.indiatimes.com/rss/feedstopstories/business.cms"),
                googleNewsTopic("BUSINESS")
        ));

        // ── Technology (also covers science) ────────────────────────────
        CATEGORY_FEEDS.put("technology", Arrays.asList(
                new RssFeed("Gadgets 360", "https://feeds.feedburner.com/gadgets360-latest"),
                new RssFeed("Indian Express Tech", "https://indianexpress.com/section/technology/feed/"),
                new RssFeed("HT Tech", "https://www.hindustantimes.com/feeds/rss/technology/rssfeed.xml"),
                new RssFeed("TOI Tech", "https://timesofindia.indiatimes.com/rss/feedstopstories/technology.cms"),
                googleNewsTopic("TECHNOLOGY")
        ));

        // ── Sports ──────────────────────────────────────────────────────
        CATEGORY_FEEDS.put("sports", Arrays.asList(
                new RssFeed("NDTV Sports", "https://feeds.feedburner.com/ndtvsports-latest"),
                new RssFeed("Indian Express Sports", "https://indianexpress.com/section/sports/feed/"),
                new RssFeed("HT Sports", "https://www.hindustantimes.com/feeds/rss/sports/rssfeed.xml"),
                new RssFeed("TOI Sports", "https://timesofindia.indiatimes.com/rss/feedstopstories/sports.cms"),
                googleNewsTopic("SPORTS")
        ));

        // ── Entertainment ───────────────────────────────────────────────
        CATEGORY_FEEDS.put("entertainment", Arrays.asList(
                new RssFeed("NDTV Movies", "https://feeds.feedburner.com/ndtvmovies-latest"),
                new RssFeed("Indian Express Entertainment", "https://indianexpress.com/section/entertainment/feed/"),
                new RssFeed("HT Entertainment", "https://www.hindustantimes.com/feeds/rss/entertainment/rssfeed.xml"),
                new RssFeed("TOI Entertainment", "https://timesofindia.indiatimes.com/rss/feedstopstories/entertainment.cms"),
                new RssFeed("India Today Entertainment", "https://www.indiatoday.in/rss/entertainment"),
                googleNewsTopic("ENTERTAINMENT")
        ));

        // ── Health ──────────────────────────────────────────────────────
        CATEGORY_FEEDS.put("health", Arrays.asList(
                new RssFeed("Indian Express Health", "https://indianexpress.com/section/lifestyle/health/feed/"),
                new RssFeed("HT Health", "https://www.hindustantimes.com/feeds/rss/lifestyle/health/rssfeed.xml"),
                new RssFeed("TOI Health", "https://timesofindia.indiatimes.com/rss/feedstopstories/health.cms"),
                googleNewsTopic("HEALTH")
        ));

        // ── Science → alias to technology feeds ─────────────────────────
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
        List<RssFeed> feeds = CATEGORY_FEEDS.getOrDefault(category,
                CATEGORY_FEEDS.get("general"));

        List<NewsDto> allArticles = new ArrayList<>();

        for (RssFeed feed : feeds) {
            try {
                List<NewsDto> articles = fetchRssFeed(feed);
                allArticles.addAll(articles);
            } catch (Exception e) {
                // Log and continue — one feed failure shouldn't break the entire request
                log.warn("Failed to fetch RSS feed '{}': {}", feed.name, e.getMessage());
            }
        }

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

        return allArticles;
    }

    // ── RSS Fetching & Parsing ──────────────────────────────────────────

    /**
     * Fetches a single RSS feed and parses its XML into NewsDto objects.
     */
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

    /**
     * Parses RSS XML text into a list of NewsDto objects.
     * Handles standard RSS 2.0 format with media extensions.
     */
    private List<NewsDto> parseRssXml(String xml, String sourceName) {
        List<NewsDto> articles = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
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

                // Try to extract image from multiple possible locations
                String imageUrl = extractImage(item);

                if (title.isBlank() || link.isBlank()) {
                    continue; // Skip items without title or link
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
            // If XML parsing fails entirely, try regex-based fallback
            log.warn("XML parser failed for source '{}', trying regex fallback: {}",
                    sourceName, e.getMessage());
            articles.addAll(parseRssWithRegex(xml, sourceName));
        }

        return articles;
    }

    /**
     * Regex-based RSS parser as fallback when XML parsing fails
     * (some feeds have malformed XML but valid RSS structure).
     */
    private List<NewsDto> parseRssWithRegex(String xml, String sourceName) {
        List<NewsDto> articles = new ArrayList<>();

        java.util.regex.Pattern itemPattern =
                java.util.regex.Pattern.compile("<item>(.*?)</item>",
                        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = itemPattern.matcher(xml);

        while (matcher.find()) {
            String block = matcher.group(1);

            String title = stripHtml(extractTagRegex(block, "title"));
            String link = extractTagRegex(block, "link");
            String description = stripHtml(extractTagRegex(block, "description"));
            String pubDate = extractTagRegex(block, "pubDate");

            // Image from enclosure or media tags
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

    /**
     * Gets text content of the first child element with the given tag name.
     */
    private String getTagContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    /**
     * Tries to extract an image URL from enclosure, media:content,
     * media:thumbnail, or img tags within an RSS item element.
     */
    private String extractImage(Element item) {
        // 1. Check <enclosure> with type="image/*"
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enclosures.getLength(); i++) {
            Element enc = (Element) enclosures.item(i);
            String url = enc.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        // 2. Check <media:content>
        NodeList mediaContent = item.getElementsByTagName("media:content");
        for (int i = 0; i < mediaContent.getLength(); i++) {
            Element mc = (Element) mediaContent.item(i);
            String url = mc.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        // 3. Check <media:thumbnail>
        NodeList mediaThumbnail = item.getElementsByTagName("media:thumbnail");
        for (int i = 0; i < mediaThumbnail.getLength(); i++) {
            Element mt = (Element) mediaThumbnail.item(i);
            String url = mt.getAttribute("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }

        // 4. Try to find <img src="..."> inside description
        String desc = getTagContent(item, "description");
        if (desc != null) {
            java.util.regex.Matcher imgMatcher = java.util.regex.Pattern
                    .compile("<img[^>]+src=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(desc);
            if (imgMatcher.find()) {
                return imgMatcher.group(1);
            }
        }

        return null;
    }

    // ── Regex Helpers ───────────────────────────────────────────────────

    /**
     * Extracts content from an XML tag using regex.
     * Handles both CDATA-wrapped and plain content.
     */
    private String extractTagRegex(String block, String tag) {
        // Try CDATA first
        java.util.regex.Pattern cdataPattern = java.util.regex.Pattern.compile(
                "<" + tag + "[^>]*><!\\[CDATA\\[([\\s\\S]*?)]]></" + tag + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = cdataPattern.matcher(block);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Plain content
        java.util.regex.Pattern plainPattern = java.util.regex.Pattern.compile(
                "<" + tag + "[^>]*>([\\s\\S]*?)</" + tag + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        m = plainPattern.matcher(block);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    /**
     * Extracts image URL from enclosure, media:content, media:thumbnail,
     * or img tags using regex.
     */
    private String extractImageRegex(String block) {
        // enclosure url
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<enclosure[^>]+url=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(block);
        if (m.find()) return m.group(1);

        // media:content url
        m = java.util.regex.Pattern
                .compile("<media:content[^>]+url=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(block);
        if (m.find()) return m.group(1);

        // media:thumbnail url
        m = java.util.regex.Pattern
                .compile("<media:thumbnail[^>]+url=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(block);
        if (m.find()) return m.group(1);

        // img src
        m = java.util.regex.Pattern
                .compile("<img[^>]+src=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(block);
        if (m.find()) return m.group(1);

        return null;
    }

    // ── HTML Stripping ──────────────────────────────────────────────────

    /**
     * Strips HTML tags, CDATA wrappers, and decodes common HTML entities.
     * Ported from the reference app's stripHtml function.
     */
    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        String text = html;

        // Remove CDATA wrappers
        text = text.replaceAll("<!\\[CDATA\\[([\\s\\S]*?)]]>", "$1");

        // Strip HTML tags (first pass)
        text = text.replaceAll("<[^>]*>", "");

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

        // Decode remaining numeric character references &#NNN; via loop
        java.util.regex.Pattern numEntity = java.util.regex.Pattern.compile("&#(\\d+);");
        java.util.regex.Matcher numMatcher = numEntity.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (numMatcher.find()) {
            try {
                int code = Integer.parseInt(numMatcher.group(1));
                numMatcher.appendReplacement(sb,
                        java.util.regex.Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (NumberFormatException e) {
                numMatcher.appendReplacement(sb, " ");
            }
        }
        numMatcher.appendTail(sb);
        text = sb.toString();

        // Decode hex character references &#xHHH;
        java.util.regex.Pattern hexEntity = java.util.regex.Pattern.compile(
                "&#x([0-9a-fA-F]+);");
        java.util.regex.Matcher hexMatcher = hexEntity.matcher(text);
        sb = new StringBuilder();
        while (hexMatcher.find()) {
            try {
                int code = Integer.parseInt(hexMatcher.group(1), 16);
                hexMatcher.appendReplacement(sb,
                        java.util.regex.Matcher.quoteReplacement(String.valueOf((char) code)));
            } catch (NumberFormatException e) {
                hexMatcher.appendReplacement(sb, " ");
            }
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        // Remove any remaining named entities
        text = text.replaceAll("&[a-zA-Z]+;", " ");

        // Strip tags again (catches encoded tags that were decoded above)
        text = text.replaceAll("<[^>]*>", "");

        // Collapse whitespace and trim
        text = text.replaceAll("\\s+", " ").trim();

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
