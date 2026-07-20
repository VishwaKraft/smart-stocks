package com.smartstocks.product.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartstocks.product.config.JwtUtils;
import com.smartstocks.product.dto.*;
import com.smartstocks.product.models.MockStocks;
import com.smartstocks.product.models.StockTransactions;
import com.smartstocks.product.models.StocksBought;
import com.smartstocks.product.models.User;
import com.smartstocks.product.service.IUserService;
import com.smartstocks.product.util.ResponseMessage;
import com.smartstocks.product.util.UtilityMethods;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
@Validated
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${upstream.details.url}")
    private String detailsUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    @Autowired
    private HttpEntity headers;

    @PostMapping("/signup")
    public ResponseEntity<RootResponseDto<String>> userSignUp(@Valid @RequestBody UserProfileRequestDto user) {
        if (userService.isEmailTaken(user.getEmail())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Email is already taken");
            RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
            return new ResponseEntity<>(response, new HttpHeaders(), 400);
        }
        User realUser = modelMapper.map(user, User.class);
        realUser.setPassword(encoder.encode(realUser.getPassword()));
        userService.saveUser(realUser);
        RootResponseDto<String> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, null);
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @PutMapping("/profile")
    public ResponseEntity<RootResponseDto<String>> updateProfile(@Valid @RequestBody SignedInUserDto userProfile, Principal user) {
        User toBeUpdate = modelMapper.map(userProfile, User.class);
        toBeUpdate.setEmail(user.getName());
        this.userService.updateUser(toBeUpdate);
        RootResponseDto<String> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, null);
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @GetMapping("/profile")
    public ResponseEntity<RootResponseDto<UserProfileResponseDto>> readProfile(Principal emailWrapper) {
        User user = this.userService.getUserByEmail(emailWrapper.getName());
        UserProfileResponseDto userWrapper = modelMapper.map(user, UserProfileResponseDto.class);
        RootResponseDto<UserProfileResponseDto> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, userWrapper);
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @PostMapping("/token")
    public ResponseEntity<RootResponseDto<TokenResponse>> getToken(@Valid @RequestBody UserLoginBody user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String token  = jwtUtils.generateToken(userDetails);
        RootResponseDto<TokenResponse> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, new TokenResponse("Bearer " + token));
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @PostMapping("/google-login")
    public ResponseEntity<RootResponseDto<TokenResponse>> googleLogin(@Valid @RequestBody GoogleLoginDto googleLoginDto) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleLoginDto.getToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                
                if (!userService.isEmailTaken(email)) {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName((String) payload.get("given_name"));
                    newUser.setLastName((String) payload.get("family_name"));
                    // Set a random strong password for google users
                    newUser.setPassword(encoder.encode(UUID.randomUUID().toString()));
                    newUser.setWallet(0.0);
                    newUser.setProfit(0.0);
                    userService.saveUser(newUser);
                }

                UserDetails userDetails = userService.loadUserByUsername(email);
                String token  = jwtUtils.generateToken(userDetails);
                
                RootResponseDto<TokenResponse> response = new RootResponseDto<>(200, HttpStatus.OK,
                        ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, new TokenResponse("Bearer " + token));
                return new ResponseEntity(response, new HttpHeaders(), 200);
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("error", "Invalid Google token");
                RootResponseDto<TokenResponse> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                        ResponseMessage.FAILED.toString(), LocalDateTime.now(), null, null);
                return new ResponseEntity(response, new HttpHeaders(), 400);
            }
        } catch (Exception e) {
            e.printStackTrace();
            RootResponseDto<TokenResponse> response = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseMessage.FAILED.toString(), LocalDateTime.now(), null, null);
            return new ResponseEntity(response, new HttpHeaders(), 500);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<RootResponseDto<List<NonCreditTransactionDto>>> getNonCreditTransactions(Principal user) {
        Set<StockTransactions> transactions = userService.getNonCreditTransactions(user.getName());
        List<NonCreditTransactionDto> transactionWrapper = modelMapper.map(transactions, new TypeToken<List<NonCreditTransactionDto>>() {}.getType());
        UtilityMethods.convertIntoMockSymbolsForNonCreditTransactionDto(transactionWrapper);
        RootResponseDto<List<NonCreditTransactionDto>> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, transactionWrapper);
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @GetMapping("/credit-transactions")
    public ResponseEntity<RootResponseDto<List<CreditTransactionDto>>> getCreditTransactions(Principal user) {
        Set<StockTransactions> transactions = userService.getCreditTransactions(user.getName());
        List<CreditTransactionDto> transactionWrapper = modelMapper.map(transactions, new TypeToken<List<CreditTransactionDto>>() {}.getType());
        RootResponseDto<List<CreditTransactionDto>> response = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, transactionWrapper);
        return new ResponseEntity(response, new HttpHeaders(), 200);
    }

    @GetMapping("/stocks")
    public ResponseEntity<RootResponseDto<List<StocksBoughtDto>>> getUserStocks(Principal user) throws IOException {
        Set<StocksBought> boughtStocks = userService.getUserByEmail(user.getName()).getBought();
        List<StocksBoughtDto> boughtStocksWrapper = modelMapper.map(boughtStocks, new TypeToken<List<StocksBoughtDto>>() {}.getType());
        String symbols = UtilityMethods.convertIntoMockSymbolsForStocksBought(boughtStocksWrapper);
        if(!symbols.isEmpty()) {
            ResponseEntity<JsonNode> response = this.restTemplate.exchange( detailsUrl + "?region=IN&lang=en&symbols=" + symbols,
                    HttpMethod.GET, headers, JsonNode.class);
            if(response.getStatusCode() == HttpStatus.OK) {
                for (int i=0; i<boughtStocksWrapper.size(); i++) {
                    StocksBoughtDto ele = boughtStocksWrapper.get(i);
                    if(!response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").isNull()) {
                        ele.setPrice(response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPrice").asDouble());
                    }
                    if(!response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketDayHigh").isNull()) {
                        ele.setHigh(response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketDayHigh").asDouble());
                    }
                    if(!response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketDayLow").isNull()) {
                        ele.setLow(response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketDayLow").asDouble());
                    }
                    if(!response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").isNull()) {
                        ele.setPreviousClose(response.getBody().get("quoteResponse").get("result").get(i).get("regularMarketPreviousClose").asDouble());
                    }
                }
            }
        }
        RootResponseDto<List<StocksBoughtDto>> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, boughtStocksWrapper);
        return new ResponseEntity(myResponse, new HttpHeaders(), 200);
    }

    @PutMapping("/buy")
    public ResponseEntity<RootResponseDto<Double>> buyStock(Principal user, @RequestBody List<@Valid StocksToBuy> stocksToBuy) {
        stocksToBuy = UtilityMethods.convertIntoRealSymbol(stocksToBuy);
        String symbols = "";
        int i;
        for(i=0; i<stocksToBuy.size()-1; i++) symbols += (stocksToBuy.get(i).getSymbol() + ",");
        symbols += stocksToBuy.get(i).getSymbol();
        List<Double> prices = new LinkedList<>();
        ResponseEntity<JsonNode> response = this.restTemplate.exchange( detailsUrl + "?region=IN&lang=en&symbols=" + symbols,
                HttpMethod.GET, headers, JsonNode.class);
        if(response.getStatusCode() == HttpStatus.OK) {
            for(int j=0; j<response.getBody().get("quoteResponse").get("result").size(); j++) {
                prices.add(response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPrice").asDouble());
            }
        }
        double remainingCredit = this.userService.buyStock(user.getName(),
                modelMapper.map(stocksToBuy, new TypeToken<List<StocksBought>>() {}.getType()),
                prices);
        RootResponseDto<Double> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, remainingCredit);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @PutMapping("/sell")
    public ResponseEntity<RootResponseDto<Double>> sellStock(Principal user, @RequestBody List<@Valid StocksToBuy> stocksToSell) {
        stocksToSell = UtilityMethods.convertIntoRealSymbol(stocksToSell);
        String symbols = "";
        int i;
        for(i=0; i<stocksToSell.size()-1; i++) symbols += (stocksToSell.get(i).getSymbol() + ",");
        symbols += stocksToSell.get(i).getSymbol();
        List<Double> prices = new LinkedList<>();
        ResponseEntity<JsonNode> response = this.restTemplate.exchange( detailsUrl + "?region=IN&lang=en&symbols=" + symbols,
                HttpMethod.GET, headers, JsonNode.class);
        if(response.getStatusCode() == HttpStatus.OK) {
            for(int j=0; j<response.getBody().get("quoteResponse").get("result").size(); j++) {
                prices.add(response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPrice").asDouble());
            }
        }
        double remainingCredit = this.userService.sellStock(user.getName(),
                stocksToSell,
                prices);
        RootResponseDto<Double> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, remainingCredit);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/money")
    public ResponseEntity<RootResponseDto<ProfitAndMoney>> getMoney(Principal user) {
        User userProfile = userService.getUserByEmail(user.getName());
        RootResponseDto<ProfitAndMoney> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, new ProfitAndMoney(userProfile.getWallet(), userProfile.getProfit()));
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @PutMapping("/money")
    public ResponseEntity<Object> addMoney(Principal user, @Valid @RequestBody MoneyDto money) {
        userService.addMoney(user.getName(), money.getValue());
        RootResponseDto<Object> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, null);
        return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
    }

    @GetMapping("/stock/all")
    public ResponseEntity getMockStocks() {
        String symbols = "";
        for(Map.Entry entry : MockStocks.mockStocksMap.entrySet()) {
            symbols += (entry.getKey() + ",");
        }
        ResponseEntity<JsonNode> response = this.restTemplate.exchange( detailsUrl + "?region=IN&lang=en&symbols=" + symbols.substring(0, symbols.length()-1),
                HttpMethod.GET, headers, JsonNode.class);
        if(response.getStatusCode() == HttpStatus.OK) {
            MockStockDto[] arr = new MockStockDto[5];
            for(int j=0; j<response.getBody().get("quoteResponse").get("result").size(); j++) {
                String symbol = response.getBody().get("quoteResponse").get("result").get(j).get("symbol").asText();
                arr[j] = new MockStockDto(MockStocks.mockStocksMap.get(symbol),
                        response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPrice").asDouble(),
                        response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketDayHigh").asDouble(),
                        response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketDayLow").asDouble(),
                        response.getBody().get("quoteResponse").get("result").get(j).get("regularMarketPreviousClose").asDouble());
            }
            RootResponseDto<Object> myResponse = new RootResponseDto<>(200, HttpStatus.OK,
                    ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, arr);
            return new ResponseEntity<>(myResponse, new HttpHeaders(), 200);
        }
        else {
            return new ResponseEntity<>("FAILED", new HttpHeaders(), 500);
        }
    }
}
