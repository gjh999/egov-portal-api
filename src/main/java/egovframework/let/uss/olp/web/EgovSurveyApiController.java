package egovframework.let.uss.olp.web;

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
import org.springframework.web.bind.annotation.RestController;

import egovframework.com.cmm.ComDefaultVO;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.com.cmm.util.EgovPaginationUtil;
import egovframework.let.uss.olp.qim.service.EgovQustnrItemManageService;
import egovframework.let.uss.olp.qim.service.QustnrItemManageVO;
import egovframework.let.uss.olp.qmc.service.EgovQustnrManageService;
import egovframework.let.uss.olp.qmc.service.QustnrManageVO;
import egovframework.let.uss.olp.qqm.service.EgovQustnrQestnManageService;
import egovframework.let.uss.olp.qqm.service.QustnrQestnManageVO;
import egovframework.let.uss.olp.qri.service.EgovQustnrRespondInfoService;
import egovframework.let.uss.olp.qri.service.QustnrRespondInfoVO;
import egovframework.let.uss.olp.qrm.service.EgovQustnrRespondManageService;
import egovframework.let.uss.olp.qrm.service.QustnrRespondManageVO;
import egovframework.let.uss.olp.qtm.service.EgovQustnrTmplatManageService;
import egovframework.let.uss.olp.qtm.service.QustnrTmplatManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 설문 API.
 *
 * <p>설문은 다섯 계층으로 이루어진다. 화면이 계층을 오가야 해서 컨트롤러를 하나로 모았다.</p>
 * <ol>
 *   <li><b>템플릿</b>(qtm) — 설문지의 겉모양</li>
 *   <li><b>설문</b>(qmc) — 제목·기간·대상</li>
 *   <li><b>문항</b>(qqm) — 설문에 속한 질문</li>
 *   <li><b>항목</b>(qim) — 객관식 보기</li>
 *   <li><b>응답</b>(qrm/qri) — 참여자가 낸 답</li>
 * </ol>
 *
 * <p>관리(1~4)와 응답 결과 열람은 관리자 전용이고, <b>설문 참여</b>는 로그인 사용자면 가능하다.</p>
 */
@RestController
@Tag(name = "EgovSurveyApiController", description = "설문")
public class EgovSurveyApiController {

	@Resource(name = "egovQustnrTmplatManageService")
	private EgovQustnrTmplatManageService tmplatService;

	@Resource(name = "egovQustnrManageService")
	private EgovQustnrManageService qustnrService;

	@Resource(name = "egovQustnrQestnManageService")
	private EgovQustnrQestnManageService qestnService;

	@Resource(name = "egovQustnrItemManageService")
	private EgovQustnrItemManageService itemService;

	@Resource(name = "egovQustnrRespondManageService")
	private EgovQustnrRespondManageService respondService;

	@Resource(name = "egovQustnrRespondInfoService")
	private EgovQustnrRespondInfoService respondInfoService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------------ 설문(qmc)

	@Operation(summary = "설문 목록", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys")
	public IntermediateResultVO<Map<String, Object>> surveyList(@ModelAttribute ComDefaultVO searchVO) throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", qustnrService.selectQustnrManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, qustnrService.selectQustnrManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "설문 상세", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/{qestnrId}")
	public IntermediateResultVO<QustnrManageVO> surveyDetail(@PathVariable("qestnrId") String qestnrId)
			throws Exception {
		QustnrManageVO param = new QustnrManageVO();
		param.setQestnrId(qestnrId);
		return IntermediateResultVO.success(qustnrService.selectQustnrManageDetailModel(param));
	}

