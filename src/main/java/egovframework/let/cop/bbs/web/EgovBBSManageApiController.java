package egovframework.let.cop.bbs.web;

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
import egovframework.let.cop.bbs.service.BoardMaster;
import egovframework.let.cop.bbs.service.BoardMasterVO;
import egovframework.let.cop.bbs.service.BoardVO;
import egovframework.let.cop.bbs.service.EgovBBSAttributeManageService;
import egovframework.let.cop.bbs.service.EgovBBSManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 게시물 API.
 *
 * <p>목록·상세는 공개, 등록·수정·삭제는 로그인이 필요하다. 수정·삭제는 작성자 본인 또는 관리자만 가능하며,
 * <b>서버가 소유권을 다시 확인한다</b> — 프론트의 버튼 노출 제어는 UX 일 뿐 보안 장치가 아니다.</p>
 */
@RestController
@Tag(name = "EgovBBSManageApiController", description = "게시물")
public class EgovBBSManageApiController {

	@Resource(name = "EgovBBSManageService")
	private EgovBBSManageService bbsMngService;

	@Resource(name = "EgovBBSAttributeManageService")
	private EgovBBSAttributeManageService bbsAttrbService;

	@Resource(name = "EgovFileMngService")
	private EgovFileMngService fileMngService;

	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileUtil;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Operation(summary = "게시물 목록", tags = {"EgovBBSManageApiController"})
	@GetMapping("/boards/{bbsId}/articles")
	public IntermediateResultVO<Map<String, Object>> list(@PathVariable("bbsId") String bbsId,
			@ModelAttribute BoardVO boardVO) throws Exception {

		boardVO.setBbsId(bbsId);
		boardVO.setPageUnit(propertiesService.getInt("pageUnit"));
		boardVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				boardVO.getPageIndex(), boardVO.getPageUnit(), boardVO.getPageSize());
		boardVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		boardVO.setLastIndex(paginationInfo.getLastRecordIndex());
		boardVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> serviceResult = bbsMngService.selectBoardArticles(boardVO, "BBSA02");

