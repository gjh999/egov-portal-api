package egovframework.let.uss.olh.qna.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.fdl.security.userdetails.util.EgovUserDetailsHelper;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.com.cmm.util.EgovPaginationUtil;
import egovframework.let.uss.olh.qna.service.EgovQnaManageService;
import egovframework.let.uss.olh.qna.service.QnaManageDefaultVO;
import egovframework.let.uss.olh.qna.service.QnaManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * Q&A(질문·답변) API.
 *
 * <p>Q&A 는 <b>비회원도 글을 남길 수 있고</b>, 본인 확인은 글마다 지정한 작성비밀번호로 한다.
 * 그래서 상세 열람 전에 비밀번호 확인 단계를 거친다(`POST /qna/{qaId}/verify`).
 * 답변 등록은 관리자만 가능하다.</p>
 */
@RestController
@Tag(name = "EgovQnaApiController", description = "Q&A")
public class EgovQnaApiController {

	@Resource(name = "QnaManageService")
	private EgovQnaManageService qnaManageService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Operation(summary = "Q&A 목록", tags = {"EgovQnaApiController"})
	@GetMapping("/qna")
	public IntermediateResultVO<Map<String, Object>> list(@ModelAttribute QnaManageDefaultVO searchVO) throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", qnaManageService.selectQnaList(searchVO));
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, qnaManageService.selectQnaListTotCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	/**
	 * 작성비밀번호 확인.
	 *
	 * <p>상세를 보기 전에 호출한다. 맞으면 {@code true} 를 돌려주고, 프론트는 그때 상세를 요청한다.
	 * 비밀번호 자체를 상세 API 로 넘기지 않는 이유는, 조회 URL 에 비밀번호가 남지 않게 하기 위해서다.</p>
	 */
	@Operation(summary = "Q&A 작성비밀번호 확인", tags = {"EgovQnaApiController"})
	@PostMapping("/qna/{qaId}/verify")
	public IntermediateResultVO<Map<String, Object>> verifyPassword(@PathVariable("qaId") String qaId,
			@RequestBody Map<String, String> body) throws Exception {

		QnaManageVO param = new QnaManageVO();
		param.setQaId(qaId);
		param.setWritngPassword(body.get("writngPassword"));

		boolean matched = qnaManageService.selectQnaPasswordConfirmCnt(param) > 0;

		Map<String, Object> result = new HashMap<>();
		result.put("matched", matched);
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "Q&A 상세 (조회수 증가)", tags = {"EgovQnaApiController"})
	@GetMapping("/qna/{qaId}")
	public IntermediateResultVO<Map<String, Object>> detail(@PathVariable("qaId") String qaId) throws Exception {
		QnaManageVO param = new QnaManageVO();
		param.setQaId(qaId);
		param.setLastUpdusrId(currentUniqId("_anonymous"));
		qnaManageService.updateQnaInqireCo(param);

		Map<String, Object> result = new HashMap<>();
		result.put("result", qnaManageService.selectQnaListDetail(param));
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "Q&A 등록", description = "비회원도 등록할 수 있다(작성비밀번호 필수).",
			tags = {"EgovQnaApiController"})
	@PostMapping("/qna")
	public IntermediateResultVO<Object> insert(@RequestBody QnaManageVO qnaManageVO) throws Exception {
		String uniqId = currentUniqId("_anonymous");
		qnaManageVO.setFrstRegisterId(uniqId);
		qnaManageVO.setLastUpdusrId(uniqId);

		qnaManageService.insertQnaCn(qnaManageVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "Q&A 수정", tags = {"EgovQnaApiController"})
	@PutMapping("/qna/{qaId}")
	public IntermediateResultVO<Object> update(@PathVariable("qaId") String qaId,
			@RequestBody QnaManageVO qnaManageVO) throws Exception {

		qnaManageVO.setQaId(qaId);

		// 작성비밀번호가 맞아야 수정할 수 있다 — 비회원 글의 유일한 본인 확인 수단이다
		if (!isOwnerOrAdmin(qnaManageVO)) {
			return authError();
		}

		qnaManageVO.setLastUpdusrId(currentUniqId("_anonymous"));
		qnaManageService.updateQnaCn(qnaManageVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "Q&A 삭제", tags = {"EgovQnaApiController"})
	@DeleteMapping("/qna/{qaId}")
	public IntermediateResultVO<Object> delete(@PathVariable("qaId") String qaId,
			@RequestParam(value = "writngPassword", required = false) String writngPassword) throws Exception {

		QnaManageVO param = new QnaManageVO();
		param.setQaId(qaId);
		param.setWritngPassword(writngPassword);

		if (!isOwnerOrAdmin(param)) {
			return authError();
		}

		qnaManageService.deleteQnaCn(param);
		return IntermediateResultVO.success(null);
	}

	// ---------------------------------------------------------------- 관리자 답변

	@Operation(summary = "Q&A 답변 상세 (관리자)", tags = {"EgovQnaApiController"})
	@GetMapping("/admin/qna/{qaId}")
	public IntermediateResultVO<Map<String, Object>> answerDetail(@PathVariable("qaId") String qaId) throws Exception {
		QnaManageVO param = new QnaManageVO();
		param.setQaId(qaId);

		Map<String, Object> result = new HashMap<>();
		result.put("result", qnaManageService.selectQnaAnswerListDetail(param));
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "Q&A 답변 등록·수정 (관리자)", tags = {"EgovQnaApiController"})
	@PutMapping("/admin/qna/{qaId}/answer")
	public IntermediateResultVO<Object> answer(@PathVariable("qaId") String qaId,
			@RequestBody QnaManageVO qnaManageVO) throws Exception {

		qnaManageVO.setQaId(qaId);
		qnaManageVO.setLastUpdusrId(currentUniqId("_anonymous"));

		qnaManageService.updateQnaCnAnswer(qnaManageVO);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	private String currentUniqId(String fallback) {
		if (!Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			return fallback;
		}
		Object user = EgovUserDetailsHelper.getAuthenticatedUser();
		return (user instanceof LoginVO loginVO) ? loginVO.getUniqId() : fallback;
	}

	/** 관리자이거나, 작성비밀번호가 일치하면 본인으로 인정한다. */
	private boolean isOwnerOrAdmin(QnaManageVO vo) throws Exception {
		if (Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())
				&& EgovUserDetailsHelper.getAuthorities() != null
				&& EgovUserDetailsHelper.getAuthorities().contains("ROLE_ADMIN")) {
			return true;
		}
		if (vo.getWritngPassword() == null || vo.getWritngPassword().isBlank()) {
			return false;
		}
		return qnaManageService.selectQnaPasswordConfirmCnt(vo) > 0;
	}

	private IntermediateResultVO<Object> authError() {
		IntermediateResultVO<Object> error = new IntermediateResultVO<>();
		error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
		return error;
	}
}
