package org.example.coursework3.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.dto.request.CaptchaRequest;
import org.example.coursework3.dto.request.EmailLoginRequest;
import org.example.coursework3.dto.request.LoginRequest;
import org.example.coursework3.dto.request.RegisterRequest;
import org.example.coursework3.entity.User;
import org.example.coursework3.dto.response.LoginResult;
import org.example.coursework3.dto.response.RegisterResult;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.AliyunMailService;
import org.example.coursework3.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private AliyunMailService mailService;

    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
       User user = authService.login(request.getEmail(),request.getPassword());
       LoginResult loginResult = new LoginResult();
       loginResult.setUser(user);
       authService.storeToken(loginResult);
       log.info("Password login successful! The corresponding token has been returned and stored.");
       return Result.success(loginResult);

    }
    @PostMapping("/send-email-code")
    public Result<Void> sendCaptcha(@Valid @RequestBody CaptchaRequest request) throws Exception {
        mailService.sendCaptcha(request.getEmail());
        log.info("Send verification code to {}",request.getEmail());
        return Result.success("Verification code sent.");
    }

    @PostMapping("/register")
    public Result<RegisterResult> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.getName(),request.getEmail(), request.getVerificationCode(), request.getPassword());
        RegisterResult registerResult = new RegisterResult();
        registerResult.setUser(user);
        authService.storeToken(registerResult);
        log.info("Registration successful! The corresponding token has been returned and stored.");
        return Result.success(registerResult);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.replace("Bearer ", "");
        authService.deleteToken(token);
        log.info("Logged out! Corresponding token has been deleted.");
        return Result.success("Logged out");
    }

    @PostMapping("/login-by-email-code")
    public Result<LoginResult> loginByEmail(@RequestBody EmailLoginRequest request){
        User user = authService.loginByCode(request.getEmail(), request.getVerificationCode());
        LoginResult loginResult = new LoginResult();
        loginResult.setUser(user);
        authService.storeToken(loginResult);
        log.info("Verification code login successful! The corresponding token has been returned and stored.");
        return Result.success(loginResult);
    }


}