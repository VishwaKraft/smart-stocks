package com.smartstocks.product.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstocks.product.dto.*;
import com.smartstocks.product.service.INewsService;
import com.smartstocks.product.service.ISerachTableService;
import com.smartstocks.product.util.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * StockController — powered by Indian Stock API (https://stock.indianapi.in).
 *
 * <p>Existing frontend endpoint paths are preserved so the Angular client continues
 * to work without any route changes. Upstream calls now target the Indian Stock API
 * instead of the old Yahoo Finance / RapidAPI endpoints.</p>
 */
@RestController
@RequestMapping("/stock")
@CrossOrigin(origins = "*")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("indianApiHeaders")
    private HttpEntity indianApiHeaders;

    @Value("${indian.api.base-url}")
    private String indianApiBaseUrl;

    @Autowired
    private INewsService newsService;

    @Autowired
    private ISerachTableService searchTableService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Helper: build URL for Indian Stock API ──────────────────────────

    private String indianUrl(String path) {
        return indianApiBaseUrl + path;
    }

    /** Safely read a text field from a JsonNode, returning fallback on null/missing. */
    private String txt(JsonNode node, String field, String fallback) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return fallback;
        return node.get(field).asText(fallback);
    }

    private String txt(JsonNode node, String field) {
        return txt(node, field, "");
    }

    /** Wrap a raw string value into the {fmt: "..."} shape that the frontend expects. */
    private DetailValuesDto fmt(String value) {
        return new DetailValuesDto(value != null ? value : "");
    }

    // ─── GET /stock/details ──────────────────────────────────────────────
    //  Maps Indian API  GET /stock?name={stockName}  →  CompanyProfileDto

    @GetMapping("/details")
    public ResponseEntity getCompanyProfile(@RequestParam("symbol") String stockName) throws IOException {
        try {
            String url = indianUrl("/stock?name=" + encodeParam(stockName));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            CompanyProfileDto profile = new CompanyProfileDto();

            // Basic info
            profile.setShortName(txt(body, "companyName"));
            profile.setLongName(txt(body, "companyName"));
            profile.setIndustry(txt(body, "industry"));

            // Current price — Indian API returns { "BSE": "186.50", "NSE": "186.53" }
            // We prefer NSE price, fall back to BSE
            JsonNode priceNode = body.get("currentPrice");
            String currentPriceStr = "0";
            if (priceNode != null) {
                if (priceNode.has("NSE") && !priceNode.get("NSE").isNull()) {
                    currentPriceStr = priceNode.get("NSE").asText("0");
                    profile.setExchange("NSE");
                } else if (priceNode.has("BSE") && !priceNode.get("BSE").isNull()) {
                    currentPriceStr = priceNode.get("BSE").asText("0");
                    profile.setExchange("BSE");
                }
            }
            profile.setCurrentPrice(fmt(currentPriceStr));

            // Percent change & compute change value
            String percentChangeStr = txt(body, "percentChange", "0");
            profile.setChangePercentage(fmt(percentChangeStr + "%"));

            // Reusable data block has close (previous close), high, low, marketCap, etc.
            JsonNode reusable = body.get("stockDetailsReusableData");
            String previousCloseStr = "0";
            if (reusable != null) {
                previousCloseStr = txt(reusable, "close", "0");
                profile.setPreviousClose(fmt(previousCloseStr));
                profile.setOpen(fmt(txt(reusable, "price", "")));
                profile.setDayHigh(fmt(txt(reusable, "high", "")));
                profile.setDayLow(fmt(txt(reusable, "low", "")));
                profile.setVolume(fmt(""));  // Volume not directly in reusable data
                profile.setMarketCap(fmt(txt(reusable, "marketCap", "")));
                profile.setFiftyTwoWeekHigh(fmt(txt(reusable, "yhigh", txt(body, "yearHigh", ""))));
                profile.setFiftyTwoWeekLow(fmt(txt(reusable, "ylow", txt(body, "yearLow", ""))));

                // PE ratio
                profile.setForwardPE(fmt(txt(reusable, "pPerEBasicExcludingExtraordinaryItemsTTM", "")));

                // Dividend yield
                profile.setTrailingAnnualDividendYield(fmt(txt(reusable, "currentDividendYieldCommonStockPrimaryIssueLTM", "")));
            } else {
                profile.setPreviousClose(fmt("0"));
                profile.setFiftyTwoWeekHigh(fmt(txt(body, "yearHigh", "")));
                profile.setFiftyTwoWeekLow(fmt(txt(body, "yearLow", "")));
            }

            // Calculate change from current price and previous close
            try {
                double cp = Double.parseDouble(currentPriceStr);
                double pc = Double.parseDouble(previousCloseStr);
                double change = cp - pc;
                profile.setChange(fmt(String.format("%.2f", change)));
            } catch (NumberFormatException e) {
                profile.setChange(fmt("0.00"));
            }

            // Company description from companyProfile
            JsonNode companyProfile = body.get("companyProfile");
            if (companyProfile != null && companyProfile.has("companyDescription")) {
                profile.setLongBusinessSummary(txt(companyProfile, "companyDescription"));
            }

            // Symbol from companyProfile
            if (companyProfile != null) {
                profile.setSymbol(txt(companyProfile, "exchangeCodeNse",
                        txt(companyProfile, "exchangeCodeBse", stockName)));
            } else {
                profile.setSymbol(stockName);
            }

            // Sector — Indian API doesn't have a separate sector, use industry
            profile.setSector(txt(body, "industry"));

            RootResponseDto<CompanyProfileDto> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, profile);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error fetching stock details for '{}': {}", stockName, e.getMessage(), e);
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Failed to fetch stock details: " + e.getMessage());
            RootResponseDto<String> errorResponse = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
            return new ResponseEntity<>(errorResponse, new HttpHeaders(), 500);
        }
    }

    // ─── GET /stock/graph/{symbol} ───────────────────────────────────────
    //  Maps Indian API  GET /historical_data  →  List<ChartInstanceDto>

    @GetMapping("/graph/{symbol}")
    public ResponseEntity getGraphData(
            @PathVariable("symbol") String stockName,
            @RequestParam(value = "range", defaultValue = "1yr") String range,
            @RequestParam(value = "interval", defaultValue = "1d") String interval) throws IOException {
        try {
            // Map frontend range param to Indian API period enum
            String period = mapRangeToPeriod(range);
            String url = indianUrl("/historical_data?stock_name=" + encodeParam(stockName)
                    + "&period=" + period + "&filter=default");

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            List<ChartInstanceDto> ans = new LinkedList<>();
            if (body != null && body.isArray()) {
                for (JsonNode item : body) {
                    ChartInstanceDto dto = new ChartInstanceDto();
                    // Historical data items have date as string, price fields
                    // Try to parse date to epoch
                    String dateStr = txt(item, "date", "");
                    dto.setDate(parseDateToEpoch(dateStr));
                    dto.setOpen(item.has("open") ? item.get("open").asDouble(0) : 0);
                    dto.setHigh(item.has("high") ? item.get("high").asDouble(0) : 0);
                    dto.setLow(item.has("low") ? item.get("low").asDouble(0) : 0);
                    dto.setClose(item.has("close") ? item.get("close").asDouble(0) : 0);
                    dto.setVolume(item.has("volume") ? item.get("volume").asDouble(0) : 0);
                    ans.add(dto);
                }
            }

            RootResponseDto<List<ChartInstanceDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error fetching graph data for '{}': {}", stockName, e.getMessage(), e);
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Failed to fetch historical data: " + e.getMessage());
            RootResponseDto<String> errorResponse = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
            return new ResponseEntity<>(errorResponse, new HttpHeaders(), 500);
        }
    }

    // ─── GET /stock/search ───────────────────────────────────────────────
    //  Maps Indian API  GET /industry_search?query={q}  →  List<SearchStocksDto>

    @GetMapping("/search")
    public ResponseEntity mySearch(@RequestParam("q") String query, @RequestParam("limit") int limit) {
        try {
            String url = indianUrl("/industry_search?query=" + encodeParam(query));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            List<SearchStocksDto> ans = new LinkedList<>();
            if (body != null && body.isArray()) {
                int count = 0;
                for (JsonNode item : body) {
                    if (count >= limit) break;
                    SearchStocksDto dto = new SearchStocksDto();
                    dto.setSymbol(txt(item, "exchangeCodeNsi", txt(item, "exchangeCodeBse", txt(item, "id", ""))));
                    dto.setExchange(item.has("exchangeCodeNsi") && !item.get("exchangeCodeNsi").isNull() ? "NSE" : "BSE");
                    String name = txt(item, "commonName", "");
                    dto.setShortName(name);
                    dto.setLongName(name);
                    ans.add(dto);
                    count++;
                }
            }

            RootResponseDto<List<SearchStocksDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error searching stocks for '{}': {}", query, e.getMessage(), e);
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Search failed: " + e.getMessage());
            RootResponseDto<String> errorResponse = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
            return new ResponseEntity<>(errorResponse, new HttpHeaders(), 500);
        }
    }

    // ─── GET /stock/all ──────────────────────────────────────────────────
    //  Maps Indian API  GET /trending  →  List<RecommendedStocks>

    @GetMapping("/all")
    public ResponseEntity allStocks(
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @RequestParam(value = "size", defaultValue = "10") int size) throws IOException {
        try {
            String url = indianUrl("/trending");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            List<RecommendedStocks> ans = new LinkedList<>();
            if (body != null && body.has("trending_stocks")) {
                JsonNode trending = body.get("trending_stocks");
                // Combine top_gainers and top_losers
                List<JsonNode> allStockNodes = new ArrayList<>();
                if (trending.has("top_gainers") && trending.get("top_gainers").isArray()) {
                    for (JsonNode n : trending.get("top_gainers")) allStockNodes.add(n);
                }
                if (trending.has("top_losers") && trending.get("top_losers").isArray()) {
                    for (JsonNode n : trending.get("top_losers")) allStockNodes.add(n);
                }

                // Paginate
                int start = pageNo * size;
                int end = Math.min(start + size, allStockNodes.size());
                for (int i = start; i < end; i++) {
                    ans.add(mapTrendingToRecommended(allStockNodes.get(i)));
                }
            }

            RootResponseDto<List<RecommendedStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error fetching all stocks: {}", e.getMessage(), e);
            return errorResponse("Failed to fetch stocks: " + e.getMessage());
        }
    }

    // ─── GET /stock/top ──────────────────────────────────────────────────
    //  Maps Indian API  GET /trending  →  List<TopStocks>  (top gainers/losers)

    @GetMapping("/top")
    public ResponseEntity getTopGainersAndLosers(
            @RequestParam(value = "days", defaultValue = "1") int days,
            @RequestParam("type") TopGainerOrTopLoser type) {
        try {
            String url = indianUrl("/trending");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            List<TopStocks> ans = new LinkedList<>();
            if (body != null && body.has("trending_stocks")) {
                JsonNode trending = body.get("trending_stocks");
                String key = (type == TopGainerOrTopLoser.TOP_LOSER) ? "top_losers" : "top_gainers";
                if (trending.has(key) && trending.get(key).isArray()) {
                    for (JsonNode item : trending.get(key)) {
                        TopStocks stock = new TopStocks();
                        stock.setSymbol(txt(item, "ric", txt(item, "ticker_id", "")));
                        stock.setCompanyName(txt(item, "company_name", ""));
                        stock.setHighPriceRange(txt(item, "price", "0"));
                        try {
                            stock.setOverallChangePerc(Double.parseDouble(txt(item, "percent_change", "0")));
                            stock.setOveralLChange(Double.parseDouble(txt(item, "net_change", "0")));
                        } catch (NumberFormatException e) {
                            stock.setOverallChangePerc(0.0);
                            stock.setOveralLChange(0.0);
                        }
                        ans.add(stock);
                    }
                }
            }

            RootResponseDto<List<TopStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error fetching top stocks: {}", e.getMessage(), e);
            return errorResponse("Failed to fetch top stocks: " + e.getMessage());
        }
    }

    // ─── GET /stock/recommended-stocks/{symbol} ──────────────────────────
    //  Uses Indian API  GET /stock?name={symbol}  → peer company list

    @GetMapping("/recommended-stocks/{symbol}")
    public ResponseEntity getRecommendedStocks(@PathVariable("symbol") String stockName) throws IOException {
        try {
            return fetchPeerStocks(stockName);
        } catch (Exception e) {
            log.error("Error fetching recommended stocks for '{}': {}", stockName, e.getMessage(), e);
            return errorResponse("Failed to fetch recommended stocks: " + e.getMessage());
        }
    }

    // ─── GET /stock/peers-stocks/{symbol} ────────────────────────────────
    //  Uses Indian API  GET /stock?name={symbol}  → peer company list

    @GetMapping("/peers-stocks/{symbol}")
    public ResponseEntity peersStocks(@PathVariable("symbol") String stockName) throws IOException {
        try {
            return fetchPeerStocks(stockName);
        } catch (Exception e) {
            log.error("Error fetching peer stocks for '{}': {}", stockName, e.getMessage(), e);
            return errorResponse("Failed to fetch peer stocks: " + e.getMessage());
        }
    }

    // ─── GET /stock/trends/{symbol} ──────────────────────────────────────
    //  Uses Indian API  GET /stock?name={symbol}  → analyst ratings from stockDetailsReusableData.stockAnalyst

    @GetMapping("/trends/{symbol}")
    public ResponseEntity trendsStocks(@PathVariable("symbol") String stockName) throws IOException {
        try {
            String url = indianUrl("/stock?name=" + encodeParam(stockName));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            JsonNode body = response.getBody();

            List<TrendDto> ans = new LinkedList<>();

            // Map stockAnalyst data from Indian API to TrendDto
            JsonNode reusable = body != null ? body.get("stockDetailsReusableData") : null;
            if (reusable != null && reusable.has("stockAnalyst") && reusable.get("stockAnalyst").isArray()) {
                // Create a single TrendDto for the current period
                TrendDto trend = new TrendDto();
                trend.setPeriod("0m");  // current period

                for (JsonNode analyst : reusable.get("stockAnalyst")) {
                    String ratingName = txt(analyst, "ratingName", "").toLowerCase();
                    int count = 0;
                    try {
                        count = Integer.parseInt(txt(analyst, "numberOfAnalystsLatest", "0"));
                    } catch (NumberFormatException ignored) {}

                    switch (ratingName) {
                        case "strong buy": trend.setStrongBuy(count); break;
                        case "buy": trend.setBuy(count); break;
                        case "hold": trend.setHold(count); break;
                        case "sell": trend.setSell(count); break;
                        case "strong sell": trend.setStrongSell(count); break;
                    }
                }
                ans.add(trend);

                // Also create historical periods from analyst data if available
                String[] periodLabels = {"-1w", "-1m", "-2m", "-3m"};
                String[] periodFields = {"numberOfAnalysts1WeekAgo", "numberOfAnalysts1MonthAgo",
                        "numberOfAnalysts2MonthAgo", "numberOfAnalysts3MonthAgo"};

                for (int p = 0; p < periodLabels.length; p++) {
                    TrendDto historicalTrend = new TrendDto();
                    historicalTrend.setPeriod(periodLabels[p]);
                    for (JsonNode analyst : reusable.get("stockAnalyst")) {
                        String ratingName = txt(analyst, "ratingName", "").toLowerCase();
                        int count = 0;
                        try {
                            count = Integer.parseInt(txt(analyst, periodFields[p], "0"));
                        } catch (NumberFormatException ignored) {}

                        switch (ratingName) {
                            case "strong buy": historicalTrend.setStrongBuy(count); break;
                            case "buy": historicalTrend.setBuy(count); break;
                            case "hold": historicalTrend.setHold(count); break;
                            case "sell": historicalTrend.setSell(count); break;
                            case "strong sell": historicalTrend.setStrongSell(count); break;
                        }
                    }
                    ans.add(historicalTrend);
                }
            }

            RootResponseDto<List<TrendDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);

        } catch (Exception e) {
            log.error("Error fetching trends for '{}': {}", stockName, e.getMessage(), e);
            return errorResponse("Failed to fetch trends: " + e.getMessage());
        }
    }

    // ─── GET /stock/news-types ───────────────────────────────────────────

    @GetMapping("/news-types")
    public ResponseEntity getNewsTypes() {
        List<NewsTypeDto> newsTypes = new LinkedList<>();
        newsTypes.add(new NewsTypeDto("business", "Business", "Business and financial news"));
        newsTypes.add(new NewsTypeDto("technology", "Technology", "Technology, science and innovation news"));
        newsTypes.add(new NewsTypeDto("general", "General", "General news"));
        newsTypes.add(new NewsTypeDto("health", "Health", "Health and medical news"));
        newsTypes.add(new NewsTypeDto("sports", "Sports", "Sports news"));
        newsTypes.add(new NewsTypeDto("entertainment", "Entertainment", "Entertainment news"));

        RootResponseDto<List<NewsTypeDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, newsTypes);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    // ─── GET /stock/news ─────────────────────────────────────────────────

    @GetMapping("/news")
    public ResponseEntity getNews(
            @RequestParam(value = "type", required = false, defaultValue = "business") String type,
            @RequestParam(value = "limit", required = false) Integer limit) throws IOException {
        try {
            String normalizedType = type.toLowerCase();
            if ("science".equals(normalizedType)) {
                normalizedType = "technology";
            }
            List<String> validTypes = Arrays.asList("business", "technology", "general", "health", "sports", "entertainment");
            if (!validTypes.contains(normalizedType)) {
                Map<String, String> errors = new HashMap<>();
                errors.put("error", "Invalid news type: " + type + ". Valid types are: " + String.join(", ", validTypes));
                RootResponseDto<String> errorResponse = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                        ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
                return new ResponseEntity<>(errorResponse, new HttpHeaders(), 400);
            }

            List<NewsDto> ans = newsService.fetchAndStoreNews(normalizedType);
            ans = ans.stream()
                .filter(news -> news.getTitle() != null && !news.getTitle().isBlank() &&
                                news.getUrl() != null && !news.getUrl().isBlank())
                .collect(java.util.stream.Collectors.toList());

            if (limit != null && limit > 0 && limit < ans.size()) {
                ans = ans.subList(0, limit);
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

    // ─── NEW ENDPOINTS: Indian Stock API features ────────────────────────

    /** GET /stock/ipo — IPO data */
    @GetMapping("/ipo")
    public ResponseEntity getIpoData() {
        try {
            String url = indianUrl("/ipo");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch IPO data: " + e.getMessage());
        }
    }

    /** GET /stock/commodities — Commodity prices */
    @GetMapping("/commodities")
    public ResponseEntity getCommodities() {
        try {
            String url = indianUrl("/commodities");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch commodities: " + e.getMessage());
        }
    }

    /** GET /stock/mutual-funds — Mutual fund data grouped by category */
    @GetMapping("/mutual-funds")
    public ResponseEntity getMutualFunds() {
        try {
            String url = indianUrl("/mutual_funds");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch mutual funds: " + e.getMessage());
        }
    }

    /** GET /stock/mutual-fund-search — Search mutual funds */
    @GetMapping("/mutual-fund-search")
    public ResponseEntity searchMutualFunds(@RequestParam("query") String query) {
        try {
            String url = indianUrl("/mutual_fund_search?query=" + encodeParam(query));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to search mutual funds: " + e.getMessage());
        }
    }

    /** GET /stock/mutual-fund-details — Mutual fund details */
    @GetMapping("/mutual-fund-details")
    public ResponseEntity getMutualFundDetails(@RequestParam("name") String stockName) {
        try {
            String url = indianUrl("/mutual_funds_details?stock_name=" + encodeParam(stockName));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch mutual fund details: " + e.getMessage());
        }
    }

    /** GET /stock/price-shockers — BSE/NSE price shockers */
    @GetMapping("/price-shockers")
    public ResponseEntity getPriceShockers() {
        try {
            String url = indianUrl("/price_shockers");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch price shockers: " + e.getMessage());
        }
    }

    /** GET /stock/52-week-high-low — 52 week high/low data */
    @GetMapping("/52-week-high-low")
    public ResponseEntity get52WeekHighLow() {
        try {
            String url = indianUrl("/fetch_52_week_high_low_data");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch 52-week data: " + e.getMessage());
        }
    }

    /** GET /stock/bse-most-active — BSE most active stocks */
    @GetMapping("/bse-most-active")
    public ResponseEntity getBseMostActive() {
        try {
            String url = indianUrl("/BSE_most_active");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch BSE most active: " + e.getMessage());
        }
    }

    /** GET /stock/nse-most-active — NSE most active stocks */
    @GetMapping("/nse-most-active")
    public ResponseEntity getNseMostActive() {
        try {
            String url = indianUrl("/NSE_most_active");
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch NSE most active: " + e.getMessage());
        }
    }

    /** GET /stock/corporate-actions — Corporate actions for a stock */
    @GetMapping("/corporate-actions")
    public ResponseEntity getCorporateActions(@RequestParam("name") String stockName) {
        try {
            String url = indianUrl("/corporate_actions?stock_name=" + encodeParam(stockName));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch corporate actions: " + e.getMessage());
        }
    }

    /** GET /stock/recent-announcements — Recent announcements for a stock */
    @GetMapping("/recent-announcements")
    public ResponseEntity getRecentAnnouncements(@RequestParam("name") String stockName) {
        try {
            String url = indianUrl("/recent_announcements?stock_name=" + encodeParam(stockName));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch announcements: " + e.getMessage());
        }
    }

    /** GET /stock/statement — Financial statements for a stock */
    @GetMapping("/statement")
    public ResponseEntity getStatement(
            @RequestParam("name") String stockName,
            @RequestParam("stats") String stats) {
        try {
            String url = indianUrl("/statement?stock_name=" + encodeParam(stockName)
                    + "&stats=" + encodeParam(stats));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch statement: " + e.getMessage());
        }
    }

    /** GET /stock/historical-stats — Historical stats for a stock */
    @GetMapping("/historical-stats")
    public ResponseEntity getHistoricalStats(
            @RequestParam("name") String stockName,
            @RequestParam("stats") String stats) {
        try {
            String url = indianUrl("/historical_stats?stock_name=" + encodeParam(stockName)
                    + "&stats=" + encodeParam(stats));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch historical stats: " + e.getMessage());
        }
    }

    /** GET /stock/stock-forecasts — Stock forecasts */
    @GetMapping("/stock-forecasts")
    public ResponseEntity getStockForecasts(
            @RequestParam("stock_id") String stockId,
            @RequestParam("measure_code") String measureCode,
            @RequestParam("period_type") String periodType,
            @RequestParam("data_type") String dataType,
            @RequestParam("age") String age) {
        try {
            String url = indianUrl("/stock_forecasts?stock_id=" + encodeParam(stockId)
                    + "&measure_code=" + encodeParam(measureCode)
                    + "&period_type=" + encodeParam(periodType)
                    + "&data_type=" + encodeParam(dataType)
                    + "&age=" + encodeParam(age));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch forecasts: " + e.getMessage());
        }
    }

    /** GET /stock/stock-target-price — Target price for a stock */
    @GetMapping("/stock-target-price")
    public ResponseEntity getStockTargetPrice(@RequestParam("stock_id") String stockId) {
        try {
            String url = indianUrl("/stock_target_price?stock_id=" + encodeParam(stockId));
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
            RootResponseDto<JsonNode> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, response.getBody());
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        } catch (Exception e) {
            return errorResponse("Failed to fetch target price: " + e.getMessage());
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    /** Shared logic for recommended-stocks and peers-stocks endpoints */
    private ResponseEntity fetchPeerStocks(String stockName) {
        String url = indianUrl("/stock?name=" + encodeParam(stockName));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, indianApiHeaders, JsonNode.class);
        JsonNode body = response.getBody();

        List<RecommendedStocks> ans = new LinkedList<>();

        // Peer companies are in companyProfile.peerCompanyList
        JsonNode companyProfile = body != null ? body.get("companyProfile") : null;
        if (companyProfile != null && companyProfile.has("peerCompanyList") && companyProfile.get("peerCompanyList").isArray()) {
            for (JsonNode peer : companyProfile.get("peerCompanyList")) {
                RecommendedStocks rec = new RecommendedStocks();
                rec.setSymbol(txt(peer, "companyName", txt(peer, "tickerId", "")));
                rec.setLongName(txt(peer, "companyName", ""));
                rec.setShortName(txt(peer, "companyName", ""));

                String priceStr = String.valueOf(peer.has("price") ? peer.get("price").asDouble(0) : 0);
                rec.setCurrentPrice(fmt(priceStr));

                // Calculate previous close from price and percentChange
                double price = peer.has("price") ? peer.get("price").asDouble(0) : 0;
                double pctChange = peer.has("percentChange") ? peer.get("percentChange").asDouble(0) : 0;
                double prevClose = (pctChange != 0) ? price / (1 + pctChange / 100.0) : price;
                rec.setPreviousClose(fmt(String.format("%.2f", prevClose)));

                double netChange = peer.has("netChange") ? peer.get("netChange").asDouble(0) : 0;
                rec.setChange(fmt(String.format("%.2f", netChange)));
                rec.setChangePercentage(fmt(String.format("%.2f%%", pctChange)));

                ans.add(rec);
            }
        }

        // Also check stockDetailsReusableData.peerCompanyList as a fallback
        if (ans.isEmpty()) {
            JsonNode reusable = body != null ? body.get("stockDetailsReusableData") : null;
            if (reusable != null && reusable.has("peerCompanyList") && reusable.get("peerCompanyList").isArray()) {
                for (JsonNode peer : reusable.get("peerCompanyList")) {
                    RecommendedStocks rec = new RecommendedStocks();
                    rec.setSymbol(txt(peer, "companyName", txt(peer, "tickerId", "")));
                    rec.setLongName(txt(peer, "companyName", ""));
                    rec.setShortName(txt(peer, "companyName", ""));

                    String priceStr = String.valueOf(peer.has("price") ? peer.get("price").asDouble(0) : 0);
                    rec.setCurrentPrice(fmt(priceStr));

                    double price = peer.has("price") ? peer.get("price").asDouble(0) : 0;
                    double pctChange = peer.has("percentChange") ? peer.get("percentChange").asDouble(0) : 0;
                    double prevClose = (pctChange != 0) ? price / (1 + pctChange / 100.0) : price;
                    rec.setPreviousClose(fmt(String.format("%.2f", prevClose)));

                    double netChange = peer.has("netChange") ? peer.get("netChange").asDouble(0) : 0;
                    rec.setChange(fmt(String.format("%.2f", netChange)));
                    rec.setChangePercentage(fmt(String.format("%.2f%%", pctChange)));

                    ans.add(rec);
                }
            }
        }

        RootResponseDto<List<RecommendedStocks>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, ans);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    /** Map a trending stock JSON node to the RecommendedStocks DTO */
    private RecommendedStocks mapTrendingToRecommended(JsonNode item) {
        RecommendedStocks rec = new RecommendedStocks();
        rec.setSymbol(txt(item, "ric", txt(item, "ticker_id", "")));
        rec.setLongName(txt(item, "company_name", ""));
        rec.setShortName(txt(item, "company_name", ""));
        rec.setCurrentPrice(fmt(txt(item, "price", "0")));
        rec.setPreviousClose(fmt(txt(item, "close", "0")));

        try {
            double price = Double.parseDouble(txt(item, "price", "0"));
            double close = Double.parseDouble(txt(item, "close", "0"));
            double change = price - close;
            double changePct = close != 0 ? (change / close * 100) : 0;
            rec.setChange(fmt(String.format("%.2f", change)));
            rec.setChangePercentage(fmt(String.format("%.2f%%", changePct)));
        } catch (NumberFormatException e) {
            rec.setChange(fmt("0.00"));
            rec.setChangePercentage(fmt("0.00%"));
        }

        return rec;
    }

    /** Map frontend range param to Indian API PeriodEnum */
    private String mapRangeToPeriod(String range) {
        if (range == null) return "1yr";
        switch (range.toLowerCase()) {
            case "1d": case "5d": case "1mo": case "1m": return "1m";
            case "3mo": case "6mo": case "6m": return "6m";
            case "1y": case "1yr": return "1yr";
            case "3y": case "3yr": return "3yr";
            case "5y": case "5yr": return "5yr";
            case "10y": case "10yr": return "10yr";
            case "max": return "max";
            default: return "1yr";
        }
    }

    /** Parse a date string (e.g. "22 Jul 2026" or ISO format) to Unix epoch seconds */
    private long parseDateToEpoch(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return 0;
        try {
            // Try ISO date first (yyyy-MM-dd)
            java.time.LocalDate ld = java.time.LocalDate.parse(dateStr);
            return ld.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toEpochSecond();
        } catch (Exception e1) {
            try {
                // Try "dd MMM yyyy" format
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH);
                java.time.LocalDate ld = java.time.LocalDate.parse(dateStr, formatter);
                return ld.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toEpochSecond();
            } catch (Exception e2) {
                try {
                    // Try "d MMM yyyy" format
                    java.time.format.DateTimeFormatter formatter =
                            java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH);
                    java.time.LocalDate ld = java.time.LocalDate.parse(dateStr, formatter);
                    return ld.atStartOfDay(java.time.ZoneId.of("Asia/Kolkata")).toEpochSecond();
                } catch (Exception e3) {
                    log.warn("Unable to parse date '{}': {}", dateStr, e3.getMessage());
                    return 0;
                }
            }
        }
    }

    /** URL-encode a parameter value */
    private String encodeParam(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    /** Build a standard error response */
    private ResponseEntity errorResponse(String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", message);
        RootResponseDto<String> errorResponse = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(errorResponse, new HttpHeaders(), 500);
    }
}
