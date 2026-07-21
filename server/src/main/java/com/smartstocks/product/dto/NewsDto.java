package com.smartstocks.product.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsDto {

    private String title;
    private String url;
    private String description;
    private String author;
    private String content;
    private String urlToImage;
    private String publishedAt;
    private String source;

}
