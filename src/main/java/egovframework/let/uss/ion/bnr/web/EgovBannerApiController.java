package egovframework.let.uss.ion.bnr.web;

import java.util.HashMap;
import java.util.Map;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.beans.factory.annotation.Value;
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
import egovframework.let.uss.ion.bnr.service.Banner;
import egovframework.let.uss.ion.bnr.service.BannerVO;
import egovframework.let.uss.ion.bnr.service.EgovBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 배너 API.
 *
 * <p>메인 화면에 노출할 배너 조회는 공개, 등록·수정·삭제는 관리자 전용이다.
 * 서버 렌더링 시절에는 ControllerAdvice 가 모든 화면 모델에 배너를 끼워 넣었지만,
 * SPA 에서는 필요한 화면이 직접 이 API 를 호출한다.</p>
 */
@RestController
@Tag(name = "EgovBannerApiController", description = "배너")
public class EgovBannerApiController {

	@Resource(name = "egovBannerService")
	private EgovBannerService bannerService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	/** 배너 슬라이드 자동 전환 주기(ms) — 화면이 캐러셀을 돌릴 때 쓴다 */
	@Value("${portal.banner.interval:5000}")
	private int bannerInterval;

	@Operation(summary = "노출용 배너 목록", description = "메인 화면 캐러셀에 사용한다.",
			tags = {"EgovBannerApiController"})
	@GetMapping("/banners")
	public IntermediateResultVO<Map<String, Object>> visibleBanners(@ModelAttribute BannerVO bannerVO) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("resultList", bannerService.selectBannerResult(bannerVO));
		result.put("interval", bannerInterval);
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "배너 목록 (관리자)", tags = {"EgovBannerApiController"})
	@GetMapping("/admin/banners")
	public IntermediateResultVO<Map<String, Object>> list(@ModelAttribute BannerVO bannerVO) throws Exception {
		bannerVO.setPageUnit(propertiesService.getInt("pageUnit"));
		bannerVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				bannerVO.getPageIndex(), bannerVO.getPageUnit(), bannerVO.getPageSize());
		bannerVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		bannerVO.setLastIndex(paginationInfo.getLastRecordIndex());
		bannerVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", bannerService.selectBannerList(bannerVO));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, bannerService.selectBannerListTotCnt(bannerVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "배너 상세 (관리자)", tags = {"EgovBannerApiController"})
	@GetMapping("/admin/banners/{bannerId}")
	public IntermediateResultVO<BannerVO> detail(@PathVariable("bannerId") String bannerId) throws Exception {
		BannerVO param = new BannerVO();
		param.setBannerId(bannerId);
		return IntermediateResultVO.success(bannerService.selectBanner(param));
	}

	@Operation(summary = "배너 등록 (관리자)", tags = {"EgovBannerApiController"})
	@PostMapping("/admin/banners")
	public IntermediateResultVO<BannerVO> insert(@RequestBody BannerVO bannerVO) throws Exception {
		return IntermediateResultVO.success(bannerService.insertBanner(bannerVO, bannerVO));
	}

	@Operation(summary = "배너 수정 (관리자)", tags = {"EgovBannerApiController"})
	@PutMapping("/admin/banners/{bannerId}")
	public IntermediateResultVO<Object> update(@PathVariable("bannerId") String bannerId,
			@RequestBody BannerVO bannerVO) throws Exception {
		bannerVO.setBannerId(bannerId);
		bannerService.updateBanner(bannerVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "배너 삭제 (관리자)", tags = {"EgovBannerApiController"})
	@DeleteMapping("/admin/banners/{bannerId}")
	public IntermediateResultVO<Object> delete(@PathVariable("bannerId") String bannerId) throws Exception {
		Banner param = new Banner();
		param.setBannerId(bannerId);
		bannerService.deleteBanner(param);
		return IntermediateResultVO.success(null);
	}
}
