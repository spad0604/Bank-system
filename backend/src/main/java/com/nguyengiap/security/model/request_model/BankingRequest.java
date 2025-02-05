package com.nguyengiap.security.model.request_model;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankingRequest {
    private String fromAccount;
    private String toAccount;
    private long fund;
    private String message;
}
