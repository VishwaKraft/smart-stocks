package com.smartstocks.product.exception;

import com.smartstocks.product.dto.RootResponseDto;
import com.smartstocks.product.util.ResponseMessage;
import lombok.Builder;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(UnknownHttpStatusCodeException.class)
    public ResponseEntity<RootResponseDto> handleCustomExceptions(UnknownHttpStatusCodeException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Something went wrong");
        RootResponseDto<String> response = new RootResponseDto<>(500, HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(response, new HttpHeaders(), 500);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler({InSufficientBalanceException.class, InvalidTransactionException.class,
    InvalidUnitException.class, InvalidStockSelection.class, ConstraintViolationException.class})
    public ResponseEntity<RootResponseDto> handleCustomExceptions(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        if(ex instanceof InSufficientBalanceException) errors.put("error", ex.getLocalizedMessage());
        else if(ex instanceof InvalidTransactionException) errors.put("error", ex.getLocalizedMessage());
        else if(ex instanceof InvalidStockSelection) errors.put("error", ex.getLocalizedMessage());
        else if(ex instanceof ConstraintViolationException) errors.put("error", "Payload is bad");
        else errors.put("INVALID UNIT", ex.getLocalizedMessage());
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(response, new HttpHeaders(), 400);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RootResponseDto> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("template_id") && message.contains("not-null")) {
            errors.put("error", "template_id cannot be null. Ensure the campaign type is correct and a valid templateId is provided for EMAIL campaigns.");
        } else if (message != null && message.contains("not-null")) {
            errors.put("error", "A required field is missing: " + message);
        } else {
            errors.put("error", "Database constraint violation: " + message);
        }
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(response, new HttpHeaders(), 400);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RootResponseDto> badCredentialsExceptionHandler(BadCredentialsException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "Entered wrong Password or E-mail");
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(response, new HttpHeaders(), 400);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<RootResponseDto> UsernameNotFoundExceptionHandler(UsernameNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getLocalizedMessage());
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return new ResponseEntity<>(response, new HttpHeaders(), 400);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        String SupportedMethods = Arrays.asList(ex.getSupportedMethods()).stream().collect(Collectors.joining(", "));
        errors.put("error", "Request method " + ex.getMethod() + " is not supported by this endpoint. Supported Request methods are: " + SupportedMethods);
        RootResponseDto<String> response = new RootResponseDto<>(405, HttpStatus.METHOD_NOT_ALLOWED,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return handleExceptionInternal(ex, response, headers, response.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        errors.put("error", "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL());
        RootResponseDto<String> response = new RootResponseDto<>(404, HttpStatus.NOT_FOUND,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), errors, null);
        return handleExceptionInternal(ex, response, headers, response.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> map = new HashMap<>();
        map.put("error", ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), map, null);
        return handleExceptionInternal(ex, response, headers, response.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getContentType());
        builder.append(" media type is not supported. Supported media types are ");
        ex.getSupportedMediaTypes().forEach(t -> builder.append(t + ", "));
        Map<String, String> map = new HashMap<>();
        map.put("error", builder.substring(0, builder.length() - 2));
        RootResponseDto<String> response = new RootResponseDto<>(415, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), map, null);
        return handleExceptionInternal(ex, response, headers, response.getStatus(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, String> map = new HashMap<>();
        map.put("error", ex.getLocalizedMessage());
        RootResponseDto<String> response = new RootResponseDto<>(400, HttpStatus.BAD_REQUEST,
                ResponseMessage.FAILED.toString(), LocalDateTime.now(), map, null);
        return handleExceptionInternal(ex, response, headers, response.getStatus(), request);
    }
}
