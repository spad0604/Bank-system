package com.nguyengiap.security.application_api.admin_api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nguyengiap.security.database_model.active_user.UserSession;
import com.nguyengiap.security.database_model.history_transistion.TransitionHistory;
import com.nguyengiap.security.service.transition_history_service.TransitionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.nguyengiap.security.config.jwt_config.JwtService;
import com.nguyengiap.security.database_model.user.User;
import com.nguyengiap.security.model.request_model.request_model_for_admin.ChangeUserInformationRequest;
import com.nguyengiap.security.model.request_model.request_model_for_admin.DeleteUserRequest;
import com.nguyengiap.security.model.request_model.request_model_for_admin.TransferBetweenUser;
import com.nguyengiap.security.model.response_model.ActiveUserResponse;
import com.nguyengiap.security.model.response_model.BalanceWithAccount;
import com.nguyengiap.security.model.response_model.UnauthorizedAccount;
import com.nguyengiap.security.service.user_service.UserService;
import com.nguyengiap.security.service.user_session_service.UserSessionService;

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

    @Autowired
    private TransitionHistoryService transitionHistoryService;

    @Autowired
    private UserSessionService userSessionService;

    @PostMapping("/change-user-information")
    public ResponseEntity<?> changeUserInformation(@RequestBody ChangeUserInformationRequest request,
            @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        if (role.equals("ADMIN")) {
            Optional<User> user = userService.findByAccount(request.getAccount());

            if (user.isPresent()) {
                Optional<User> userAccount = userService.findByAccount(request.getAccount());

                if (userAccount.isPresent()) {
                    Optional<User> existingEmailUser = userService.findByEmail(request.getEmail());
                    if (existingEmailUser.isPresent() && !existingEmailUser.get().getAccount().equals(request.getAccount())) {
                        return ResponseEntity.status(403)
                                .body(UnauthorizedAccount.builder().status(403).message("Email already exists").build());
                    }
                    String newEncodedPassword = passwordEncoder.encode(request.getPassword());
                    userService.changeUserInformation(request.getAccount(), newEncodedPassword, request.getFirstName(),
                            request.getLastName(), request.getEmail(), request.getAddress(), request.getPhoneNumber());
                    return ResponseEntity.status(200).body(
                            UnauthorizedAccount.builder().status(200).message("Change information successfully").build());
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
    public ResponseEntity<?> transferBetweenUser(@RequestBody TransferBetweenUser request,
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

    @GetMapping("/check-user-transition")
    public ResponseEntity<?> checkUserTransition(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = true) String account,
            @RequestParam(required = false) String dateTime,
            @RequestBody(required = false) String message) {
        final String role = jwtService.extractRole(token.substring(7));

        if (role.equals("ADMIN")) {
            Optional<User> user = userService.findByAccount(account);
            if (user.isPresent()) {
                if (dateTime != null) {
                    if (message != null) {
                        List<TransitionHistory> listTransitory = transitionHistoryService
                                .findTransitionByAccountAndDateTimeAndMessage(account, dateTime, message);
                        return ResponseEntity.status(200).body(listTransitory);
                    } else {
                        List<TransitionHistory> listTransitory = transitionHistoryService
                                .findTransitionByAccountAndDateTime(account, dateTime);
                        return ResponseEntity.status(200).body(listTransitory);
                    }
                } else {
                    if (message != null) {
                        List<TransitionHistory> listTransitory = transitionHistoryService
                                .findTransitionByAccountAndMessage(account, message);
                        return ResponseEntity.status(200).body(listTransitory);
                    } else {
                        List<TransitionHistory> listTransitory = transitionHistoryService
                                .findTransitionByAccount(account);
                        return ResponseEntity.status(200).body(listTransitory);
                    }
                }
            } else {
                return ResponseEntity.status(404)
                        .body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403)
                    .body(UnauthorizedAccount.builder().status(403).message("You are not admin"));
        }
    }

    @GetMapping("/get-all-active-session")
    public ResponseEntity<?> getAllActiveSession(@RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));

        if(role.equals("ADMIN")) {
            try {
                List<ActiveUserResponse> listUser = new ArrayList<>();
                List<UserSession> listSession = userSessionService.getUserSession();
                
                for(UserSession session : listSession) {
                    ActiveUserResponse user = ActiveUserResponse.builder()
                                    .account(session.getAccount())
                                    .firstName(session.getFirstName())
                                    .lastName(session.getLastName())
                                    .email(session.getEmail())
                                    .phoneNumber(session.getPhoneNumber())
                                    .balance(session.getBalance())
                                    .build();
                    listUser.add(user);
                }
                return ResponseEntity.status(200).body(listUser);
            } catch (Exception e) {
                return ResponseEntity.status(500)
                    .body(UnauthorizedAccount.builder()
                        .status(500)
                        .message("Error retrieving user sessions: " + e.getMessage())
                        .build());
            }
        } else {
            return ResponseEntity.status(403)
                .body(UnauthorizedAccount.builder()
                    .status(403)
                    .message("You are not admin")
                    .build());
        }
    }

    @PostMapping("/delete-user")
    public ResponseEntity<?> deleteUser(@RequestBody DeleteUserRequest request, @RequestHeader("Authorization") String token) {
        final String role = jwtService.extractRole(token.substring(7));
        Optional<User> user = userService.findByAccount(request.getAccount());
        if(role.equals("ADMIN")) {
            if(user.isPresent()) {
                userService.deleteUser(request.getAccount());
                return ResponseEntity.status(200).body(UnauthorizedAccount.builder().status(200).message("Delete user successfully").build());
            } else {
                return ResponseEntity.status(404).body(UnauthorizedAccount.builder().status(404).message("Account not found").build());
            }
        } else {
            return ResponseEntity.status(403).body(UnauthorizedAccount.builder().status(403).message("You are not admin"));
        }
    }
}