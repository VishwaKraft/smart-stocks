package com.smartstocks.product.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstocks.product.dto.*;
import com.smartstocks.product.models.SearchTable;
import com.smartstocks.product.repository.StockRepository;
import com.smartstocks.product.service.ISerachTableService;
import com.smartstocks.product.util.ResponseMessage;
import com.smartstocks.product.util.UtilityMethods;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/stock")
@CrossOrigin(origins = "*")
public class StockController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${upstream.company.url}")
    private String companyUrl;

    @Value("${upstream.chart.url}")
    private String chartUrl;

    @Value("${upstream.rapid.url}")
    private String rapidUrl;

    @Value("${upstream.recommend.url}")
    private String recommendedUrl;

    @Value("${upstream.details.url}")
    private String detailsUrl;

    @Value("${upstream.news.baseurl}")
    private String newsBaseUrl;
    
    @Value("${upstream.news.apikey}")
    private String newsApiKey;

    @Value("${upstream.search.url}")
    private String searchUrl;

    @Autowired
    private HttpEntity headers;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ISerachTableService searchTableService;

    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/all")
    public ResponseEntity allStocks(@RequestParam("pageNo") int pageNo, @RequestParam("size") int size) throws IOException {
        Page<SearchTable> page = searchTableService.allStocks(pageNo, size);
        ObjectMapper mapper = new ObjectMapper();
        List<RecommendedStocks> ans = new LinkedList<>();
        for(SearchTable symbol : page.getContent()) {
            ResponseEntity<JsonNode> response = this.restTemplate.exchange(companyUrl + symbol.getSymbol() + "?region=IN&modules=summaryDetail,financialData,quoteType",
                    HttpMethod.GET, headers, JsonNode.class);
            if(response.getStatusCode() == HttpStatus.OK) {
                RecommendedStocks ele = mapper.readerFor(new TypeReference<RecommendedStocks>(){}).
                        readValue(response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail"));
                mapper.readerForUpdating(ele).
                        readValue(response.getBody().get("quoteSummary").get("result").get(0).get("financialData"));
                mapper.readerForUpdating(ele).
                        readValue(response.getBody().get("quoteSummary").get("result").get(0).get("quoteType"));
                DetailValuesDto[] changeValues = UtilityMethods.calculateChange(
                        response.getBody().get("quoteSummary").get("result").get(0).get("financialData").get("currentPrice").get("raw").asDouble(),
                        response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail").get("previousClose").get("raw").asDouble());
                ele.setChange(changeValues[0]);
                ele.setChangePercentage(changeValues[1]);
                ele.setSymbol(symbol.getSymbol());
                ans.add(ele);
            }
        }
        RootResponseDto<List<RecommendedStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/search")
    public ResponseEntity mySearch(@RequestParam("q") String query, @RequestParam("limit") int limit) {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(searchUrl + "?lang=en&region=US&query=" + query,
                HttpMethod.GET, headers, JsonNode.class);
        List<SearchStocksDto> ans = new LinkedList<>();
        JsonNode body = response.getBody();
        if(!body.get("ResultSet").isNull() && !body.get("ResultSet").isEmpty()) {
            JsonNode resultSet = body.get("ResultSet");
            if(!resultSet.get("Result").isNull() && !resultSet.get("Result").isEmpty()) {
                JsonNode result = resultSet.get("Result");
                System.out.println(result);
                    for(int i=0; i<Math.min(result.size(), limit); i++) {
                        SearchStocksDto ele = new SearchStocksDto();
                        if(!result.get(i).get("exch").isNull()) ele.setExchange(result.get(i).get("exch").asText());
                        if(!result.get(i).get("symbol").isNull()) ele.setSymbol(result.get(i).get("symbol").asText());
                        if(!result.get(i).get("name").isNull()) {
                            ele.setShortName(result.get(i).get("name").asText());
                            ele.setLongName(result.get(i).get("name").asText());
                        }
                        ans.add(ele);
                    }
            }
        }
        RootResponseDto<List<SearchStocksDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/graph/{symbol}")
    public ResponseEntity getGraphData(@PathVariable("symbol") String symbol, @RequestParam("range") String range, @RequestParam("interval") String interval) throws IOException {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(chartUrl + symbol + "?range="+ range +"&region=IN&interval="+ interval +"&lang=en",
                HttpMethod.GET, headers, JsonNode.class);
        List<ChartInstanceDto> ans = new LinkedList<>();
        JsonNode body = response.getBody();
        if(!body.get("chart").isNull() && !body.get("chart").isEmpty()) {
            JsonNode chart = body.get("chart");
            if(!chart.get("result").isNull() && !chart.get("result").isEmpty()) {
                JsonNode result = chart.get("result");
                if(!result.get(0).get("timestamp").isNull() && !result.get(0).get("timestamp").isEmpty()) {
                    int size = result.get(0).get("timestamp").size();
                    for(int i=0; i<size; i++) {
                        ChartInstanceDto ele = new ChartInstanceDto();
                        ele.setDate(result.get(0).get("timestamp").get(i).asLong());
                        if(!result.get(0).get("indicators").isNull() && !result.get(0).get("indicators").isEmpty()) {
                            JsonNode indicators = result.get(0).get("indicators");
                            if(!indicators.get("quote").isNull() && !indicators.get("quote").isEmpty()) {
                                JsonNode quote = indicators.get("quote");
                                if(!quote.get(0).get("low").isNull() && !quote.get(0).get("low").isEmpty()) {
                                    ele.setLow(quote.get(0).get("low").get(i).asDouble());
                                }
                                if(!quote.get(0).get("high").isNull() && !quote.get(0).get("high").isEmpty()) {
                                    ele.setHigh(quote.get(0).get("high").get(i).asDouble());
                                }
                                if(!quote.get(0).get("volume").isNull() && !quote.get(0).get("volume").isEmpty()) {
                                    ele.setVolume(quote.get(0).get("volume").get(i).asDouble());
                                }
                                if(!quote.get(0).get("close").isNull() && !quote.get(0).get("close").isEmpty()) {
                                    ele.setClose(quote.get(0).get("close").get(i).asDouble());
                                }
                                if(!quote.get(0).get("open").isNull() && !quote.get(0).get("open").isEmpty()) {
                                    ele.setOpen(quote.get(0).get("open").get(i).asDouble());
                                }
                            }
                        }
                        ans.add(ele);
                    }
                }
            }
        }
        RootResponseDto<List<ChartInstanceDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/details")
    public ResponseEntity getCompanyProfile(@RequestParam("symbol") String symbol) throws IOException {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(companyUrl + symbol + "?region=IN&modules=assetProfile,summaryDetail,financialData,defaultKeyStatistics,quoteType",
                HttpMethod.GET, headers, JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        CompanyProfileDto ans = new CompanyProfileDto();
        double currentPrice = 0, previousClose = 0;
        if(!response.getBody().get("quoteSummary").isNull() && !response.getBody().get("quoteSummary").isEmpty()) {
            if(!response.getBody().get("quoteSummary").get("result").isNull() && !response.getBody().get("quoteSummary").get("result").isEmpty()) {
                if(!response.getBody().get("quoteSummary").get("result").get(0).get("assetProfile").isNull()) {
                    mapper.readerForUpdating(ans)
                            .readValue(response.getBody().get("quoteSummary").get("result").get(0).get("assetProfile"));
                }
                if(!response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail").isNull()) {
                    mapper.readerForUpdating(ans).
                            readValue(response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail"));
                    if(!response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail").get("previousClose").isNull()) {
                        previousClose = response.getBody().get("quoteSummary").get("result").get(0).get("summaryDetail").get("previousClose").get("raw").asDouble();
                    }
                }
                if(!response.getBody().get("quoteSummary").get("result").get(0).get("financialData").isNull()) {
                    mapper.readerForUpdating(ans).
                            readValue(response.getBody().get("quoteSummary").get("result").get(0).get("financialData"));
                    if(!response.getBody().get("quoteSummary").get("result").get(0).get("financialData").get("currentPrice").isNull()) {
                        currentPrice = response.getBody().get("quoteSummary").get("result").get(0).get("financialData").get("currentPrice").get("raw").asDouble();
                    }
                }
                if(!response.getBody().get("quoteSummary").get("result").get(0).get("defaultKeyStatistics").isNull()) {
                    mapper.readerForUpdating(ans).
                            readValue(response.getBody().get("quoteSummary").get("result").get(0).get("defaultKeyStatistics"));
                }
                if(!response.getBody().get("quoteSummary").get("result").get(0).get("quoteType").isNull()) {
                    mapper.readerForUpdating(ans).
                            readValue(response.getBody().get("quoteSummary").get("result").get(0).get("quoteType"));
                }
            }
        }
        DetailValuesDto[] changeValues = UtilityMethods.calculateChange(currentPrice, previousClose);
        ans.setChange(changeValues[0]);
        ans.setChangePercentage(changeValues[1]);
        RootResponseDto<CompanyProfileDto> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/news-types")
    public ResponseEntity getNewsTypes() {
        List<NewsTypeDto> newsTypes = new LinkedList<>();
        newsTypes.add(new NewsTypeDto("business", "Business", "Business and financial news"));
        newsTypes.add(new NewsTypeDto("technology", "Technology", "Technology and innovation news"));
        newsTypes.add(new NewsTypeDto("general", "General", "General news"));
        newsTypes.add(new NewsTypeDto("health", "Health", "Health and medical news"));
        newsTypes.add(new NewsTypeDto("science", "Science", "Science and research news"));
        newsTypes.add(new NewsTypeDto("sports", "Sports", "Sports news"));
        newsTypes.add(new NewsTypeDto("entertainment", "Entertainment", "Entertainment news"));
        
        RootResponseDto<List<NewsTypeDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, newsTypes);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/news")
    public ResponseEntity getNews(@RequestParam(value = "type", required = false, defaultValue = "business") String type) throws IOException {
        try {
            // Validate news type
            List<String> validTypes = Arrays.asList("business", "technology", "general", "health", "science", "sports", "entertainment");
            if (!validTypes.contains(type.toLowerCase())) {
                Map<String, String> errors = new HashMap<>();
                errors.put("error", "Invalid news type: " + type + ". Valid types are: " + String.join(", ", validTypes));
                RootResponseDto<String> errorResponse = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                        ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
                return new ResponseEntity<>(errorResponse, new HttpHeaders(), 400);
            }
            
            String newsUrl = newsBaseUrl + "?category=" + type + "&apiKey=" + newsApiKey;
            ResponseEntity<JsonNode> response = this.restTemplate.exchange(newsUrl, HttpMethod.GET, headers, JsonNode.class);
            ObjectMapper mapper = new ObjectMapper();
            List<NewsDto> ans = new LinkedList<>();
            if(!response.getBody().get("articles").isNull() && !response.getBody().get("articles").isEmpty()) {
                ans = mapper.readerFor(new TypeReference<List<NewsDto>>(){})
                        .readValue(response.getBody().get("articles"));
            }
            RootResponseDto<List<NewsDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Failed to fetch news: " + e.getMessage());
            RootResponseDto<String> errorResponse = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
            return new ResponseEntity<>(errorResponse, new HttpHeaders(), 500);
        }
    }

    @GetMapping("/recommended-stocks/{symbol}")
    public ResponseEntity getRecommendedStocks(@PathVariable("symbol") String symbol) throws IOException {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(recommendedUrl + symbol,
                HttpMethod.GET, headers, JsonNode.class);
        List<RecommendedStocks> ans = new LinkedList<>();
        JsonNode body = response.getBody();
        if(!body.get("finance").isNull() && !body.get("finance").isEmpty()) {
            JsonNode finance = body.get("finance");
            if(!finance.get("result").isNull() && !finance.get("result").isEmpty()) {
                JsonNode result= finance.get("result");
                if(!result.get(0).get("recommendedSymbols").isNull() && !result.get(0).get("recommendedSymbols").isEmpty()) {
                    JsonNode recommendedSymbols = result.get(0).get("recommendedSymbols");
                    if(!recommendedSymbols.isNull() && !recommendedSymbols.isEmpty()) {
                        int size = recommendedSymbols.size();
                        String symbols = "";
                        for (int i=0; i<size; i++) {
                            if(!recommendedSymbols.get(i).isNull() && !recommendedSymbols.get(i).isEmpty()) {
                                if(!recommendedSymbols.get(i).get("symbol").isNull()) {
                                    RecommendedStocks ele = new RecommendedStocks();
                                    ele.setSymbol(recommendedSymbols.get(i).get("symbol").asText());
                                    ans.add(ele);
                                    symbols += (recommendedSymbols.get(i).get("symbol").asText() + ",");
                                }
                            }
                        }
                        ResponseEntity<JsonNode> nestResponse = this.restTemplate.exchange(detailsUrl + "?region=IN&lang=en&symbols=" + symbols,
                                HttpMethod.GET, headers, JsonNode.class);
                        ObjectMapper mapper = new ObjectMapper();
                        if(!nestResponse.getBody().get("quoteResponse").isNull() && !nestResponse.getBody().get("quoteResponse").isEmpty()) {
                            if (!nestResponse.getBody().get("quoteResponse").get("result").isNull()) {
                                for(int i=0; i<ans.size(); i++) {
                                    RecommendedStocks ele = ans.get(i);
                                    double currentPrice = 0, previousClose = 0;
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("longName").isNull()) {
                                        ele.setLongName(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("longName").asText());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("shortName").isNull()) {
                                        ele.setShortName(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("shortName").asText());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").isNull()) {
                                        DetailValuesDto nestEle = new DetailValuesDto(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").asText());
                                        ele.setCurrentPrice(nestEle);
                                        currentPrice = Double.parseDouble(nestEle.getFmt());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").isNull()) {
                                        DetailValuesDto nestEle = new DetailValuesDto(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").asText());
                                        ele.setPreviousClose(nestEle);
                                        previousClose = Double.parseDouble(nestEle.getFmt());
                                    }
                                    DetailValuesDto[] change = UtilityMethods.calculateChange(currentPrice, previousClose);
                                    ele.setChange(change[0]);
                                    ele.setChangePercentage(change[1]);
                                }
                            }
                        }
                    }
                }
            }
        }
        RootResponseDto<List<RecommendedStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/peers-stocks/{symbol}")
    public ResponseEntity peersStocks(@PathVariable("symbol") String symbol) throws IOException {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(rapidUrl + "/stock/v2/get-recommendations?symbol=" + symbol,
                HttpMethod.GET, headers, JsonNode.class);
        List<RecommendedStocks> ans = new LinkedList<>();
        JsonNode body = response.getBody();
        if(!body.get("finance").isNull() && !body.get("finance").isEmpty()) {
            JsonNode finance = body.get("finance");
            if(!finance.get("result").isNull() && !finance.get("result").isEmpty()) {
                JsonNode result= finance.get("result");
                if(!result.get(0).get("quotes").isNull() && !result.get(0).get("quotes").isEmpty()) {
                    JsonNode quotes = result.get(0).get("quotes");
                    if(!quotes.isNull() && !quotes.isEmpty()) {
                        int size = quotes.size();
                        String symbols = "";
                        for (int i=0; i<size; i++) {
                            if(!quotes.get(i).isNull() && !quotes.get(i).isEmpty()) {
                                if(!quotes.get(i).get("symbol").isNull()) {
                                    RecommendedStocks ele = new RecommendedStocks();
                                    ele.setSymbol(quotes.get(i).get("symbol").asText());
                                    ans.add(ele);
                                    symbols += (quotes.get(i).get("symbol").asText() + ",");
                                }
                            }
                        }
                        ResponseEntity<JsonNode> nestResponse = this.restTemplate.exchange(detailsUrl + "?region=IN&lang=en&symbols=" + symbols,
                                HttpMethod.GET, headers, JsonNode.class);
                        ObjectMapper mapper = new ObjectMapper();
                        if(!nestResponse.getBody().get("quoteResponse").isNull() && !nestResponse.getBody().get("quoteResponse").isEmpty()) {
                            if (!nestResponse.getBody().get("quoteResponse").get("result").isNull()) {
                                for(int i=0; i<ans.size(); i++) {
                                    RecommendedStocks ele = ans.get(i);
                                    double currentPrice = 0, previousClose = 0;
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("longName").isNull()) {
                                        ele.setLongName(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("longName").asText());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("shortName").isNull()) {
                                        ele.setShortName(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("shortName").asText());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").isNull()) {
                                        DetailValuesDto nestEle = new DetailValuesDto(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").asText());
                                        ele.setCurrentPrice(nestEle);
                                        currentPrice = Double.parseDouble(nestEle.getFmt());
                                    }
                                    if(!nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").isNull()) {
                                        DetailValuesDto nestEle = new DetailValuesDto(nestResponse.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").asText());
                                        ele.setPreviousClose(nestEle);
                                        previousClose = Double.parseDouble(nestEle.getFmt());
                                    }
                                    DetailValuesDto[] change = UtilityMethods.calculateChange(currentPrice, previousClose);
                                    ele.setChange(change[0]);
                                    ele.setChangePercentage(change[1]);
                                }
                            }
                        }
                    }
                }
            }
        }
        RootResponseDto<List<RecommendedStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/trends/{symbol}")
    public ResponseEntity trendsStocks(@PathVariable("symbol") String symbol) throws IOException {
        ResponseEntity<JsonNode> response = this.restTemplate.exchange(rapidUrl + "/stock/v2/get-summary?region=IN&symbol=" + symbol,
                HttpMethod.GET, headers, JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        List<TrendDto> ans = new LinkedList<>();
        if(!response.getBody().get("recommendationTrend").isNull() && !response.getBody().get("recommendationTrend").isEmpty()) {
            if(!response.getBody().get("recommendationTrend").get("trend").isNull() && !response.getBody().get("recommendationTrend").get("trend").isEmpty()) {
                ans = mapper.readerFor(new TypeReference<List<TrendDto>>(){})
                        .readValue(response.getBody().get("recommendationTrend").get("trend"));
            }
        }
        RootResponseDto<List<TrendDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/top")
    public ResponseEntity getTopGainersAndLosers(@RequestParam("days") int days, @RequestParam("type") TopGainerOrTopLoser type) {
        List<TopStocks> ans = (type.toString() == "TOP_LOSER") ? stockRepository.topLosers(days) : stockRepository.topGainers(days);
        if(!ans.isEmpty()) {
            String symbols = "";
            for(int i=0; i<ans.size(); i++) symbols += (ans.get(i).getSymbol() + ",");
            ResponseEntity<JsonNode> response = this.restTemplate.exchange( detailsUrl + "?region=IN&lang=en&symbols=" + symbols.substring(0, symbols.length()-1),
                    HttpMethod.GET, headers, JsonNode.class);
            if(!response.getBody().get("quoteResponse").isNull() && !response.getBody().get("quoteResponse").isEmpty()) {
                if(!response.getBody().get("quoteResponse").get("result").isNull()) {
                    for(int j=0; j<response.getBody().get("quoteResponse").get("result").size(); j++) {
                        if(!response.getBody().get("quoteResponse").get("result").get(j).get("longName").isNull()) {
                            ans.get(j).setCompanyName(response.getBody().get("quoteResponse").get("result").get(j).get("longName").asText());
                        }
                        if(!response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPrice").isNull()) {
                            ans.get(j).setHighPriceRange(response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPrice").asText());
                        }
                    }
                }
            }
        }
        RootResponseDto<List<TopStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }
}
