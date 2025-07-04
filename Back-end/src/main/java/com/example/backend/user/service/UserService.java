package com.example.backend.user.service;

import com.example.backend.jwt.config.JWTGenerator;
import com.example.backend.jwt.dto.JwtDto;
import com.example.backend.user.dto.request.UserRequest;
import com.example.backend.user.dto.response.UserResponse;
import com.example.backend.user.entity.User;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTGenerator jwtGenerator;

    // ✅ UserId 구하기
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        return UUID.fromString(authentication.getName());
    }

    // ✅ 로그인 실패 기록
    private final Map<String, LoginAttempt> loginFailures = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 5 * 60 * 1000; // 5분

    // ✅ 로그인 실패 횟수 및 마지막 로그인 시도 시간
    private static class LoginAttempt {
        int count;
        long lastAttemptTime;

        LoginAttempt(int count, long lastAttemptTime) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
        }
    }

    // 1️⃣ 회원가입 로직
    @Transactional
    public void register(UserRequest.registerRequest register) {
        if (!isValidEmail(register.getEmail())) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        } else if (userRepository.existsByEmail(register.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(register.getPassword());

        User user = User.builder()
                .email(register.getEmail())
                .password(encodedPassword)
                .userNickname(register.getUserNickname())
                .userName(register.getUserName())
                .userRole(User.Role.USER)
                .userProfileImage(register.getUserProfileImage()) // ✅ 이미지 URL 직접 저장
                .build();
        userRepository.save(user);
    }

    // 2️⃣ 유저정보 변경 로직
    @Transactional
    public UserResponse.updateResponse update(UserRequest.updateRequest updateRequest) {
        User user = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (updateRequest.getEmail() != null) {
            if (!isValidEmail(updateRequest.getEmail())) {
                throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
            }
            if (userRepository.existsByEmailAndUserIdNot(updateRequest.getEmail(), user.getUserId())) {
                throw new IllegalArgumentException("이미 등록된 이메일입니다.");
            }
        }

        user.updateUserInfo(
                updateRequest.getEmail(),
                updateRequest.getPassword(),
                updateRequest.getUserName(),
                updateRequest.getUserNickname(),
                updateRequest.getUserProfileImage(),
                passwordEncoder
        );

        return UserResponse.updateResponse.builder()
                .email(user.getEmail())
                .userName(user.getUserName())
                .userNickname(user.getUserNickname())
                .userProfileImage(user.getUserProfileImage())
                .build();
    }
    // 3️⃣ 유저 삭제 로직
    @Transactional
    public void delete() {
        UUID userId = getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }
    // 4️⃣ 로그인 로직
    public UserResponse.loginResponse login(UserRequest.loginRequest login) {
        String email = login.getEmail();

        if (isBlocked(email)) {
            throw new IllegalArgumentException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    recordLoginFailure(email);
                    return new IllegalArgumentException("이메일이 존재하지 않습니다.");
                });

        if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            recordLoginFailure(email);
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        resetLoginFailures(email);
        log.info("로그인 성공 - 사용자 이메일 : {}", email);

        JwtDto jwtDto = jwtGenerator.generateToken(user);

        return UserResponse.loginResponse.builder()
                .jwtDto(jwtDto)
                .userNickname(user.getUserNickname())
                .userProfileImage(user.getUserProfileImage()) // ✅ 바로 반환
                .build();
    }

    // ✅ 이메일 유효성 검사
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailRegex);
    }

    // ✅ 로그인 실패 기록
    private void recordLoginFailure(String email) {
        LoginAttempt attempt = loginFailures.getOrDefault(email, new LoginAttempt(0, System.currentTimeMillis()));
        attempt.count++;
        attempt.lastAttemptTime = System.currentTimeMillis();
        loginFailures.put(email, attempt);
    }

    // ✅ 로그인 성공 시 실패 기록 초기화
    private void resetLoginFailures(String email) {
        loginFailures.remove(email);
    }

    // ✅ 차단 여부 확인 (차단 시간 경과 시 자동 해제)
    private boolean isBlocked(String email) {
        LoginAttempt attempt = loginFailures.get(email);
        if (attempt == null) return false;

        long now = System.currentTimeMillis();

        if (now - attempt.lastAttemptTime > BLOCK_DURATION_MS) {
            loginFailures.remove(email);
            return false;
        }

        return attempt.count >= MAX_ATTEMPTS;
    }
}
