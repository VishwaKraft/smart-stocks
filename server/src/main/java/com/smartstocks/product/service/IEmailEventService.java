package com.smartstocks.product.service;

import com.smartstocks.product.dto.CreateEmailEventRequestDto;
import com.smartstocks.product.dto.EmailEventDto;
import com.smartstocks.product.dto.TriggerEventEmailRequestDto;
import com.smartstocks.product.dto.TriggerEventEmailResponseDto;

import java.util.List;
import java.util.Optional;

public interface IEmailEventService {

    /** Create a new email event linking a campaign and template. */
    EmailEventDto createEmailEvent(CreateEmailEventRequestDto request);

    /** List all email events (including inactive). */
    List<EmailEventDto> listAllEmailEvents();

    /** List only active email events. */
    List<EmailEventDto> listActiveEmailEvents();

    /** Find a single event DTO by ID. */
    Optional<EmailEventDto> getById(Long id);

    /** Soft-delete an email event (sets isActive = false). */
    boolean deleteEmailEvent(Long id);

    /**
     * Trigger the event: render the associated template with the given variables,
     * resolve the campaign's email provider, send to the supplied recipients,
     * and persist an audit log entry.
     */
    TriggerEventEmailResponseDto triggerEvent(String eventName, TriggerEventEmailRequestDto request);
}
