package egovframework.let.uss.sam.terms.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.com.cmm.util.EgovPaginationUtil;
import egovframework.let.uss.sam.ipm.service.EgovIndvdlInfoPolicyService;
import egovframework.let.uss.sam.ipm.service.IndvdlInfoPolicy;
import egovframework.let.uss.sam.stp.service.EgovStplatManageService;
import egovframework.let.uss.sam.stp.service.StplatManageDefaultVO;
import egovframework.let.uss.sam.stp.service.StplatManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 약관 · 개인정보처리방침 API.
 *
 * <p>가입 화면에서 동의를 받아야 하므로 <b>대표 약관 조회는 공개</b>다.
 * 등록·수정·삭제와 대표 지정은 관리자 전용이다.</p>
 *
 * <p>"대표"는 여러 버전 중 현재 노출할 하나를 가리킨다. 새 버전을 만들어도 대표로 지정하기 전까지는
 * 사용자에게 보이지 않으므로, 개정 작업을 미리 해 둘 수 있다.</p>
 */
@RestController
@Tag(name = "EgovTermsApiController", description = "약관 · 개인정보처리방침")
public class EgovTermsApiController {

	@Resource(name = "StplatManageService")
	private EgovStplatManageService stplatManageService;

	@Resource(name = "egovIndvdlInfoPolicyService")
	private EgovIndvdlInfoPolicyService indvdlInfoPolicyService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------------ 공개 조회

	@Operation(summary = "현재 노출 중인 이용약관", tags = {"EgovTermsApiController"})
	@GetMapping("/terms/stplat")
	public IntermediateResultVO<StplatManageVO> representStplat() throws Exception {
		return IntermediateResultVO.success(stplatManageService.selectRepresentStplat());
	}

	@Operation(summary = "현재 노출 중인 개인정보처리방침", tags = {"EgovTermsApiController"})
	@GetMapping("/terms/privacy")
	public IntermediateResultVO<IndvdlInfoPolicy> representPrivacy() throws Exception {
		return IntermediateResultVO.success(indvdlInfoPolicyService.selectRepresentIndvdlInfoPolicy());
	}

	// ------------------------------------------------------------------ 약관 관리

