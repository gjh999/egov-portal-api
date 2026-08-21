package egovframework.let.cop.bbs.web;

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
import egovframework.let.cop.bbs.service.BoardMaster;
import egovframework.let.cop.bbs.service.BoardMasterVO;
import egovframework.let.cop.bbs.service.EgovBBSAttributeManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 게시판 마스터(게시판 자체의 속성) API.
 *
 * <p>조회는 공개다 — 프론트가 게시판 이름·첨부 정책을 알아야 목록 화면을 그릴 수 있다.
 * 생성·수정·삭제는 관리자 전용(`/admin/board-masters`)이다.</p>
 */
@RestController
@Tag(name = "EgovBoardMasterApiController", description = "게시판 마스터")
public class EgovBoardMasterApiController {

	@Resource(name = "EgovBBSAttributeManageService")
	private EgovBBSAttributeManageService bbsAttrbService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	@Operation(summary = "게시판 목록", tags = {"EgovBoardMasterApiController"})
	@GetMapping("/board-masters")
	public IntermediateResultVO<Map<String, Object>> list(@ModelAttribute BoardMasterVO searchVO) throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> serviceResult = bbsAttrbService.selectBBSMasterInfs(searchVO);

		Map<String, Object> result = new HashMap<>(serviceResult);
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, toInt(serviceResult.get("resultCnt"))));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "게시판 상세", tags = {"EgovBoardMasterApiController"})
	@GetMapping("/board-masters/{bbsId}")
	public IntermediateResultVO<BoardMasterVO> detail(@PathVariable("bbsId") String bbsId) throws Exception {
		BoardMaster param = new BoardMaster();
		param.setBbsId(bbsId);
		return IntermediateResultVO.success(bbsAttrbService.selectBBSMasterInf(param));
	}

	@Operation(summary = "게시판 생성 (관리자)", tags = {"EgovBoardMasterApiController"})
	@PostMapping("/admin/board-masters")
	public IntermediateResultVO<Map<String, Object>> insert(@RequestBody BoardMaster boardMaster) throws Exception {
		String bbsId = bbsAttrbService.insertBBSMastetInf(boardMaster);

		Map<String, Object> result = new HashMap<>();
		result.put("bbsId", bbsId);
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "게시판 수정 (관리자)", tags = {"EgovBoardMasterApiController"})
	@PutMapping("/admin/board-masters/{bbsId}")
	public IntermediateResultVO<Object> update(@PathVariable("bbsId") String bbsId,
			@RequestBody BoardMaster boardMaster) throws Exception {
		boardMaster.setBbsId(bbsId);
		bbsAttrbService.updateBBSMasterInf(boardMaster);
		return IntermediateResultVO.success(null);
	}

	/**
	 * 게시판 삭제 (관리자).
	 * 실제로는 사용 여부를 'N' 으로 바꾸는 논리 삭제다 — 이미 쌓인 게시물을 잃지 않기 위해서다.
	 */
	@Operation(summary = "게시판 삭제 (관리자)", tags = {"EgovBoardMasterApiController"})
	@DeleteMapping("/admin/board-masters/{bbsId}")
	public IntermediateResultVO<Object> delete(@PathVariable("bbsId") String bbsId) throws Exception {
		BoardMaster param = new BoardMaster();
		param.setBbsId(bbsId);
		bbsAttrbService.deleteBBSMasterInf(param);
		return IntermediateResultVO.success(null);
	}

	private int toInt(Object value) {
		return (value instanceof Number number) ? number.intValue() : 0;
	}
}
