package com.thekey.stylekeyserver.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.thekey.stylekeyserver.auth.dto.request.AuthReissueRequestDto;
import com.thekey.stylekeyserver.auth.dto.request.AuthRequestDto;
import com.thekey.stylekeyserver.auth.dto.response.AuthReissueResponseDto;
import com.thekey.stylekeyserver.auth.dto.response.AuthResponseDto;
import com.thekey.stylekeyserver.auth.entity.AuthEntity;
import com.thekey.stylekeyserver.auth.repository.AuthRepository;
import com.thekey.stylekeyserver.global.jwt.JwtUtil;
import com.thekey.stylekeyserver.global.jwt.TokenDto;
import com.thekey.stylekeyserver.global.response.ApiResponseDto;
import com.thekey.stylekeyserver.global.response.ErrorType;
import com.thekey.stylekeyserver.global.response.SuccessType;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    // private final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtUtil jwtUtil;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResponseEntity<ApiResponseDto> signUp(AuthRequestDto authRequestDto) {

        Optional<AuthEntity> member = authRepository.findByUserId(authRequestDto.getMember_id());

        if (member.isPresent()) {
            return ResponseEntity.ok(ApiResponseDto.of(ErrorType.USER_EXIST));
        }

        String encodedPassword = passwordEncoder.encode(authRequestDto.getPassword());
        OffsetDateTime now = OffsetDateTime.now();
        authRepository.save(AuthEntity.of(authRequestDto.getMember_id(), encodedPassword, now));
        return ResponseEntity.ok(ApiResponseDto.of(SuccessType.SIGN_UP_SUCCESS));
    }

    public ResponseEntity<ApiResponseDto> login(AuthRequestDto authRequestDto, HttpServletResponse response) {

        Optional<AuthEntity> member = authRepository.findByUserId(authRequestDto.getMember_id());

        if (member.isEmpty()) {
            return ResponseEntity.ok(ApiResponseDto.of(ErrorType.USER_NOT_FOUND));
        }

        if (!passwordEncoder.matches(authRequestDto.getPassword(), member.get().getPassword())) {
            return ResponseEntity.ok(ApiResponseDto.of(ErrorType.PASSWORD_MISMATCH));
        }

        String accessToken = jwtUtil.createToken(authRequestDto.getMember_id());
        String refreshToken = jwtUtil.createRefreshToken(authRequestDto.getMember_id());

        TokenDto tokenDto = new TokenDto(accessToken, refreshToken);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, tokenDto.getAccessToken());

        AuthResponseDto authResponseDto = AuthResponseDto.of(
            member.get().getUserId(),
            accessToken,
            refreshToken
        );

        return ResponseEntity.ok(ApiResponseDto.of(SuccessType.LOG_IN_SUCCESS, authResponseDto));
    }

    @Transactional
    public ResponseEntity<ApiResponseDto> changePassword(AuthRequestDto authRequestDto) {

        Optional<AuthEntity> member = authRepository.findByUserId(authRequestDto.getMember_id());

        if (member.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseDto.of(ErrorType.USER_NOT_FOUND));
        }

        String encodedPassword = passwordEncoder.encode(authRequestDto.getPassword());
        member.get().changePassword(encodedPassword);
        authRepository.save(member.get());

        return ResponseEntity.ok(ApiResponseDto.of(SuccessType.CHANGE_PASSWORD));
    }

    @Transactional
    public ResponseEntity<ApiResponseDto> reissueToken(@Valid AuthReissueRequestDto authReissueRequestDto,
            HttpServletResponse response) {
        jwtUtil.validateToken(authReissueRequestDto.getRefresh_token());
        // String user_id = jwtUtil.getUserInfoFromToken(authReissueRequestDto.getAccess_token()).toString();

        String accessToken = jwtUtil.refreshToken(authReissueRequestDto.getAccess_token());
        AuthReissueResponseDto authReissueResponseDto = AuthReissueResponseDto.of(accessToken);

        return ResponseEntity.ok(ApiResponseDto.of(SuccessType.REISSUE_SUCCESS, authReissueResponseDto));
    
    }
}