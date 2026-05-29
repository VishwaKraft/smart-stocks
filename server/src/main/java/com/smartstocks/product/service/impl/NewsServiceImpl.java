package com.smartstocks.product.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstocks.product.dto.NewsDto;
import com.smartstocks.product.models.NewsArticle;
import com.smartstocks.product.repository.NewsArticleRepository;
import com.smartstocks.product.service.INewsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class NewsServiceImpl implements INewsService {

    private final RestTemplate restTemplate;
    private final HttpEntity<?> headers;
    private final NewsArticleRepository newsArticleRepository;
    private final String newsBaseUrl;
    private final String newsApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsServiceImpl(
            RestTemplate restTemplate,
            HttpEntity<?> headers,
            NewsArticleRepository newsArticleRepository,
            @Value("${upstream.news.baseurl}") String newsBaseUrl,
            @Value("${upstream.news.apikey}") String newsApiKey) {
        this.restTemplate = restTemplate;
        this.headers = headers;
        this.newsArticleRepository = newsArticleRepository;
        this.newsBaseUrl = newsBaseUrl;
        this.newsApiKey = newsApiKey;
    }

    @Override
    @Transactional
    public List<NewsDto> fetchAndStoreNews(String category) {
        String newsUrl = newsBaseUrl + "?category=" + category + "&apiKey=" + newsApiKey;
        ResponseEntity<JsonNode> response = restTemplate.exchange(newsUrl, HttpMethod.GET, headers, JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null || body.get("articles") == null || body.get("articles").isEmpty()) {
            return Collections.emptyList();
        }

        List<NewsDto> articles = objectMapper.convertValue(
                body.get("articles"),
                new TypeReference<List<NewsDto>>() {}
        );

        for (NewsDto article : articles) {
            persistIfNew(article, category);
        }

        return new LinkedList<>(articles);
    }

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

        try {
            newsArticleRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // Another request stored the same URL concurrently.
        }
    }
}
