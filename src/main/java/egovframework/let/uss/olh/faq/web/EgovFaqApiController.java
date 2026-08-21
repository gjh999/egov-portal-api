package egovframework.let.uss.olh.faq.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.EgovFileMngUtil;
import egovframework.com.cmm.service.FileVO;
import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.com.cmm.util.EgovPaginationUtil;
import egovframework.let.uss.olh.faq.service.EgovFaqManageService;
import egovframework.let.uss.olh.faq.service.FaqManageDefaultVO;
import egovframework.let.uss.olh.faq.service.FaqManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * FAQ API.
 *
 * <p>조회는 공개, 등록·수정·삭제는 로그인이 필요하다(SecurityConfig 의 GET 화이트리스트와 짝을 이룬다).</p>
 */
@RestController
@Tag(name = "EgovFaqApiController", description = "FAQ")
public class EgovFaqApiController {

	@Resource(name = "FaqManageService")
	private EgovFaqManageService faqManageService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileUtil;

	@Operation(summary = "FAQ 목록", tags = {"EgovFaqApiController"})
	@GetMapping("/faq")
	public IntermediateResultVO<Map<String, Object>> list(@ModelAttribute FaqManageDefaultVO searchVO) throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", faqManageService.selectFaqList(searchVO));
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, faqManageService.selectFaqListTotCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	/**
	 * FAQ 상세.
	 *
	 * <p>서버 렌더링 판은 조회수 증가를 별도 URL(FaqInqireCoUpdt.do)에서 처리한 뒤 상세로 forward 했다.
	 * 왕복이 한 번 더 필요할 이유가 없으므로 상세 조회에 합쳤다.</p>
	 */
	@Operation(summary = "FAQ 상세 (조회수 증가)", tags = {"EgovFaqApiController"})
	@GetMapping("/faq/{faqId}")
	public IntermediateResultVO<Map<String, Object>> detail(@PathVariable("faqId") String faqId) throws Exception {
		FaqManageVO param = new FaqManageVO();
		param.setFaqId(faqId);

		// 조회수 증가 — 로그인하지 않은 방문자도 카운트한다
		param.setLastUpdusrId(currentUniqId("_anonymous"));
		faqManageService.updateFaqInqireCo(param);

		FaqManageVO vo = faqManageService.selectFaqListDetail(param);

		Map<String, Object> result = new HashMap<>();
		result.put("result", vo);
		result.put("fileList", attachedFiles(vo == null ? null : vo.getAtchFileId()));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "FAQ 등록", tags = {"EgovFaqApiController"})
	@PostMapping("/faq")
	public IntermediateResultVO<Object> insert(MultipartHttpServletRequest multiRequest,
			@ModelAttribute FaqManageVO faqManageVO) throws Exception {

		String uniqId = currentUniqId(null);
		if (uniqId == null) {
			return authError();
		}

		faqManageVO.setAtchFileId(saveNewFiles(multiRequest, ""));
		faqManageVO.setFrstRegisterId(uniqId);
		faqManageVO.setLastUpdusrId(uniqId);

		faqManageService.insertFaqCn(faqManageVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "FAQ 수정", tags = {"EgovFaqApiController"})
	@PutMapping("/faq/{faqId}")
	public IntermediateResultVO<Object> update(@PathVariable("faqId") String faqId,
			MultipartHttpServletRequest multiRequest,
			@ModelAttribute FaqManageVO faqManageVO) throws Exception {

		String uniqId = currentUniqId(null);
		if (uniqId == null) {
			return authError();
		}

		faqManageVO.setFaqId(faqId);

		// 기존 첨부가 있으면 이어 붙이고, 없으면 새 그룹을 만든다
		String atchFileId = faqManageVO.getAtchFileId();
		if (atchFileId == null || atchFileId.isBlank()) {
			faqManageVO.setAtchFileId(saveNewFiles(multiRequest, ""));
		} else {
			appendFiles(multiRequest, atchFileId);
		}

		faqManageVO.setLastUpdusrId(uniqId);
		faqManageService.updateFaqCn(faqManageVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "FAQ 삭제", tags = {"EgovFaqApiController"})
	@DeleteMapping("/faq/{faqId}")
	public IntermediateResultVO<Object> delete(@PathVariable("faqId") String faqId) throws Exception {
		if (currentUniqId(null) == null) {
			return authError();
		}

		FaqManageVO param = new FaqManageVO();
		param.setFaqId(faqId);

		// 첨부 그룹 ID 는 삭제 전에 읽어 둬야 한다 — 지운 뒤에는 조회할 수 없다
		FaqManageVO stored = faqManageService.selectFaqListDetail(param);
		String atchFileId = (stored == null) ? null : stored.getAtchFileId();

		faqManageService.deleteFaqCn(param);

		if (atchFileId != null && !atchFileId.isBlank()) {
			FileVO fvo = new FileVO();
			fvo.setAtchFileId(atchFileId);
			fileMngService.deleteAllFileInf(fvo);
		}
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	/** 로그인 사용자의 uniqId. 비로그인이면 fallback 을 돌려준다(null 이면 미인증 판정용). */
	private String currentUniqId(String fallback) {
		if (!Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			return fallback;
		}
		Object user = EgovUserDetailsHelper.getAuthenticatedUser();
		return (user instanceof LoginVO loginVO) ? loginVO.getUniqId() : fallback;
	}

	private IntermediateResultVO<Object> authError() {
		IntermediateResultVO<Object> error = new IntermediateResultVO<>();
		error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
		return error;
	}

	/** 첨부파일 목록. 첨부가 없으면 빈 목록. */
	private List<FileVO> attachedFiles(String atchFileId) throws Exception {
		if (atchFileId == null || atchFileId.isBlank()) {
			return new ArrayList<>();
		}
		FileVO fileVO = new FileVO();
		fileVO.setAtchFileId(atchFileId);
		return fileMngService.selectFileInfs(fileVO);
	}

	/** 새 첨부 그룹을 만든다. 실제 파일이 없으면 빈 문자열. */
	private String saveNewFiles(MultipartHttpServletRequest multiRequest, String atchFileId) throws Exception {
		Map<String, MultipartFile> files = multiRequest.getFileMap();
		if (!hasRealFile(files)) {
			return atchFileId;
		}
		List<FileVO> parsed = fileUtil.parseFileInf(files, "FAQ_", 0, atchFileId, "");
		return (parsed == null || parsed.isEmpty()) ? atchFileId : fileMngService.insertFileInfs(parsed);
	}

	/** 기존 첨부 그룹에 파일을 덧붙인다. */
	private void appendFiles(MultipartHttpServletRequest multiRequest, String atchFileId) throws Exception {
		Map<String, MultipartFile> files = multiRequest.getFileMap();
		if (!hasRealFile(files)) {
			return;
		}
		FileVO fvo = new FileVO();
		fvo.setAtchFileId(atchFileId);
		int nextSn = fileMngService.getMaxFileSN(fvo);
		List<FileVO> parsed = fileUtil.parseFileInf(files, "FAQ_", nextSn, atchFileId, "");
		if (parsed != null && !parsed.isEmpty()) {
			fileMngService.updateFileInfs(parsed);
		}
	}

	/**
	 * 실제로 내용이 있는 파일이 하나라도 있는지 확인한다.
	 * 브라우저는 파일을 고르지 않아도 빈 파트를 보내는 경우가 있어, 그대로 저장하면 0바이트 첨부가 생긴다.
	 */
	private boolean hasRealFile(Map<String, MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return false;
		}
		return files.values().stream().anyMatch(file ->
				file != null && !file.isEmpty()
						&& file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty());
	}
}
