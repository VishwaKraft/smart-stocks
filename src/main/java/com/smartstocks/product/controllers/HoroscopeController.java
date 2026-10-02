package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.HoroscopeItem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/horoscope")
@CrossOrigin(origins = "*")
public class HoroscopeController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HoroscopeItem>> getHoroscopeJson() {
        String url = "https://www.hindustantimes.com/feeds/rss/astrology/horoscope/rssfeed.xml";
        try {
            String rssFeed = restTemplate.getForObject(url, String.class);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(rssFeed)));
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("item");
            List<HoroscopeItem> items = new ArrayList<>();
            java.util.Set<String> seenZodiacs = new java.util.HashSet<>();

            List<String> validZodiacs = java.util.Arrays.asList(
                    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
                    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
            );

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node node = nList.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    
                    String title = getElementValue(element, "title");
                    String description = getElementValue(element, "description");
                    String link = getElementValue(element, "link");
                    String pubDate = getElementValue(element, "pubDate");

                    // No imageUrl extracted from feed - will be set from static zodiac map below

                    if (title != null) {
                        String[] titleParts = title.split(":", 2);
                        if (titleParts.length == 2) {
                            title = titleParts[1].trim();
                        }
                    }

                    java.util.Map<String, String> zodiacImages = new java.util.LinkedHashMap<>();
                    zodiacImages.put("Aries",       "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2648.png");
                    zodiacImages.put("Taurus",      "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2649.png");
                    zodiacImages.put("Gemini",      "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264a.png");
                    zodiacImages.put("Cancer",      "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264b.png");
                    zodiacImages.put("Leo",         "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264c.png");
                    zodiacImages.put("Virgo",       "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264d.png");
                    zodiacImages.put("Libra",       "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264e.png");
                    zodiacImages.put("Scorpio",     "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/264f.png");
                    zodiacImages.put("Sagittarius", "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2650.png");
                    zodiacImages.put("Capricorn",   "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2651.png");
                    zodiacImages.put("Aquarius",    "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2652.png");
                    zodiacImages.put("Pisces",      "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/2653.png");

                    String zodiacName = null;
                    if (description != null) {
                        String[] parts = description.split("(?i)\\s*horoscope today:\\s*", 2);
                        if (parts.length == 2) {
                            zodiacName = parts[0].trim();
                            if (zodiacName.isEmpty()) {
                                zodiacName = null;
                            } else {
                                zodiacName = zodiacName.substring(0, 1).toUpperCase() + zodiacName.substring(1).toLowerCase();
                            }
                            description = parts[1].trim();
                        }
                    }

                    String imageUrl = zodiacImages.getOrDefault(zodiacName, null);

                    if (zodiacName != null && validZodiacs.contains(zodiacName) && !seenZodiacs.contains(zodiacName)) {
                        seenZodiacs.add(zodiacName);
                        items.add(HoroscopeItem.builder()
                                .title(title)
                                .description(description)
                                .link(link)
                                .pubDate(pubDate)
                                .zodiacName(zodiacName)
                                .imageUrl(imageUrl)
                                .build());
                    }
                }
            }

            items.sort(java.util.Comparator.comparingInt(item -> validZodiacs.indexOf(item.getZodiacName())));

            return ResponseEntity.ok(items);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getElementValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node != null && node.getTextContent() != null) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }
}
