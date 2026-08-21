package egovframework.com.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import egovframework.com.cmm.LoginVO;

/**
 * JWT 토큰 생성·파싱 단위 테스트.
 *
 * <p>서명 키는 {@code @Value("${Globals.jwt.secret}")} 로 주입된다. 스프링 컨텍스트 없이
 * {@code new} 로 만들면 키가 {@code null} 이라 서명 단계에서 NPE 가 난다. 컨텍스트 전체를 띄우는 대신
 * 테스트용 키를 리플렉션으로 주입해 단위 테스트 속도를 유지한다.</p>
 */
class EgovJwtTokenUtilTest {

	/** HS256 서명에 필요한 최소 길이(32바이트)를 충족하는 테스트 전용 키 */
	private static final String TEST_SECRET = "test-secret-key-for-unit-test-only-32bytes+";

	private EgovJwtTokenUtil jwtTokenUtil;

	@BeforeEach
	void setUp() {
		jwtTokenUtil = new EgovJwtTokenUtil();
		ReflectionTestUtils.setField(jwtTokenUtil, "secretKeyString", TEST_SECRET);
	}

	private LoginVO sampleUser() {
		LoginVO loginVO = new LoginVO();
		loginVO.setId("admin");
		loginVO.setName("관리자");
		loginVO.setUserSe("USR");
		loginVO.setOrgnztId("ORGNZT_0000000000000");
		loginVO.setUniqId("USRCNFRM_00000000000");
		loginVO.setEmail("admin@example.com");
		return loginVO;
	}

	@DisplayName("발급한 토큰에서 로그인 사용자를 그대로 복원한다")
	@Test
	void generateAndParse() {
		String token = jwtTokenUtil.generateToken(sampleUser(), "ROLE_ADMIN");

		LoginVO result = jwtTokenUtil.getLoginVOFromToken(token);

		assertNotNull(result);
		assertEquals("admin", result.getId());
		assertEquals("관리자", result.getName());
		assertEquals("USR", result.getUserSe());
		assertEquals("ORGNZT_0000000000000", result.getOrgnztId());
		assertEquals("USRCNFRM_00000000000", result.getUniqId());
		assertEquals("admin@example.com", result.getEmail());
	}

	@DisplayName("토큰에 담은 권한을 그대로 읽어온다")
	@Test
	void roleIsPreserved() {
		assertEquals("ROLE_ADMIN", jwtTokenUtil.getRoleFromToken(
				jwtTokenUtil.generateToken(sampleUser(), "ROLE_ADMIN")));
		assertEquals("ROLE_USER", jwtTokenUtil.getRoleFromToken(
				jwtTokenUtil.generateToken(sampleUser(), "ROLE_USER")));
	}

	@DisplayName("권한 클레임이 비어 있으면 일반 사용자로 본다")
	@Test
	void blankRoleFallsBackToUser() {
		String token = jwtTokenUtil.generateToken(sampleUser(), "");
		assertEquals("ROLE_USER", jwtTokenUtil.getRoleFromToken(token));
	}

	@DisplayName("형식이 잘못된 토큰은 InvalidJwtException 이다")
	@Test
	void malformedTokenIsRejected() {
		assertThrows(InvalidJwtException.class, () -> jwtTokenUtil.getLoginVOFromToken("not-a-jwt"));
	}

	@DisplayName("id 클레임이 없는 토큰은 InvalidJwtException 이다")
	@Test
	void tokenWithoutIdIsRejected() {
		String token = jwtTokenUtil.generateToken(new LoginVO(), "ROLE_USER");
		assertThrows(InvalidJwtException.class, () -> jwtTokenUtil.getLoginVOFromToken(token));
	}

	@DisplayName("다른 키로 서명된 토큰은 InvalidJwtException 이다 (위조 방지)")
	@Test
	void tokenSignedWithOtherSecretIsRejected() {
		EgovJwtTokenUtil otherUtil = new EgovJwtTokenUtil();
		ReflectionTestUtils.setField(otherUtil, "secretKeyString", "another-secret-key-that-is-long-enough-32");
		String forged = otherUtil.generateToken(sampleUser(), "ROLE_ADMIN");

		assertThrows(InvalidJwtException.class, () -> jwtTokenUtil.getLoginVOFromToken(forged));
	}
}
