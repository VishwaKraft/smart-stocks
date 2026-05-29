package com.smartstocks.product.service;

import com.smartstocks.product.dto.NewsDto;

import java.util.List;

public interface INewsService {

    List<NewsDto> fetchAndStoreNews(String category);
}