		Map<String, Object> result = new HashMap<>(serviceResult);
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, toInt(serviceResult.get("resultCnt"))));
		result.put("brdMstrVO", boardMaster(bbsId));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "게시물 상세 (조회수 증가)", tags = {"EgovBBSManageApiController"})
	@GetMapping("/boards/{bbsId}/articles/{nttId}")
	public IntermediateResultVO<Map<String, Object>> detail(@PathVariable("bbsId") String bbsId,
			@PathVariable("nttId") long nttId) throws Exception {

		BoardVO param = new BoardVO();
		param.setBbsId(bbsId);
		param.setNttId(nttId);
		param.setPlusCount(true); // 상세 조회 시 조회수 증가

		BoardVO article = bbsMngService.selectBoardArticle(param);

		Map<String, Object> result = new HashMap<>();
		result.put("boardVO", article);
		result.put("brdMstrVO", boardMaster(bbsId));
		result.put("fileList", attachedFiles(article == null ? null : article.getAtchFileId()));
		result.put("sessionUniqId", currentUniqId(null));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "게시물 등록", tags = {"EgovBBSManageApiController"})
	@PostMapping("/boards/{bbsId}/articles")
	public IntermediateResultVO<Object> insert(@PathVariable("bbsId") String bbsId,
			MultipartHttpServletRequest multiRequest,
			@ModelAttribute BoardVO boardVO) throws Exception {

		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		boardVO.setBbsId(bbsId);
		boardVO.setAtchFileId(saveNewFiles(multiRequest, ""));
		boardVO.setFrstRegisterId(user.getUniqId());
		boardVO.setNtcrNm(user.getName());

		bbsMngService.insertBoardArticle(boardVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "게시물 수정 (작성자·관리자)", tags = {"EgovBBSManageApiController"})
	@PutMapping("/boards/{bbsId}/articles/{nttId}")
	public IntermediateResultVO<Object> update(@PathVariable("bbsId") String bbsId,
			@PathVariable("nttId") long nttId,
			MultipartHttpServletRequest multiRequest,
			@ModelAttribute BoardVO boardVO) throws Exception {

		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		BoardVO stored = loadArticle(bbsId, nttId);
		if (!canModify(stored, user)) {
			return authError();
		}

		boardVO.setBbsId(bbsId);
		boardVO.setNttId(nttId);

		String atchFileId = stored.getAtchFileId();
		if (atchFileId == null || atchFileId.isBlank()) {
			boardVO.setAtchFileId(saveNewFiles(multiRequest, ""));
		} else {
			appendFiles(multiRequest, atchFileId);
			boardVO.setAtchFileId(atchFileId);
		}

		boardVO.setLastUpdusrId(user.getUniqId());
		bbsMngService.updateBoardArticle(boardVO);
		return IntermediateResultVO.success(null);
	}

	/**
	 * 게시물 답변 등록.
	 *
	 * <p>답변은 원글 아래에 붙어 트리로 보인다. 그 순서를 서버가 계산할 수 있도록
	 * <b>원글의 정렬 정보(parnts·sortOrdr·replyLc)를 함께</b> 넘긴다 —
	 * 프론트가 아니라 원글에서 읽어 채우므로, 클라이언트가 조작해도 트리가 망가지지 않는다.</p>
	 */
	@Operation(summary = "게시물 답변 등록", tags = {"EgovBBSManageApiController"})
	@PostMapping("/boards/{bbsId}/articles/{nttId}/replies")
	public IntermediateResultVO<Object> reply(@PathVariable("bbsId") String bbsId,
			@PathVariable("nttId") long nttId,
			MultipartHttpServletRequest multiRequest,
			@ModelAttribute BoardVO boardVO) throws Exception {

		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		BoardVO parent = loadArticle(bbsId, nttId);
		if (parent == null) {
			IntermediateResultVO<Object> error = new IntermediateResultVO<>();
			error.setResultCode(ResponseCode.INPUT_CHECK_ERROR.getCode());
			error.setResultMessage("원글을 찾을 수 없습니다.");
			return error;
		}

		boardVO.setBbsId(bbsId);
		boardVO.setReplyAt("Y");
		// 트리 위치는 원글 값에서 파생한다 (요청 본문 값은 신뢰하지 않는다)
		boardVO.setParnts(Long.toString(parent.getNttId()));
		boardVO.setSortOrdr(parent.getSortOrdr());
		boardVO.setReplyLc(Integer.toString(Integer.parseInt(parent.getReplyLc()) + 1));

		boardVO.setAtchFileId(saveNewFiles(multiRequest, ""));
		boardVO.setFrstRegisterId(user.getUniqId());
		boardVO.setNtcrNm(user.getName());

		bbsMngService.insertBoardArticle(boardVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "게시물 삭제 (작성자·관리자)", tags = {"EgovBBSManageApiController"})
	@DeleteMapping("/boards/{bbsId}/articles/{nttId}")
	public IntermediateResultVO<Object> delete(@PathVariable("bbsId") String bbsId,
			@PathVariable("nttId") long nttId) throws Exception {

		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		BoardVO stored = loadArticle(bbsId, nttId);
		if (!canModify(stored, user)) {
			return authError();
		}

		BoardVO param = new BoardVO();
		param.setBbsId(bbsId);
		param.setNttId(nttId);
		param.setLastUpdusrId(user.getUniqId());

		bbsMngService.deleteBoardArticle(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	private BoardMasterVO boardMaster(String bbsId) throws Exception {
		BoardMaster param = new BoardMaster();
		param.setBbsId(bbsId);
		return bbsAttrbService.selectBBSMasterInf(param);
	}

	private BoardVO loadArticle(String bbsId, long nttId) throws Exception {
		BoardVO param = new BoardVO();
		param.setBbsId(bbsId);
		param.setNttId(nttId);
		param.setPlusCount(false); // 소유권 확인 목적이므로 조회수를 올리지 않는다
		return bbsMngService.selectBoardArticle(param);
	}

	/** 작성자 본인이거나 관리자면 수정·삭제할 수 있다. */
	private boolean canModify(BoardVO article, LoginVO user) {
		if (article == null || user == null) {
			return false;
		}
		if (EgovUserDetailsHelper.getAuthorities() != null
				&& EgovUserDetailsHelper.getAuthorities().contains("ROLE_ADMIN")) {
			return true;
		}
		return user.getUniqId() != null && user.getUniqId().equals(article.getFrstRegisterId());
	}

	private LoginVO currentUser() {
		if (!Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			return null;
		}
		Object user = EgovUserDetailsHelper.getAuthenticatedUser();
		return (user instanceof LoginVO loginVO) ? loginVO : null;
	}

	private String currentUniqId(String fallback) {
		LoginVO user = currentUser();
		return (user == null) ? fallback : user.getUniqId();
	}

	private IntermediateResultVO<Object> authError() {
		IntermediateResultVO<Object> error = new IntermediateResultVO<>();
		error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
		return error;
	}

	private List<FileVO> attachedFiles(String atchFileId) throws Exception {
		if (atchFileId == null || atchFileId.isBlank()) {
			return new ArrayList<>();
		}
		FileVO fileVO = new FileVO();
		fileVO.setAtchFileId(atchFileId);
		return fileMngService.selectFileInfs(fileVO);
	}

	private String saveNewFiles(MultipartHttpServletRequest multiRequest, String atchFileId) throws Exception {
		Map<String, MultipartFile> files = multiRequest.getFileMap();
		if (!hasRealFile(files)) {
			return atchFileId;
		}
		List<FileVO> parsed = fileUtil.parseFileInf(files, "BBS_", 0, atchFileId, "");
		return (parsed == null || parsed.isEmpty()) ? atchFileId : fileMngService.insertFileInfs(parsed);
	}

	private void appendFiles(MultipartHttpServletRequest multiRequest, String atchFileId) throws Exception {
		Map<String, MultipartFile> files = multiRequest.getFileMap();
		if (!hasRealFile(files)) {
			return;
		}
		FileVO fvo = new FileVO();
		fvo.setAtchFileId(atchFileId);
		int nextSn = fileMngService.getMaxFileSN(fvo);
		List<FileVO> parsed = fileUtil.parseFileInf(files, "BBS_", nextSn, atchFileId, "");
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

	private int toInt(Object value) {
		return (value instanceof Number number) ? number.intValue() : 0;
	}
}
