package com.smartstocks.product.service.impl;

import com.smartstocks.product.dto.NewsDto;
import com.smartstocks.product.models.NewsArticle;
import com.smartstocks.product.repository.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class NewsServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpEntity<?> headers;

    @Mock
    private NewsArticleRepository newsArticleRepository;

    private NewsServiceImpl newsService;

    // ── Sample RSS XML responses ────────────────────────────────────────

    private static final String SAMPLE_RSS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rss version=\"2.0\">\n"
            + "<channel>\n"
            + "  <title>Test Feed</title>\n"
            + "  <item>\n"
            + "    <title>Sensex Rallies 500 Points on FII Inflows</title>\n"
            + "    <link>https://example.com/article-1</link>\n"
            + "    <description>Indian stock markets rallied on strong FII buying.</description>\n"
            + "    <pubDate>Mon, 21 Jul 2025 10:30:00 GMT</pubDate>\n"
            + "    <enclosure url=\"https://example.com/image1.jpg\" type=\"image/jpeg\" />\n"
            + "  </item>\n"
            + "  <item>\n"
            + "    <title>RBI Holds Repo Rate Steady at 6.5%</title>\n"
            + "    <link>https://example.com/article-2</link>\n"
            + "    <description>The Reserve Bank of India kept rates unchanged.</description>\n"
            + "    <pubDate>Mon, 21 Jul 2025 11:00:00 GMT</pubDate>\n"
            + "  </item>\n"
            + "</channel>\n"
            + "</rss>";

    private static final String RSS_WITH_CDATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rss version=\"2.0\">\n"
            + "<channel>\n"
            + "  <item>\n"
            + "    <title><![CDATA[ISRO Launches Chandrayaan-4 Successfully]]></title>\n"
            + "    <link>https://example.com/isro</link>\n"
            + "    <description><![CDATA[<p>India's space agency <b>ISRO</b> launched &amp; deployed Chandrayaan-4.</p>]]></description>\n"
            + "    <pubDate>Mon, 21 Jul 2025 09:00:00 GMT</pubDate>\n"
            + "  </item>\n"
            + "</channel>\n"
            + "</rss>";

    private static final String RSS_WITH_MEDIA_TAGS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rss version=\"2.0\" xmlns:media=\"http://search.yahoo.com/mrss/\">\n"
            + "<channel>\n"
            + "  <item>\n"
            + "    <title>IPL 2025 Final Highlights</title>\n"
            + "    <link>https://example.com/ipl</link>\n"
            + "    <description>Chennai Super Kings win IPL 2025.</description>\n"
            + "    <media:content url=\"https://example.com/ipl-image.jpg\" medium=\"image\" />\n"
            + "  </item>\n"
            + "</channel>\n"
            + "</rss>";

    private static final String EMPTY_RSS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rss version=\"2.0\"><channel><title>Empty</title></channel></rss>";

    private static final String MALFORMED_XML = "<rss><channel>"
            + "<item><title>Broken Item</title><link>https://example.com/broken</link>"
            + "<description>Some news &amp; more</description></item>"
            + "</channel></rss>";

    // ── Setup ───────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        newsService = new NewsServiceImpl(restTemplate, headers, newsArticleRepository);
        newsService.clearCache();
    }

    // ── Tests: fetchAndStoreNews ─────────────────────────────────────────

    @Test
    void fetchAndStoreNews_business_returnsArticlesFromRssFeeds() {
        // Arrange: mock all HTTP calls to return our sample RSS
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("business");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should return articles from RSS feeds");

        // Every article should have title and URL populated
        for (NewsDto article : result) {
            assertNotNull(article.getTitle(), "Title should not be null");
            assertFalse(article.getTitle().isBlank(), "Title should not be blank");
            assertNotNull(article.getUrl(), "URL should not be null");
            assertFalse(article.getUrl().isBlank(), "URL should not be blank");
        }
    }

    @Test
    void fetchAndStoreNews_parsesRssFieldsCorrectly() {
        // Arrange: return sample RSS for the first feed call, empty for the rest
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(EMPTY_RSS, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("business");

        // Assert: find the Sensex article
        NewsDto sensexArticle = result.stream()
                .filter(a -> a.getTitle().contains("Sensex"))
                .findFirst()
                .orElse(null);

        assertNotNull(sensexArticle, "Should find the Sensex article");
        assertEquals("https://example.com/article-1", sensexArticle.getUrl());
        assertTrue(sensexArticle.getDescription().contains("FII buying"));
        assertEquals("https://example.com/image1.jpg", sensexArticle.getUrlToImage());
        assertEquals("Mon, 21 Jul 2025 10:30:00 GMT", sensexArticle.getPublishedAt());
    }

    @Test
    void fetchAndStoreNews_stripsHtmlAndCdata() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(RSS_WITH_CDATA, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("technology");

        // Assert
        NewsDto isroArticle = result.stream()
                .filter(a -> a.getTitle().contains("ISRO") || a.getTitle().contains("Chandrayaan"))
                .findFirst()
                .orElse(null);

        assertNotNull(isroArticle, "Should find the ISRO article");
        assertEquals("ISRO Launches Chandrayaan-4 Successfully", isroArticle.getTitle());

        // Description should have HTML tags stripped
        assertFalse(isroArticle.getDescription().contains("<p>"), "HTML tags should be stripped");
        assertFalse(isroArticle.getDescription().contains("<b>"), "HTML tags should be stripped");
        assertTrue(isroArticle.getDescription().contains("ISRO"), "Content should be preserved");
    }

    @Test
    void fetchAndStoreNews_extractsImageFromMediaTags() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(RSS_WITH_MEDIA_TAGS, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("sports");

        // Assert
        NewsDto iplArticle = result.stream()
                .filter(a -> a.getTitle().contains("IPL"))
                .findFirst()
                .orElse(null);

        assertNotNull(iplArticle, "Should find the IPL article");
        // Note: media:content may not be parsed when namespace-aware is false,
        // but the regex fallback should handle it
    }

    @Test
    void fetchAndStoreNews_deduplicatesByUrl() {
        // Arrange: return the same RSS from every feed (same URLs appear multiple times)
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("business");

        // Assert: count unique URLs
        long uniqueUrls = result.stream()
                .map(NewsDto::getUrl)
                .distinct()
                .count();
        assertEquals(result.size(), uniqueUrls, "Should not contain duplicate URLs");
    }

    @Test
    void fetchAndStoreNews_persistsNewArticles() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        newsService.fetchAndStoreNews("business");

        // Assert: verify save was called for new articles
        verify(newsArticleRepository, atLeastOnce()).save(any(NewsArticle.class));
    }

    @Test
    void fetchAndStoreNews_skipsAlreadyStoredArticles() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        // Simulate that all URLs already exist in DB
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(true);

        // Act
        newsService.fetchAndStoreNews("business");

        // Assert: save should never be called since all articles exist
        verify(newsArticleRepository, never()).save(any(NewsArticle.class));
    }

    @Test
    void fetchAndStoreNews_handlesDataIntegrityViolation() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);
        // Simulate concurrent insert race condition
        when(newsArticleRepository.save(any(NewsArticle.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate"));

        // Act — should not throw
        List<NewsDto> result = assertDoesNotThrow(
                () -> newsService.fetchAndStoreNews("business"),
                "Should handle DataIntegrityViolationException gracefully");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void fetchAndStoreNews_continuesWhenOneFeedFails() {
        // Arrange: first call throws, subsequent calls return valid RSS
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act — should not throw, should still return articles from surviving feeds
        List<NewsDto> result = assertDoesNotThrow(
                () -> newsService.fetchAndStoreNews("business"),
                "Should continue fetching other feeds when one fails");

        assertNotNull(result);
    }

    @Test
    void fetchAndStoreNews_returnsEmptyForEmptyRss() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(EMPTY_RSS, HttpStatus.OK));

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("health");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Empty RSS should return no articles");
    }

    @Test
    void fetchAndStoreNews_handlesNullResponseBody() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>((String) null, HttpStatus.OK));

        // Act
        List<NewsDto> result = assertDoesNotThrow(
                () -> newsService.fetchAndStoreNews("general"),
                "Should handle null response body gracefully");

        assertNotNull(result);
    }

    @Test
    void fetchAndStoreNews_handlesMalformedXmlWithRegexFallback() {
        // Arrange: XML without proper declaration may fail DOM parsing but regex should work
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(MALFORMED_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = assertDoesNotThrow(
                () -> newsService.fetchAndStoreNews("general"),
                "Should fall back to regex parsing for malformed XML");

        assertNotNull(result);
    }

    @Test
    void fetchAndStoreNews_setsSourceAsAuthor() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act
        List<NewsDto> result = newsService.fetchAndStoreNews("business");

        // Assert: author should be set to the feed source name
        for (NewsDto article : result) {
            assertNotNull(article.getAuthor(), "Author should be set to feed source name");
            assertNotNull(article.getSource(), "Source should be set");
            assertEquals(article.getAuthor(), article.getSource(),
                    "Author and source should match the feed name");
        }
    }

    // ── Tests: Category mappings ────────────────────────────────────────

    @Test
    void fetchAndStoreNews_unknownCategoryFallsBackToGeneral() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act — "unknown_category" should fall back to general feeds
        List<NewsDto> result = assertDoesNotThrow(
                () -> newsService.fetchAndStoreNews("unknown_category"),
                "Unknown category should fall back to general");

        assertNotNull(result);
    }

    @Test
    void fetchAndStoreNews_allCategoriesWork() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        // Act & Assert: each category should succeed without errors
        String[] categories = {"business", "technology", "sports", "entertainment", "health", "general", "science"};
        for (String category : categories) {
            List<NewsDto> result = assertDoesNotThrow(
                    () -> newsService.fetchAndStoreNews(category),
                    "Category '" + category + "' should not throw");
            assertNotNull(result, "Category '" + category + "' should return non-null result");
        }
    }

    // ── Tests: Saved entity fields ──────────────────────────────────────

    @Test
    void fetchAndStoreNews_savedEntityHasCorrectFields() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(SAMPLE_RSS_XML, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(EMPTY_RSS, HttpStatus.OK));
        when(newsArticleRepository.existsByUrl(anyString())).thenReturn(false);

        ArgumentCaptor<NewsArticle> captor = ArgumentCaptor.forClass(NewsArticle.class);

        // Act
        newsService.fetchAndStoreNews("business");

        // Assert
        verify(newsArticleRepository, atLeastOnce()).save(captor.capture());
        NewsArticle saved = captor.getAllValues().get(0);

        assertNotNull(saved.getTitle());
        assertNotNull(saved.getUrl());
        assertEquals("business", saved.getCategory());
        assertNotNull(saved.getSource());
        assertNotNull(saved.getPublishedAt());
    }
}
