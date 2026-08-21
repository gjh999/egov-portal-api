package egovframework.com.jwt;

import java.io.IOException;
import java.util.List;

import org.egovframe.rte.fdl.security.userdetails.EgovUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import egovframework.com.cmm.LoginVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 JWT 를 확인해 SecurityContext 를 채우는 필터.
 *
 * <p><b>중요:</b> principal 로 {@link EgovUserDetails} 를 넣는다. eGovFrame RTE 의
 * {@code EgovUserDetailsHelper.getAuthenticatedUser()} 가 이 타입을 전제로 로그인 사용자를 꺼내기 때문이다.
 * 다른 타입을 넣으면 컨트롤러·서비스 전반에서 로그인 사용자가 {@code null} 로 보여 조용히 망가진다.</p>
 *
 * <p>토큰은 (1) ACCESS_TOKEN 쿠키, (2) Authorization 헤더 순으로 찾는다.
 * 브라우저 SPA 는 쿠키를 쓰고, Swagger·외부 클라이언트는 헤더를 쓸 수 있다.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER_STRING = "Authorization";
	public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

	@Autowired
	private EgovJwtTokenUtil jwtTokenUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		String jwtToken = resolveToken(req);

		if (jwtToken == null || jwtToken.isBlank()) {
			// 비로그인 요청도 그대로 통과시킨다 — 공개 API 가 있고, 보호가 필요한 경로는
			// SecurityConfig 의 인가 규칙이 막는다.
			chain.doFilter(req, res);
			return;
		}

		try {
			LoginVO loginVO = jwtTokenUtil.getLoginVOFromToken(jwtToken);
			String role = jwtTokenUtil.getRoleFromToken(jwtToken);

			List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
			EgovUserDetails principal = new EgovUserDetails(loginVO.getId(), "", true, loginVO);

			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, "", authorities);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (InvalidJwtException e) {
			// 토큰은 있으나 위조·만료.
			// 이 백엔드에는 로그인 화면이 없다(SPA 프론트가 담당) — 리다이렉트하지 않고 401 JSON 을 반환한다.
			SecurityContextHolder.clearContext();
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setContentType("application/json");
			res.setCharacterEncoding("UTF-8");
			res.getWriter().write("{\"resultCode\":401,\"resultMessage\":\"invalid or expired token\"}");
			return;
		}

		chain.doFilter(req, res);
	}

	private String resolveToken(HttpServletRequest req) {
		if (req.getCookies() != null) {
			for (Cookie cookie : req.getCookies()) {
				if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		String header = req.getHeader(HEADER_STRING);
		if (header != null && !header.isBlank()) {
			return header.startsWith("Bearer ") ? header.substring(7) : header;
		}
		return null;
	}
}
