package egovframework.let.cop.com.web;

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
import egovframework.let.cop.com.service.BoardUseInf;
import egovframework.let.cop.com.service.BoardUseInfVO;
import egovframework.let.cop.com.service.EgovBBSUseInfoManageService;
import egovframework.let.cop.com.service.EgovTemplateManageService;
import egovframework.let.cop.com.service.TemplateInf;
import egovframework.let.cop.com.service.TemplateInfVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 게시판 부가 설정 API — 사용정보 · 템플릿 (관리자 전용).
 *
 * <p>둘 다 게시판을 <b>어디에·어떤 모양으로</b> 붙일지를 정하는 설정이다.</p>
 * <ul>
 *   <li><b>사용정보</b> — 어떤 대상(커뮤니티·동호회 등)이 어떤 게시판을 쓰는지의 연결 정보</li>
 *   <li><b>템플릿</b> — 게시판 화면의 표시 형식. 게시판 마스터가 {@code tmplatId} 로 참조한다</li>
 * </ul>
 */
@RestController
@Tag(name = "EgovBoardSupportApiController", description = "게시판 사용정보 · 템플릿")
public class EgovBoardSupportApiController {

	@Resource(name = "EgovBBSUseInfoManageService")
	private EgovBBSUseInfoManageService bbsUseService;

	@Resource(name = "EgovTemplateManageService")
	private EgovTemplateManageService templateService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------- 게시판 사용정보

