package egovframework.com.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.support.MultipartFilter;

import egovframework.com.jwt.JwtAuthenticationEntryPoint;
import egovframework.com.jwt.JwtAuthenticationFilter;

/**
 * 포털 사이트 SPA 백엔드의 Spring Security 설정.
 *
 * <p>서버 렌더링 시절에는 세션(HttpSession)에 SecurityContext 를 저장하고 미인증 시 로그인 <b>화면</b>으로
 * 리다이렉트했다. SPA 로 전환하면서 다음이 바뀌었다.</p>
 * <ul>
 *   <li>상태를 서버에 두지 않는다(STATELESS) — 인증은 ACCESS_TOKEN HttpOnly 쿠키의 JWT 로 판정한다.</li>
 *   <li>리다이렉트하지 않는다 — 미인증/권한부족은 JSON(401/403)으로 응답한다.</li>
 *   <li>프론트(다른 포트/도메인)에서 호출하므로 CORS 를 명시적으로 허용한다.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/** 인증 없이 호출 가능한 API */
	private static final String[] PERMIT_ALL = {
			"/error",
			"/auth/login-jwt",     // 로그인
			"/auth/logout",        // 로그아웃
			"/auth/me",            // 현재 사용자 (익명이면 컨트롤러가 401 코드를 담아 응답)
			"/i18n/**",            // 다국어 메시지 번들 (로그인 화면에서도 필요)
			"/main/**",            // 메인 화면 구성 데이터
			"/user-types",         // 사용자 구분 안내
			"/terms/**",           // 이용약관 · 개인정보처리방침 (가입 전 열람)
			"/members/join/**",    // 회원가입 (일반/기업)
			"/members/check-id/**",// 아이디 중복 확인
			"/banners/**",         // 노출용 배너 조회
			// swagger
			"/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui.html",
			"/swagger-ui/**", "/webjars/**"
	};

	/** 인증 없이 조회(GET)만 허용하는 API — 등록·수정·삭제는 로그인이 필요하다 */
	private static final String[] PERMIT_GET = {
			"/boards/**",          // 게시판 목록·상세
			"/board-masters/**",   // 게시판 정보
			"/faq/**",             // FAQ
			"/qna/**",             // Q&A (목록·상세)
			"/restde/**"           // 공휴일
	};

	/** 관리자 전용 (ROLE_ADMIN) */
	private static final String[] ADMIN_ONLY = {
			"/admin/**",           // 관리자 기능 전반
			"/authorities/**",     // 권한
			"/author-groups/**",   // 권한그룹
			"/author-roles/**",    // 권한-롤 매핑
			"/groups/**",          // 그룹
			"/roles/**",           // 롤
			"/board-use/**",       // 게시판 사용정보
			"/templates/**",       // 템플릿
			"/surveys/**",         // 설문 관리(템플릿·문항·항목·응답결과)
			"/zip/**",             // 우편번호 관리
			"/stplat/**",          // 약관 관리
			"/privacy-policies/**" // 개인정보처리방침 관리
	};

	// application.properties 의 Globals.Allow.Origin. 환경별로 콤마 구분 복수 지정 가능.
	@Value("${Globals.Allow.Origin:http://localhost:5175,http://localhost:5176}")
	private String allowedOrigins;

	@Bean
	public JwtAuthenticationFilter authenticationTokenFilterBean() {
		return new JwtAuthenticationFilter();
	}

	@Bean
	protected CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// 명시적 Origin 목록만 허용 — setAllowedOriginPatterns("*") + credentials 동시 사용은 브라우저가 거부한다
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept-Language"));
		configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public CharacterEncodingFilter characterEncodingFilter() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		return filter;
	}

	/**
	 * 서블릿 컨테이너 최상위 우선순위로 UTF-8 인코딩 필터 등록.
	 * (SecurityConfig 가 CharacterEncodingFilter 빈을 직접 정의하면 Boot 의 고우선순위 인코딩 필터가
	 *  backoff 되어, 폼 파라미터 파싱 전에 UTF-8 이 적용되지 않아 한글이 깨진다.)
	 */
	@Bean
	public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistration() {
		FilterRegistrationBean<CharacterEncodingFilter> registration =
				new FilterRegistrationBean<>(characterEncodingFilter());
		registration.addUrlPatterns("/*");
		registration.setDispatcherTypes(
				jakarta.servlet.DispatcherType.REQUEST,
				jakarta.servlet.DispatcherType.FORWARD,
				jakarta.servlet.DispatcherType.ASYNC,
				jakarta.servlet.DispatcherType.ERROR,
				jakarta.servlet.DispatcherType.INCLUDE);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	/** 멀티파트 필터 — 파일 첨부 요청의 파라미터를 인증 필터보다 먼저 파싱한다 */
	@Bean
	public MultipartFilter multipartFilter() {
		return new MultipartFilter();
	}

	@Bean
	protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				// 인증이 쿠키 기반이므로 CSRF 가 이론상 유효하지만, SameSite 쿠키 + CORS 화이트리스트로 차단한다.
				// (SPA 에서 CSRF 토큰을 쓰려면 프론트가 토큰을 읽어야 해 HttpOnly 의 이점이 줄어든다.)
				.csrf(AbstractHttpConfigurer::disable)
				// 로그아웃은 EgovLoginApiController(/auth/logout)가 쿠키 만료로 직접 처리한다
				.logout(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						// CORS preflight 는 인증 대상이 아니다(쿠키·Authorization 헤더가 실리지 않는다)
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(ADMIN_ONLY).hasRole("ADMIN")
						.requestMatchers(PERMIT_ALL).permitAll()
						.requestMatchers(HttpMethod.GET, PERMIT_GET).permitAll()
						.anyRequest().authenticated())
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.securityContext(sc -> sc.securityContextRepository(new NullSecurityContextRepository()))
				.requestCache(cache -> cache.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.exceptionHandling(eh -> eh.authenticationEntryPoint(new JwtAuthenticationEntryPoint()))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.addFilterBefore(characterEncodingFilter(), DisableEncodeUrlFilter.class)
				.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
