package egovframework.let.uat.uia.web;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egovframe.rte.fdl.security.userdetails.EgovUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.ResultVO;
import egovframework.com.jwt.EgovJwtTokenUtil;
import egovframework.let.uat.uia.service.EgovLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 기반 로그인 API.
 *
 * <p>서버 렌더링 시절에는 로그인 성공 후 세션에 SecurityContext 를 저장하고 메인 화면으로 리다이렉트했다.
 * SPA 에서는 화면 이동을 프론트가 하므로, 서버는 <b>ACCESS_TOKEN HttpOnly 쿠키</b>만 심고 JSON 을 돌려준다.</p>
 *
 * <p><b>토큰은 응답 본문에 넣지 않는다.</b> 본문에 실으면 프론트의 JS 가 읽을 수 있게 되어
 * XSS 로 탈취 가능해진다. HttpOnly 쿠키는 JS 가 읽을 수 없다.</p>
 */
@Slf4j
@RestController
@Tag(name = "EgovLoginApiController", description = "로그인 · 인증")
public class EgovLoginApiController {

	private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

	@Resource(name = "loginService")
	private EgovLoginService loginService;

	@Resource(name = "egovMessageSource")
	private EgovMessageSource egovMessageSource;

	@Autowired
	private EgovJwtTokenUtil jwtTokenUtil;

	@Value("${Globals.jwt.cookieSecure:false}")
	private boolean cookieSecure;

	/**
	 * 쿠키 SameSite 속성.
	 * 프론트와 API 가 같은 사이트면 {@code Lax} 로 충분하다. 등록도메인이 서로 다르면
	 * 브라우저가 크로스사이트 요청에 쿠키를 싣지 않으므로 {@code None} + Secure(HTTPS) 가 필요하다.
	 */
	@Value("${Globals.jwt.cookieSameSite:Lax}")
	private String cookieSameSite;

	@Operation(summary = "로그인", description = "자격증명 확인 후 ACCESS_TOKEN 쿠키를 발급한다.",
			tags = {"EgovLoginApiController"})
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그인 성공(resultCode 200) 또는 실패(resultCode 300)")
	})
	@PostMapping("/auth/login-jwt")
	public Map<String, Object> login(@RequestBody LoginVO loginVO, HttpServletResponse response) throws Exception {
		Map<String, Object> resultMap = new HashMap<>();

		if (loginVO.getUserSe() == null || loginVO.getUserSe().isEmpty()) {
			loginVO.setUserSe("USR"); // 기본: 업무사용자(직원)
		}

		LoginVO resultVO = loginService.actionLogin(loginVO);

		if (resultVO == null || resultVO.getId() == null || resultVO.getId().isEmpty()) {
			resultMap.put("resultCode", "300");
			resultMap.put("resultMessage", egovMessageSource.getMessage("fail.common.login"));
			return resultMap;
		}

		String role = resolveRole(resultVO);
		String jwtToken = jwtTokenUtil.generateToken(resultVO, role);

		ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, jwtToken)
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite(cookieSameSite)
				.path("/")
				.maxAge(Duration.ofSeconds(EgovJwtTokenUtil.JWT_TOKEN_VALIDITY))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		// 응답 본문에는 화면 표시·권한 분기에 필요한 최소 정보만 담는다 (토큰·비밀번호 미포함)
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("id", resultVO.getId());
		userInfo.put("name", resultVO.getName());
		userInfo.put("userSe", resultVO.getUserSe());
		userInfo.put("uniqId", resultVO.getUniqId());
		userInfo.put("roles", List.of(role));

		resultMap.put("resultVO", userInfo);
		resultMap.put("resultCode", "200");
		resultMap.put("resultMessage", "성공했습니다.");

		log.debug("로그인 성공: {} / 권한 {}", resultVO.getId(), role);
		return resultMap;
	}

	@Operation(summary = "로그아웃", description = "ACCESS_TOKEN 쿠키를 만료시킨다.",
			tags = {"EgovLoginApiController"})
	@GetMapping("/auth/logout")
	public ResultVO logout(HttpServletResponse response) {
		ResponseCookie expired = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite(cookieSameSite)
				.path("/")
				.maxAge(Duration.ZERO)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
		SecurityContextHolder.clearContext();

		ResultVO resultVO = new ResultVO();
		resultVO.setResultCode(ResponseCode.SUCCESS.getCode());
		resultVO.setResultMessage(ResponseCode.SUCCESS.getMessage());
		return resultVO;
	}

	/**
	 * 현재 로그인 사용자.
	 *
	 * <p>쿠키가 HttpOnly 라 프론트는 로그인 여부를 스스로 알 수 없다. 앱 시작 시 이 API 를 한 번 호출해
	 * 라우트 가드와 메뉴 분기에 쓴다. 비로그인은 오류가 아니므로 HTTP 200 + {@code resultCode 401} 로 답한다.</p>
	 */
	@Operation(summary = "현재 사용자", description = "인증된 사용자의 ID·이름·권한을 반환한다.",
			tags = {"EgovLoginApiController"})
	@GetMapping("/auth/me")
	public Map<String, Object> me() {
		Map<String, Object> resultMap = new HashMap<>();

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof EgovUserDetails principal)) {
			resultMap.put("resultCode", "401");
			resultMap.put("resultMessage", "not authenticated");
			return resultMap;
		}

		Object user = principal.getEgovUserVO();
		if (!(user instanceof LoginVO loginVO) || loginVO.getId() == null || loginVO.getId().isEmpty()) {
			resultMap.put("resultCode", "401");
			resultMap.put("resultMessage", "not authenticated");
			return resultMap;
		}

		List<String> roles = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());

		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("id", loginVO.getId());
		userInfo.put("name", loginVO.getName() == null ? "" : loginVO.getName());
		userInfo.put("userSe", loginVO.getUserSe() == null ? "" : loginVO.getUserSe());
		userInfo.put("uniqId", loginVO.getUniqId() == null ? "" : loginVO.getUniqId());
		userInfo.put("roles", roles);

		resultMap.put("resultVO", userInfo);
		resultMap.put("resultCode", "200");
		resultMap.put("resultMessage", "성공했습니다.");
		return resultMap;
	}

	/**
	 * 권한 판정.
	 *
	 * <p>서버 렌더링 판과 동일한 규칙을 유지한다 — 데모 데이터에는 권한 매핑 테이블이 채워져 있지 않아
	 * {@code admin} 계정만 관리자로 취급한다. 실제 운영에서는 권한 테이블(TB_AUTHOR_*) 조회로 대체해야 한다.</p>
	 */
	private String resolveRole(LoginVO loginVO) {
		return "admin".equalsIgnoreCase(loginVO.getId()) ? "ROLE_ADMIN" : "ROLE_USER";
	}
}
