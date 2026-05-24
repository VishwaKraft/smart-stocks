package com.smartstocks.product.controllers;

import com.smartstocks.product.dto.EventLogRequestDto;
import com.smartstocks.product.dto.EventLogResponseDto;
import com.smartstocks.product.dto.RootResponseDto;
import com.smartstocks.product.service.IEventLogService;
import com.smartstocks.product.util.HttpRequestUtils;
import com.smartstocks.product.util.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@CrossOrigin(origins = "*")
@Validated
public class EventLogController {

    @Autowired
    private IEventLogService eventLogService;

    @PostMapping
    public ResponseEntity<RootResponseDto<EventLogResponseDto>> logEvent(
            @Valid @RequestBody EventLogRequestDto request,
            HttpServletRequest httpRequest,
            Principal principal) {
        EventLogResponseDto saved = eventLogService.logEvent(
                request,
                HttpRequestUtils.resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                HttpRequestUtils.extractHeaders(httpRequest),
                principal
        );
        RootResponseDto<EventLogResponseDto> response = new RootResponseDto<>(
                201, HttpStatus.CREATED, ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, saved);
        return new ResponseEntity<>(response, new HttpHeaders(), 201);
    }

    @GetMapping
    public ResponseEntity<RootResponseDto<List<EventLogResponseDto>>> getEvents(
            @RequestParam(value = "user_id", required = false) Long userId,
            @RequestParam(value = "event_type", required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        Page<EventLogResponseDto> events = eventLogService.getEvents(
                userId, eventType, PageRequest.of(page, size), principal);
        RootResponseDto<List<EventLogResponseDto>> response = new RootResponseDto<>(
                200, HttpStatus.OK, ResponseMessage.SUCCESS.toString(), LocalDateTime.now(), null, events.getContent());
        return new ResponseEntity<>(response, new HttpHeaders(), 200);
    }
}
