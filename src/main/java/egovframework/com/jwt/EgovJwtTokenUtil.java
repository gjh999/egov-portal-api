package egovframework.com.jwt;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import egovframework.com.cmm.LoginVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성·파싱 유틸.
 *
 * <p>서버 렌더링 시절 이 포털은 세션(HttpSession)에 로그인 상태를 담았다. SPA 로 전환하면서
 * 상태를 서버에 두지 않고(STATELESS) JWT 를 HttpOnly 쿠키로 주고받는다.</p>
 *
 * <p>토큰에는 화면 표시·권한 판정에 필요한 최소 정보만 담는다. 비밀번호 등 민감정보는 넣지 않는다
 * (JWT 본문은 서명될 뿐 암호화되지 않아 누구나 디코딩해 읽을 수 있다).</p>
 */
@Slf4j
@Component
public class EgovJwtTokenUtil implements Serializable {

	private static final long serialVersionUID = -5180902194184255251L;

	/** JWT 토큰 유효시간 (초) — 1시간 */
	public static final long JWT_TOKEN_VALIDITY = 60 * 60;

	@Value("${Globals.jwt.secret}")
	private String secretKeyString;

	@PostConstruct
	private void validateSecret() {
		// localhost 개발 편의를 위해 application.properties 에 디폴트 placeholder 가 들어 있다.
		// 운영 배포 전 환경변수 EGOV_JWT_SECRET 으로 32자 이상 무작위 값으로 교체해야 한다.
		if (secretKeyString == null || secretKeyString.length() < 32) {
			log.warn("[SECURITY] JWT secret is null or shorter than 32 characters."
					+ " Set EGOV_JWT_SECRET env var before production deployment.");
		} else if (secretKeyString.contains("my-secret-jwt-key-for-egovframe")) {
			log.warn("[SECURITY] Default JWT secret placeholder detected."
					+ " Set EGOV_JWT_SECRET env var before production deployment.");
		}
	}

	private SecretKey getSecretKey() {
		return Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 로그인 사용자와 권한으로 토큰을 만든다.
	 *
	 * @param loginVO 인증에 성공한 사용자
	 * @param role    부여할 권한 (ROLE_ADMIN / ROLE_USER)
	 */
	public String generateToken(LoginVO loginVO, String role) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("id", loginVO.getId());
		claims.put("name", loginVO.getName());
		claims.put("userSe", loginVO.getUserSe());
		claims.put("orgnztId", loginVO.getOrgnztId());
		claims.put("uniqId", loginVO.getUniqId());
		claims.put("email", loginVO.getEmail());
		claims.put("role", role);

		return Jwts.builder()
				.claims(claims)
				.subject("Authorization")
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
				.signWith(getSecretKey())
				.compact();
	}

	public Claims getAllClaimsFromToken(String token) {
		return Jwts.parser()
				.verifyWith(getSecretKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private String getInfoFromToken(String type, Claims claims) {
		Object info = claims.get(type);
		return info != null ? info.toString() : null;
	}

	/** 토큰에 담긴 권한. 값이 없으면 일반 사용자로 본다. */
	public String getRoleFromToken(String token) throws InvalidJwtException {
		String role = getInfoFromToken("role", parse(token));
		return (role == null || role.isBlank()) ? "ROLE_USER" : role;
	}

	/**
	 * 토큰에서 로그인 사용자를 복원한다.
	 *
	 * @throws InvalidJwtException 서명이 맞지 않거나 만료됐거나 id 클레임이 없는 경우
	 */
	public LoginVO getLoginVOFromToken(String token) throws InvalidJwtException {
		Claims claims = parse(token);

		LoginVO loginVO = new LoginVO();
		loginVO.setId(getInfoFromToken("id", claims));
		loginVO.setName(getInfoFromToken("name", claims));
		loginVO.setUserSe(getInfoFromToken("userSe", claims));
		loginVO.setOrgnztId(getInfoFromToken("orgnztId", claims));
		loginVO.setUniqId(getInfoFromToken("uniqId", claims));
		loginVO.setEmail(getInfoFromToken("email", claims));

		if (loginVO.getId() == null) {
			throw new InvalidJwtException("Missing id in token");
		}
		return loginVO;
	}

	private Claims parse(String token) throws InvalidJwtException {
		try {
			return getAllClaimsFromToken(token);
		} catch (IllegalArgumentException | JwtException e) {
			throw new InvalidJwtException("Unable to verify JWT Token: " + e.getMessage());
		}
	}
}
