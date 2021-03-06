package exceptionhandler;

import exceptions.*;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public final class ExceptionHandlerAdvice { 

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Object> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UsernameAlreadyExistsException.HTTP_STATUS.value())
                                                                .put("error", UsernameAlreadyExistsException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UsernameAlreadyExistsException.HTTP_STATUS);
    }  
     
    @ExceptionHandler(UsernameDoesNotExistException.class)
    public ResponseEntity<Object> handleUsernameDoesNotExistException(UsernameDoesNotExistException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UsernameDoesNotExistException.HTTP_STATUS.value())
                                                                .put("error", UsernameDoesNotExistException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UsernameDoesNotExistException.HTTP_STATUS);
    } 
    
    @ExceptionHandler(ChatAlreadyExistsException.class)
    public ResponseEntity<Object> handleChatAlreadyExistsException(ChatAlreadyExistsException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", ChatAlreadyExistsException.HTTP_STATUS.value())
                                                                .put("error", ChatAlreadyExistsException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, ChatAlreadyExistsException.HTTP_STATUS);
    }

    @ExceptionHandler(UserIdDoesNotExistException.class)
    public ResponseEntity<Object> handleUserIdDoesNotExistException(UserIdDoesNotExistException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UserIdDoesNotExistException.HTTP_STATUS.value())
                                                                .put("error", UserIdDoesNotExistException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UserIdDoesNotExistException.HTTP_STATUS);
    }

    @ExceptionHandler(ChatIdDoesNotExistException.class)
    public ResponseEntity<Object> handleChatIdDoesNotExistException(ChatIdDoesNotExistException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", ChatIdDoesNotExistException.HTTP_STATUS.value())
                                                                .put("error", ChatIdDoesNotExistException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, ChatIdDoesNotExistException.HTTP_STATUS);
    }  

    @ExceptionHandler(MessageIdDoesNotExistException.class)
    public ResponseEntity<Object> handleMessageIdDoesNotExistException(MessageIdDoesNotExistException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", MessageIdDoesNotExistException.HTTP_STATUS.value())
                                                                .put("error", MessageIdDoesNotExistException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, MessageIdDoesNotExistException.HTTP_STATUS);
    }  
    
    @ExceptionHandler(UserChatIdDoesNotExistException.class)
    public ResponseEntity<Object> handleUserChatIdDoesNotExistException(UserChatIdDoesNotExistException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UserChatIdDoesNotExistException.HTTP_STATUS.value())
                                                                .put("error", UserChatIdDoesNotExistException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UserChatIdDoesNotExistException.HTTP_STATUS);
    }  

    @ExceptionHandler(MessageIdDoesNotBelongToChatIdException.class)
    public ResponseEntity<Object> handleMessageIdDoesNotBelongToChatIdException(MessageIdDoesNotBelongToChatIdException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", MessageIdDoesNotBelongToChatIdException.HTTP_STATUS.value())
                                                                .put("error", MessageIdDoesNotBelongToChatIdException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, MessageIdDoesNotBelongToChatIdException.HTTP_STATUS);
    }

    @ExceptionHandler(UsernameMissingFromRequestBodyException.class)
    public ResponseEntity<Object> handleUsernameMissingFromRequestBodyException(UsernameMissingFromRequestBodyException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UsernameMissingFromRequestBodyException.HTTP_STATUS.value())
                                                                .put("error", UsernameMissingFromRequestBodyException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UsernameMissingFromRequestBodyException.HTTP_STATUS);
    }

    @ExceptionHandler(UserIdMissingFromRequestURLPathException.class)
    public ResponseEntity<Object> handleUserIdMissingFromRequestURLPathException(UserIdMissingFromRequestURLPathException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UserIdMissingFromRequestURLPathException.HTTP_STATUS.value())
                                                                .put("error", UserIdMissingFromRequestURLPathException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UserIdMissingFromRequestURLPathException.HTTP_STATUS);
    }

    @ExceptionHandler(UserIdMissingFromRequestURLParameterException.class)
    public ResponseEntity<Object> handleUserIdMissingFromRequestURLParameterException(UserIdMissingFromRequestURLParameterException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", UserIdMissingFromRequestURLParameterException.HTTP_STATUS.value())
                                                                .put("error", UserIdMissingFromRequestURLParameterException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, UserIdMissingFromRequestURLParameterException.HTTP_STATUS);
    }

    @ExceptionHandler(ChatIdMissingFromRequestURLParameterException.class)
    public ResponseEntity<Object> handleChatIdMissingFromRequestURLParameterException(ChatIdMissingFromRequestURLParameterException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", ChatIdMissingFromRequestURLParameterException.HTTP_STATUS.value())
                                                                .put("error", ChatIdMissingFromRequestURLParameterException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, ChatIdMissingFromRequestURLParameterException.HTTP_STATUS);
    }

    @ExceptionHandler(TextContentMissingFromRequestBodyException.class)
    public ResponseEntity<Object> handleTextContentMissingFromRequestBodyException(TextContentMissingFromRequestBodyException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", TextContentMissingFromRequestBodyException.HTTP_STATUS.value())
                                                                .put("error", TextContentMissingFromRequestBodyException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, TextContentMissingFromRequestBodyException.HTTP_STATUS);
    }

    @ExceptionHandler(ChatIdMissingFromRequestURLPathException.class)
    public ResponseEntity<Object> handleChatIdMissingFromRequestURLPathException(ChatIdMissingFromRequestURLPathException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", ChatIdMissingFromRequestURLPathException.HTTP_STATUS.value())
                                                                .put("error", ChatIdMissingFromRequestURLPathException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, ChatIdMissingFromRequestURLPathException.HTTP_STATUS);
    }

    @ExceptionHandler(MessageIdMissingFromRequestURLParameterException.class)
    public ResponseEntity<Object> handleMessageIdMissingFromRequestURLParameterException(MessageIdMissingFromRequestURLParameterException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", MessageIdMissingFromRequestURLParameterException.HTTP_STATUS.value())
                                                                .put("error", MessageIdMissingFromRequestURLParameterException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, MessageIdMissingFromRequestURLParameterException.HTTP_STATUS);
    }

    @ExceptionHandler(InvalidCountValueInRequestURLParameterException.class)
    public ResponseEntity<Object> handleInvalidCountValueInRequestURLParameterException(InvalidCountValueInRequestURLParameterException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", InvalidCountValueInRequestURLParameterException.HTTP_STATUS.value())
                                                                .put("error", InvalidCountValueInRequestURLParameterException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, InvalidCountValueInRequestURLParameterException.HTTP_STATUS);
    }  

    @ExceptionHandler(EmptyMessageException.class)
    public ResponseEntity<Object> EmptyMessageException(EmptyMessageException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object> builder()
                                                                .put("timestamp", LocalDateTime.now())
                                                                .put("status", EmptyMessageException.HTTP_STATUS.value())
                                                                .put("error", EmptyMessageException.HTTP_STATUS.toString())
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, EmptyMessageException.HTTP_STATUS);
    }

    @ExceptionHandler(MessageDoesNotContainAttachmentException.class)
    public ResponseEntity<Object> MessageDoesNotContainAttachmentException(MessageDoesNotContainAttachmentException e) {

        ImmutableMap<String, Object> responseBody = ImmutableMap.<String, Object>builder()
                                                                .put("timestamp", LocalDateTime.now()) 
                                                                .put("status", MessageDoesNotContainAttachmentException.HTTP_STATUS.value()) 
                                                                .put("error", MessageDoesNotContainAttachmentException.HTTP_STATUS.toString()) 
                                                                .put("message", e.getMessage())
                                                                .put("path", e.getPath())
                                                                .build();

        return new ResponseEntity<Object>(responseBody, MessageDoesNotContainAttachmentException.HTTP_STATUS);
    }
} 
