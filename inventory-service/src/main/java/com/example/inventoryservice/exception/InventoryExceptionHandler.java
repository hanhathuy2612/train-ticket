package com.example.inventoryservice.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.shared.exception.GlobalExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class InventoryExceptionHandler extends GlobalExceptionHandler {
}