	@Operation(summary = "게시판 사용정보 목록", tags = {"EgovBoardSupportApiController"})
	@GetMapping("/board-use")
	public IntermediateResultVO<Map<String, Object>> useInfoList(@ModelAttribute BoardUseInfVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(
				searchVO.getPageIndex(), searchVO::setPageUnit, searchVO::setPageSize,
				searchVO::setFirstIndex, searchVO::setLastIndex, searchVO::setRecordCountPerPage);

		Map<String, Object> serviceResult = bbsUseService.selectBBSUseInfs(searchVO);

		Map<String, Object> result = new HashMap<>(serviceResult);
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, toInt(serviceResult.get("resultCnt"))));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "게시판 사용정보 상세", tags = {"EgovBoardSupportApiController"})
	@GetMapping("/board-use/{trgetId}/{bbsId}")
	public IntermediateResultVO<BoardUseInfVO> useInfoDetail(@PathVariable("trgetId") String trgetId,
			@PathVariable("bbsId") String bbsId) throws Exception {
		BoardUseInfVO param = new BoardUseInfVO();
		param.setTrgetId(trgetId);
		param.setBbsId(bbsId);
		return IntermediateResultVO.success(bbsUseService.selectBBSUseInf(param));
	}

	@Operation(summary = "게시판 사용정보 등록", tags = {"EgovBoardSupportApiController"})
	@PostMapping("/board-use")
	public IntermediateResultVO<Object> insertUseInfo(@RequestBody BoardUseInf bdUseInf) throws Exception {
		bbsUseService.insertBBSUseInf(bdUseInf);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "게시판 사용정보 수정", tags = {"EgovBoardSupportApiController"})
	@PutMapping("/board-use/{trgetId}/{bbsId}")
	public IntermediateResultVO<Object> updateUseInfo(@PathVariable("trgetId") String trgetId,
			@PathVariable("bbsId") String bbsId, @RequestBody BoardUseInf bdUseInf) throws Exception {
		bdUseInf.setTrgetId(trgetId);
		bdUseInf.setBbsId(bbsId);
		bbsUseService.updateBBSUseInf(bdUseInf);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "게시판 사용정보 삭제", tags = {"EgovBoardSupportApiController"})
	@DeleteMapping("/board-use/{trgetId}/{bbsId}")
	public IntermediateResultVO<Object> deleteUseInfo(@PathVariable("trgetId") String trgetId,
			@PathVariable("bbsId") String bbsId) throws Exception {
		BoardUseInf param = new BoardUseInf();
		param.setTrgetId(trgetId);
		param.setBbsId(bbsId);
		bbsUseService.deleteBBSUseInf(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 템플릿

	@Operation(summary = "템플릿 목록", tags = {"EgovBoardSupportApiController"})
	@GetMapping("/templates")
	public IntermediateResultVO<Map<String, Object>> templateList(@ModelAttribute TemplateInfVO searchVO)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(
				searchVO.getPageIndex(), searchVO::setPageUnit, searchVO::setPageSize,
				searchVO::setFirstIndex, searchVO::setLastIndex, searchVO::setRecordCountPerPage);

		Map<String, Object> serviceResult = templateService.selectTemplateInfs(searchVO);

		Map<String, Object> result = new HashMap<>(serviceResult);
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, toInt(serviceResult.get("resultCnt"))));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "템플릿 상세", tags = {"EgovBoardSupportApiController"})
	@GetMapping("/templates/{tmplatId}")
	public IntermediateResultVO<TemplateInfVO> templateDetail(@PathVariable("tmplatId") String tmplatId)
			throws Exception {
		TemplateInfVO param = new TemplateInfVO();
		param.setTmplatId(tmplatId);
		return IntermediateResultVO.success(templateService.selectTemplateInf(param));
	}

	/**
	 * 템플릿 미리보기.
	 * 목록에서 고른 템플릿이 실제로 어떻게 보이는지 확인할 때 쓴다(적용 전 확인용).
	 */
	@Operation(summary = "템플릿 미리보기", tags = {"EgovBoardSupportApiController"})
	@GetMapping("/templates/{tmplatId}/preview")
	public IntermediateResultVO<TemplateInfVO> templatePreview(@PathVariable("tmplatId") String tmplatId)
			throws Exception {
		TemplateInfVO param = new TemplateInfVO();
		param.setTmplatId(tmplatId);
		return IntermediateResultVO.success(templateService.selectTemplatePreview(param));
	}

	@Operation(summary = "템플릿 등록", tags = {"EgovBoardSupportApiController"})
	@PostMapping("/templates")
	public IntermediateResultVO<Object> insertTemplate(@RequestBody TemplateInf tmplatInf) throws Exception {
		templateService.insertTemplateInf(tmplatInf);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "템플릿 수정", tags = {"EgovBoardSupportApiController"})
	@PutMapping("/templates/{tmplatId}")
	public IntermediateResultVO<Object> updateTemplate(@PathVariable("tmplatId") String tmplatId,
			@RequestBody TemplateInf tmplatInf) throws Exception {
		tmplatInf.setTmplatId(tmplatId);
		templateService.updateTemplateInf(tmplatInf);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "템플릿 삭제", tags = {"EgovBoardSupportApiController"})
	@DeleteMapping("/templates/{tmplatId}")
	public IntermediateResultVO<Object> deleteTemplate(@PathVariable("tmplatId") String tmplatId) throws Exception {
		TemplateInf param = new TemplateInf();
		param.setTmplatId(tmplatId);
		templateService.deleteTemplateInf(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	/**
	 * 검색 조건 VO 들이 공통 부모를 갖지 않아 setter 를 넘겨받아 채운다.
	 * (VO 마다 pageIndex·firstIndex 필드를 따로 선언하고 있다)
	 */
	private PaginationInfo preparePaging(int pageIndex,
			java.util.function.IntConsumer setPageUnit,
			java.util.function.IntConsumer setPageSize,
			java.util.function.IntConsumer setFirstIndex,
			java.util.function.IntConsumer setLastIndex,
			java.util.function.IntConsumer setRecordCountPerPage) {

		int pageUnit = propertiesService.getInt("pageUnit");
		int pageSize = propertiesService.getInt("pageSize");
		setPageUnit.accept(pageUnit);
		setPageSize.accept(pageSize);

		PaginationInfo paginationInfo = EgovPaginationUtil.create(pageIndex, pageUnit, pageSize);
		setFirstIndex.accept(paginationInfo.getFirstRecordIndex());
		setLastIndex.accept(paginationInfo.getLastRecordIndex());
		setRecordCountPerPage.accept(paginationInfo.getRecordCountPerPage());
		return paginationInfo;
	}

	private int toInt(Object value) {
		return (value instanceof Number number) ? number.intValue() : 0;
	}
}
