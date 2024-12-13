package com.nguyengiap.security.application_api.admin_api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nguyengiap.security.config.jwt_config.JwtService;
import com.nguyengiap.security.database_model.user.User;
import com.nguyengiap.security.model.request_model.request_model_for_admin.ChangeUserPasswordRequest;
import com.nguyengiap.security.model.response_model.BalanceWithAccount;
import com.nguyengiap.security.model.response_model.UnauthorizedAccount;
import com.nguyengiap.security.service.user_service.UserService;

import lombok.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
class AdminAPI {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/change-user-password")
    public ResponseEntity<?> changeUserPassword(@RequestBody ChangeUserPasswordRequest request,
            @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        System.out.println(role);

        if (role.equals("ADMIN")) {
            Optional<User> user = userService.findByAccount(request.getAccount());

            if (user.isPresent()) {
                Optional<User> userAccount = userService.findByAccount(request.getAccount());

                if (userAccount.isPresent()) {
                    String newEncodedPassword = passwordEncoder.encode(request.getPassword());
                    userService.changePassword(request.getAccount(), newEncodedPassword);
                    return ResponseEntity.status(200).body(
                            UnauthorizedAccount.builder().status(200).message("Change password successfully").build());
                } else {
                    return ResponseEntity.status(404)
                            .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
                }
            } else {
                return ResponseEntity.status(404)
                        .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403)
                    .body(UnauthorizedAccount.builder().status(403).message("You are not admin").build());
        }
    }

    @PostMapping("/transfer-between-user")
    public ResponseEntity<?> transferBetweenUser(@RequestBody TranferBetweenUser request,
            @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        if (role.equals("ADMIN")) {
            Optional<User> fromAccount = userService.findByAccount(request.getFromAccount());
            Optional<User> toAccount = userService.findByAccount(request.getToAccount());

            if (fromAccount.isPresent() && toAccount.isPresent()) {
                Optional<BalanceWithAccount> fromAccountBalance = userService
                        .findBalanceByAccount(request.getFromAccount());

                if (fromAccountBalance.isPresent()) {
                    if (fromAccountBalance.get().getFund() >= request.getFund()) {
                        userService.bankingToAccount(request.getFromAccount(), request.getFund());
                        userService.bankingToAccount2(request.getToAccount(), request.getFund());
                        return ResponseEntity.status(200).body(
                                UnauthorizedAccount.builder().status(200).message("Transfer successfully").build());
                    } else {
                        return ResponseEntity.status(40)
                                .body(UnauthorizedAccount.builder().status(401).message("Fund not enough").build());
                    }
                } else {
                    return ResponseEntity.status(404)
                            .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
                }
            } else {
                return ResponseEntity.status(404)
                        .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403)
                    .body(UnauthorizedAccount.builder().status(403).message("You are not admin").build());
        }
    }

    @PostMapping("/add-fund-to-account")
    public ResponseEntity<?> addFundToAccount(@RequestBody AddFundToAccount request,
            @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        if (role.equals("ADMIN")) {
            Optional<User> user = userService.findByAccount(request.getAccount());

            if (user.isPresent()) {
                userService.bankingToAccount(request.getAccount(), request.getFund());
                return ResponseEntity.status(200)
                        .body(UnauthorizedAccount.builder().status(200).message("Add fund successfully").build());
            } else {
                return ResponseEntity.status(404)
                        .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403)
                    .body(UnauthorizedAccount.builder().status(403).message("You are not admin").build());
        }
    }

    @PostMapping("/change-user-email")
    public ResponseEntity<?> changeUserEmail(@RequestBody ChangeUserEmail request,
            @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        if (role.equals("ADMIN")) {
            Optional<User> user = userService.findByAccount(request.getAccount());

            if (user.isPresent()) {
                userService.updateEmail(request.getAccount(), request.getEmail());
                return ResponseEntity.status(200)
                        .body(UnauthorizedAccount.builder().status(200).message("Change email successfully").build());
            } else {
                return ResponseEntity.status(404)
                        .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403)
                    .body(UnauthorizedAccount.builder().status(403).message("You are not admin").build());
        }
    }
}