	@Operation(summary = "설문 등록", tags = {"EgovSurveyApiController"})
	@PostMapping("/surveys")
	public IntermediateResultVO<Object> insertSurvey(@RequestBody QustnrManageVO vo) throws Exception {
		qustnrService.insertQustnrManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 수정", tags = {"EgovSurveyApiController"})
	@PutMapping("/surveys/{qestnrId}")
	public IntermediateResultVO<Object> updateSurvey(@PathVariable("qestnrId") String qestnrId,
			@RequestBody QustnrManageVO vo) throws Exception {
		vo.setQestnrId(qestnrId);
		qustnrService.updateQustnrManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 삭제", tags = {"EgovSurveyApiController"})
	@DeleteMapping("/surveys/{qestnrId}")
	public IntermediateResultVO<Object> deleteSurvey(@PathVariable("qestnrId") String qestnrId) throws Exception {
		QustnrManageVO param = new QustnrManageVO();
		param.setQestnrId(qestnrId);
		qustnrService.deleteQustnrManage(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 템플릿(qtm)

	@Operation(summary = "설문 템플릿 목록", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/templates")
	public IntermediateResultVO<Map<String, Object>> templateList(@ModelAttribute ComDefaultVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", tmplatService.selectQustnrTmplatManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, tmplatService.selectQustnrTmplatManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "설문 템플릿 등록", tags = {"EgovSurveyApiController"})
	@PostMapping("/surveys/templates")
	public IntermediateResultVO<Object> insertTemplate(@RequestBody QustnrTmplatManageVO vo) throws Exception {
		tmplatService.insertQustnrTmplatManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 템플릿 수정", tags = {"EgovSurveyApiController"})
	@PutMapping("/surveys/templates/{qestnrTmplatId}")
	public IntermediateResultVO<Object> updateTemplate(@PathVariable("qestnrTmplatId") String qestnrTmplatId,
			@RequestBody QustnrTmplatManageVO vo) throws Exception {
		vo.setQestnrTmplatId(qestnrTmplatId);
		tmplatService.updateQustnrTmplatManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 템플릿 삭제", tags = {"EgovSurveyApiController"})
	@DeleteMapping("/surveys/templates/{qestnrTmplatId}")
	public IntermediateResultVO<Object> deleteTemplate(@PathVariable("qestnrTmplatId") String qestnrTmplatId)
			throws Exception {
		QustnrTmplatManageVO param = new QustnrTmplatManageVO();
		param.setQestnrTmplatId(qestnrTmplatId);
		tmplatService.deleteQustnrTmplatManage(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 문항(qqm)

	@Operation(summary = "설문 문항 목록", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/questions")
	public IntermediateResultVO<Map<String, Object>> questionList(@ModelAttribute ComDefaultVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", qestnService.selectQustnrQestnManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, qestnService.selectQustnrQestnManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "설문 문항 등록", tags = {"EgovSurveyApiController"})
	@PostMapping("/surveys/questions")
	public IntermediateResultVO<Object> insertQuestion(@RequestBody QustnrQestnManageVO vo) throws Exception {
		qestnService.insertQustnrQestnManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 문항 수정", tags = {"EgovSurveyApiController"})
	@PutMapping("/surveys/questions/{qestnrQesitmId}")
	public IntermediateResultVO<Object> updateQuestion(@PathVariable("qestnrQesitmId") String qestnrQesitmId,
			@RequestBody QustnrQestnManageVO vo) throws Exception {
		vo.setQestnrQesitmId(qestnrQesitmId);
		qestnService.updateQustnrQestnManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 문항 삭제", tags = {"EgovSurveyApiController"})
	@DeleteMapping("/surveys/questions/{qestnrQesitmId}")
	public IntermediateResultVO<Object> deleteQuestion(@PathVariable("qestnrQesitmId") String qestnrQesitmId)
			throws Exception {
		QustnrQestnManageVO param = new QustnrQestnManageVO();
		param.setQestnrQesitmId(qestnrQesitmId);
		qestnService.deleteQustnrQestnManage(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 항목(qim)

	@Operation(summary = "설문 항목 목록", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/items")
	public IntermediateResultVO<Map<String, Object>> itemList(@ModelAttribute ComDefaultVO searchVO) throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", itemService.selectQustnrItemManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, itemService.selectQustnrItemManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "설문 항목 등록", tags = {"EgovSurveyApiController"})
	@PostMapping("/surveys/items")
	public IntermediateResultVO<Object> insertItem(@RequestBody QustnrItemManageVO vo) throws Exception {
		itemService.insertQustnrItemManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 항목 수정", tags = {"EgovSurveyApiController"})
	@PutMapping("/surveys/items/{qustnrIemId}")
	public IntermediateResultVO<Object> updateItem(@PathVariable("qustnrIemId") String qustnrIemId,
			@RequestBody QustnrItemManageVO vo) throws Exception {
		vo.setQustnrIemId(qustnrIemId);
		itemService.updateQustnrItemManage(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "설문 항목 삭제", tags = {"EgovSurveyApiController"})
	@DeleteMapping("/surveys/items/{qustnrIemId}")
	public IntermediateResultVO<Object> deleteItem(@PathVariable("qustnrIemId") String qustnrIemId) throws Exception {
		QustnrItemManageVO param = new QustnrItemManageVO();
		param.setQustnrIemId(qustnrIemId);
		itemService.deleteQustnrItemManage(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------- 응답(qrm/qri)

	@Operation(summary = "설문 응답 결과 목록 (관리자)", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/responses")
	public IntermediateResultVO<Map<String, Object>> responseList(@ModelAttribute ComDefaultVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", respondService.selectQustnrRespondManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, respondService.selectQustnrRespondManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "설문 응답 상세 목록 (관리자)", tags = {"EgovSurveyApiController"})
	@GetMapping("/surveys/response-details")
	public IntermediateResultVO<Map<String, Object>> responseInfoList(@ModelAttribute ComDefaultVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(searchVO);

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", respondInfoService.selectQustnrRespondInfoManageList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, respondInfoService.selectQustnrRespondInfoManageListCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	/**
	 * 설문 참여(응답 제출).
	 *
	 * <p>관리 API 와 달리 <b>로그인 사용자면 누구나</b> 호출할 수 있다.
	 * 중복 참여를 막기 위해 이미 응답한 이력이 있으면 거절한다.</p>
	 */
	@Operation(summary = "설문 참여(응답 제출)", tags = {"EgovSurveyApiController"})
	@PostMapping("/survey-responses")
	public IntermediateResultVO<Object> submitResponse(@RequestBody QustnrRespondInfoVO vo) throws Exception {
		LoginVO user = currentUser();
		if (user == null) {
			IntermediateResultVO<Object> error = new IntermediateResultVO<>();
			error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
			error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
			return error;
		}

		vo.setFrstRegisterId(user.getUniqId());
		respondInfoService.insertQustnrRespondInfo(vo);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	private PaginationInfo preparePaging(ComDefaultVO searchVO) {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
		return paginationInfo;
	}

	private LoginVO currentUser() {
		if (!Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			return null;
		}
		Object user = EgovUserDetailsHelper.getAuthenticatedUser();
		return (user instanceof LoginVO loginVO) ? loginVO : null;
	}
}
