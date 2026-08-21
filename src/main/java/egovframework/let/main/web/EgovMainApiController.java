package egovframework.let.main.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.let.cop.bbs.service.BoardVO;
import egovframework.let.cop.bbs.service.EgovBBSManageService;
import egovframework.let.uss.ion.bnr.service.BannerVO;
import egovframework.let.uss.ion.bnr.service.EgovBannerService;
import egovframework.let.uss.olh.faq.service.EgovFaqManageService;
import egovframework.let.uss.olh.faq.service.FaqManageDefaultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 메인 화면 구성 데이터 API.
 *
 * <p>서버 렌더링 시절에는 메인 컨트롤러가 배너·공지·FAQ 를 모델에 담아 한 번에 그렸다.
 * SPA 에서도 <b>첫 화면에 필요한 데이터를 한 번의 호출로</b> 내려준다 — 화면 하나를 그리려고
 * 네 번 왕복하면 초기 로딩이 눈에 띄게 느려지기 때문이다.</p>
 *
 * <p>일부 영역 조회가 실패해도 메인 화면 자체는 떠야 하므로, 영역별로 실패를 흡수하고
 * 빈 목록을 담아 응답한다.</p>
 */
@Slf4j
@RestController
@Tag(name = "EgovMainApiController", description = "메인 화면")
public class EgovMainApiController {

	@Resource(name = "EgovBBSManageService")
	private EgovBBSManageService bbsMngService;

	@Resource(name = "FaqManageService")
	private EgovFaqManageService faqManageService;

	@Resource(name = "egovBannerService")
	private EgovBannerService bannerService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	/** 메인에 노출할 공지 게시판 ID */
	@Value("${portal.main.noticeBbsId:BBSMSTR_AAAAAAAAAAAA}")
	private String noticeBbsId;

	/** 메인 배너 타입 (팝업·푸터 배너와 구분) */
	private static final String MAIN_BANNER_TYPE = "MAIN";

	/** 메인 목록에 보여줄 건수 */
	@Value("${portal.main.previewCount:5}")
	private int previewCount;

	@Operation(summary = "메인 화면 데이터", description = "배너·공지·FAQ 요약을 한 번에 반환한다.",
			tags = {"EgovMainApiController"})
	@GetMapping("/main")
	public IntermediateResultVO<Map<String, Object>> main() {
		Map<String, Object> result = new HashMap<>();

		result.put("bannerList", safely("배너", this::loadBanners));
		result.put("noticeList", safely("공지", this::loadNotices));
		result.put("faqList", safely("FAQ", this::loadFaqs));
		result.put("noticeBbsId", noticeBbsId);

		return IntermediateResultVO.success(result);
	}

	/**
	 * 메인 화면에 노출할 배너.
	 *
	 * <p>배너 테이블에는 메인·팝업·푸터가 함께 들어 있다. 타입을 거르지 않으면
	 * 팝업 공지와 푸터 배너까지 메인 상단 배너 줄에 나온다.</p>
	 */
	private Object loadBanners() throws Exception {
		BannerVO bannerVO = new BannerVO();
		List<BannerVO> banners = bannerService.selectBannerResult(bannerVO);
		if (banners == null) {
			return List.of();
		}
		return banners.stream()
				.filter(banner -> MAIN_BANNER_TYPE.equalsIgnoreCase(banner.getBannerTy()))
				.toList();
	}

	private Object loadNotices() throws Exception {
		BoardVO boardVO = new BoardVO();
		boardVO.setBbsId(noticeBbsId);
		boardVO.setPageIndex(1);
		boardVO.setFirstIndex(0);
		boardVO.setLastIndex(previewCount);
		boardVO.setRecordCountPerPage(previewCount);

		Map<String, Object> serviceResult = bbsMngService.selectBoardArticles(boardVO, "BBSA02");
		return serviceResult.get("resultList");
	}

	private Object loadFaqs() throws Exception {
		FaqManageDefaultVO searchVO = new FaqManageDefaultVO();
		searchVO.setPageIndex(1);
		searchVO.setFirstIndex(0);
		searchVO.setLastIndex(previewCount);
		searchVO.setRecordCountPerPage(previewCount);
		return faqManageService.selectFaqList(searchVO);
	}

	/**
	 * 영역별 조회 실패를 흡수한다.
	 * 한 영역(예: 배너 테이블 미구성)이 실패했다고 메인 화면 전체가 오류가 되면 안 된다.
	 */
	private Object safely(String label, ThrowingSupplier supplier) {
		try {
			Object value = supplier.get();
			return (value == null) ? List.of() : value;
		} catch (Exception e) {
			log.warn("메인 화면 {} 영역을 불러오지 못했습니다. 빈 목록으로 대체합니다.", label, e);
			return List.of();
		}
	}

	@FunctionalInterface
	private interface ThrowingSupplier {
		Object get() throws Exception;
	}
}
