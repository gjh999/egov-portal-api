package egovframework.com.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.ResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 미인증 요청에 대한 응답.
 *
 * <p>이 백엔드에는 로그인 화면이 없다(SPA 프론트가 담당) — Accept 헤더와 무관하게 항상 401 JSON 을 반환한다.
 * 프론트의 응답 인터셉터가 401 을 받아 로그인 화면으로 라우팅한다.</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		ResultVO resultVO = new ResultVO();
		resultVO.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		resultVO.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(mapper.writeValueAsString(resultVO));
	}
}