	@Operation(summary = "약관 목록 (관리자)", tags = {"EgovTermsApiController"})
	@GetMapping("/stplat")
	public IntermediateResultVO<Map<String, Object>> stplatList(@ModelAttribute StplatManageDefaultVO searchVO)
			throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", stplatManageService.selectStplatList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, stplatManageService.selectStplatListTotCnt(searchVO)));
		result.put("activeCnt", stplatManageService.selectActiveStplatCnt());

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "약관 상세 (관리자)", tags = {"EgovTermsApiController"})
	@GetMapping("/stplat/{useStplatId}")
	public IntermediateResultVO<StplatManageVO> stplatDetail(@PathVariable("useStplatId") String useStplatId)
			throws Exception {
		StplatManageVO param = new StplatManageVO();
		param.setUseStplatId(useStplatId);
		return IntermediateResultVO.success(stplatManageService.selectStplatDetail(param));
	}

	@Operation(summary = "약관 등록 (관리자)", tags = {"EgovTermsApiController"})
	@PostMapping("/stplat")
	public IntermediateResultVO<Object> insertStplat(@RequestBody StplatManageVO vo) throws Exception {
		stplatManageService.insertStplatCn(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "약관 수정 (관리자)", tags = {"EgovTermsApiController"})
	@PutMapping("/stplat/{useStplatId}")
	public IntermediateResultVO<Object> updateStplat(@PathVariable("useStplatId") String useStplatId,
			@RequestBody StplatManageVO vo) throws Exception {
		vo.setUseStplatId(useStplatId);
		stplatManageService.updateStplatCn(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "약관 삭제 (관리자)", tags = {"EgovTermsApiController"})
	@DeleteMapping("/stplat/{useStplatId}")
	public IntermediateResultVO<Object> deleteStplat(@PathVariable("useStplatId") String useStplatId) throws Exception {
		StplatManageVO param = new StplatManageVO();
		param.setUseStplatId(useStplatId);
		stplatManageService.deleteStplatCn(param);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "약관 대표 지정 (관리자)", description = "지정한 약관 하나만 사용자에게 노출된다.",
			tags = {"EgovTermsApiController"})
	@PutMapping("/stplat/{useStplatId}/represent")
	public IntermediateResultVO<Object> setRepresentStplat(@PathVariable("useStplatId") String useStplatId) {
		stplatManageService.setRepresentStplat(useStplatId);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "약관 사용여부 변경 (관리자)", tags = {"EgovTermsApiController"})
	@PutMapping("/stplat/{useStplatId}/use-at")
	public IntermediateResultVO<Object> updateStplatUseAt(@PathVariable("useStplatId") String useStplatId,
			@RequestBody Map<String, String> body) {
		stplatManageService.updateUseAtStplat(useStplatId, body.getOrDefault("useAt", "N"));
		return IntermediateResultVO.success(null);
	}

	// -------------------------------------------------------- 개인정보처리방침 관리

	@Operation(summary = "개인정보처리방침 목록 (관리자)", tags = {"EgovTermsApiController"})
	@GetMapping("/privacy-policies")
	public IntermediateResultVO<Map<String, Object>> privacyList(
			@ModelAttribute egovframework.com.cmm.ComDefaultVO searchVO) throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", indvdlInfoPolicyService.selectIndvdlInfoPolicyList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, indvdlInfoPolicyService.selectIndvdlInfoPolicyListCnt(searchVO)));
		result.put("activeCnt", indvdlInfoPolicyService.selectActiveIndvdlInfoPolicyCnt());

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "개인정보처리방침 상세 (관리자)", tags = {"EgovTermsApiController"})
	@GetMapping("/privacy-policies/{indvdlInfoId}")
	public IntermediateResultVO<IndvdlInfoPolicy> privacyDetail(@PathVariable("indvdlInfoId") String indvdlInfoId)
			throws Exception {
		IndvdlInfoPolicy param = new IndvdlInfoPolicy();
		param.setIndvdlInfoId(indvdlInfoId);
		return IntermediateResultVO.success(indvdlInfoPolicyService.selectIndvdlInfoPolicyDetail(param));
	}

	@Operation(summary = "개인정보처리방침 등록 (관리자)", tags = {"EgovTermsApiController"})
	@PostMapping("/privacy-policies")
	public IntermediateResultVO<Object> insertPrivacy(@RequestBody IndvdlInfoPolicy policy) throws Exception {
		indvdlInfoPolicyService.insertIndvdlInfoPolicy(policy);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "개인정보처리방침 수정 (관리자)", tags = {"EgovTermsApiController"})
	@PutMapping("/privacy-policies/{indvdlInfoId}")
	public IntermediateResultVO<Object> updatePrivacy(@PathVariable("indvdlInfoId") String indvdlInfoId,
			@RequestBody IndvdlInfoPolicy policy) throws Exception {
		policy.setIndvdlInfoId(indvdlInfoId);
		indvdlInfoPolicyService.updateIndvdlInfoPolicy(policy);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "개인정보처리방침 삭제 (관리자)", tags = {"EgovTermsApiController"})
	@DeleteMapping("/privacy-policies/{indvdlInfoId}")
	public IntermediateResultVO<Object> deletePrivacy(@PathVariable("indvdlInfoId") String indvdlInfoId)
			throws Exception {
		IndvdlInfoPolicy param = new IndvdlInfoPolicy();
		param.setIndvdlInfoId(indvdlInfoId);
		indvdlInfoPolicyService.deleteIndvdlInfoPolicy(param);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "개인정보처리방침 대표 지정 (관리자)", tags = {"EgovTermsApiController"})
	@PutMapping("/privacy-policies/{indvdlInfoId}/represent")
	public IntermediateResultVO<Object> setRepresentPrivacy(@PathVariable("indvdlInfoId") String indvdlInfoId) {
		indvdlInfoPolicyService.setRepresentIndvdlInfoPolicy(indvdlInfoId);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "개인정보처리방침 사용여부 변경 (관리자)", tags = {"EgovTermsApiController"})
	@PutMapping("/privacy-policies/{indvdlInfoId}/use-at")
	public IntermediateResultVO<Object> updatePrivacyUseAt(@PathVariable("indvdlInfoId") String indvdlInfoId,
			@RequestBody Map<String, String> body) {
		indvdlInfoPolicyService.updateUseAtIndvdlInfoPolicy(indvdlInfoId, body.getOrDefault("useAt", "N"));
		return IntermediateResultVO.success(null);
	}
}
