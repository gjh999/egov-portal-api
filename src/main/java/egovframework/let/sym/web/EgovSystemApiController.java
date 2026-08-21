package egovframework.let.sym.web;

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
import egovframework.let.sym.cal.service.EgovCalRestdeManageService;
import egovframework.let.sym.cal.service.Restde;
import egovframework.let.sym.cal.service.RestdeVO;
import egovframework.let.sym.ccm.zip.service.EgovCcmZipManageService;
import egovframework.let.sym.ccm.zip.service.Zip;
import egovframework.let.sym.ccm.zip.service.ZipVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 시스템 관리 API — 공휴일 · 우편번호.
 *
 * <p>공휴일 조회는 공개다(달력 화면이 로그인 전에도 필요). 나머지는 관리자 전용이다.</p>
 */
@RestController
@Tag(name = "EgovSystemApiController", description = "시스템 관리 (공휴일 · 우편번호)")
public class EgovSystemApiController {

	@Resource(name = "RestdeManageService")
	private EgovCalRestdeManageService restdeService;

	@Resource(name = "ZipManageService")
	private EgovCcmZipManageService zipService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------------ 공휴일

	@Operation(summary = "공휴일 목록", tags = {"EgovSystemApiController"})
	@GetMapping("/restde")
	public IntermediateResultVO<Map<String, Object>> restdeList(@ModelAttribute RestdeVO searchVO) throws Exception {
		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), propertiesService.getInt("pageUnit"), propertiesService.getInt("pageSize"));
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", restdeService.selectRestdeList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, restdeService.selectRestdeListTotCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "공휴일 상세", tags = {"EgovSystemApiController"})
	@GetMapping("/restde/{restdeNo}")
	public IntermediateResultVO<Restde> restdeDetail(@PathVariable("restdeNo") int restdeNo) throws Exception {
		Restde param = new Restde();
		param.setRestdeNo(restdeNo);
		return IntermediateResultVO.success(restdeService.selectRestdeDetail(param));
	}

	@Operation(summary = "공휴일 등록 (관리자)", tags = {"EgovSystemApiController"})
	@PostMapping("/admin/restde")
	public IntermediateResultVO<Object> insertRestde(@RequestBody Restde restde) throws Exception {
		restdeService.insertRestde(restde);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "공휴일 수정 (관리자)", tags = {"EgovSystemApiController"})
	@PutMapping("/admin/restde/{restdeNo}")
	public IntermediateResultVO<Object> updateRestde(@PathVariable("restdeNo") int restdeNo,
			@RequestBody Restde restde) throws Exception {
		restde.setRestdeNo(restdeNo);
		restdeService.updateRestde(restde);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "공휴일 삭제 (관리자)", tags = {"EgovSystemApiController"})
	@DeleteMapping("/admin/restde/{restdeNo}")
	public IntermediateResultVO<Object> deleteRestde(@PathVariable("restdeNo") int restdeNo) throws Exception {
		Restde param = new Restde();
		param.setRestdeNo(restdeNo);
		restdeService.deleteRestde(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 우편번호

	@Operation(summary = "우편번호 목록 (관리자)", tags = {"EgovSystemApiController"})
	@GetMapping("/zip")
	public IntermediateResultVO<Map<String, Object>> zipList(@ModelAttribute ZipVO searchVO) throws Exception {
		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), propertiesService.getInt("pageUnit"), propertiesService.getInt("pageSize"));
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", zipService.selectZipList(searchVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, zipService.selectZipListTotCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "우편번호 상세 (관리자)", tags = {"EgovSystemApiController"})
	@GetMapping("/zip/{zip}/{sn}")
	public IntermediateResultVO<Zip> zipDetail(@PathVariable("zip") String zip, @PathVariable("sn") int sn)
			throws Exception {
		Zip param = new Zip();
		param.setZip(zip);
		param.setSn(sn);
		return IntermediateResultVO.success(zipService.selectZipDetail(param));
	}

	@Operation(summary = "우편번호 등록 (관리자)", tags = {"EgovSystemApiController"})
	@PostMapping("/zip")
	public IntermediateResultVO<Object> insertZip(@RequestBody Zip zip) throws Exception {
		zipService.insertZip(zip);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "우편번호 수정 (관리자)", tags = {"EgovSystemApiController"})
	@PutMapping("/zip/{zip}/{sn}")
	public IntermediateResultVO<Object> updateZip(@PathVariable("zip") String zipCode,
			@PathVariable("sn") int sn, @RequestBody Zip zip) throws Exception {
		zip.setZip(zipCode);
		zip.setSn(sn);
		zipService.updateZip(zip);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "우편번호 삭제 (관리자)", tags = {"EgovSystemApiController"})
	@DeleteMapping("/zip/{zip}/{sn}")
	public IntermediateResultVO<Object> deleteZip(@PathVariable("zip") String zipCode,
			@PathVariable("sn") int sn) throws Exception {
		Zip param = new Zip();
		param.setZip(zipCode);
		param.setSn(sn);
		zipService.deleteZip(param);
		return IntermediateResultVO.success(null);
	}
}
