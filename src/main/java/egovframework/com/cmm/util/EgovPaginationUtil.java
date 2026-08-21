package egovframework.com.cmm.util;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;

/**
 * 목록 조회의 페이징 계산을 한곳에 모은 유틸.
 *
 * <p>서버 렌더링 시절에는 컨트롤러마다 같은 코드(pageUnit·pageSize 설정 → PaginationInfo 생성 →
 * firstIndex/lastIndex 역주입 → totalRecordCount 설정)를 복사해 두고 있었다. 한 곳이라도 순서가
 * 어긋나면 목록이 조용히 어긋나므로(예: 2페이지에 1페이지 내용이 나옴) 계산을 유틸로 고정한다.</p>
 *
 * <p>검색 조건 VO 들이 공통 부모를 갖지 않아(각자 {@code pageIndex}·{@code firstIndex} 필드를 따로 선언)
 * VO 자체를 인자로 받지는 못한다. 대신 계산만 담당하고, 결과 인덱스를 VO 에 넣는 일은 호출부가 한다.</p>
 *
 * <p>계산 결과는 응답의 {@code paginationInfo} 로 그대로 내보낸다 — 프론트가 페이지 범위를
 * 다시 계산하지 않게 하기 위해서다(두 곳에서 계산하면 반드시 어긋난다).</p>
 */
public final class EgovPaginationUtil {

	private EgovPaginationUtil() {
	}

	/**
	 * 현재 페이지 정보를 담은 PaginationInfo 를 만든다.
	 *
	 * <p>반환된 객체에는 <b>총 건수가 아직 없다</b>. 목록을 조회한 뒤
	 * {@link #applyTotalCount(PaginationInfo, int)} 로 총 건수를 넣어야 전체 페이지 수가 확정된다.</p>
	 *
	 * @param pageIndex 현재 페이지 번호 (1부터)
	 * @param pageUnit  한 페이지에 보여줄 건수
	 * @param pageSize  페이지 번호를 몇 개씩 묶어 보여줄지
	 */
	public static PaginationInfo create(int pageIndex, int pageUnit, int pageSize) {
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(pageIndex);
		paginationInfo.setRecordCountPerPage(pageUnit);
		paginationInfo.setPageSize(pageSize);
		return paginationInfo;
	}

	/** 조회된 총 건수를 반영해 전체 페이지 수를 확정한다. */
	public static PaginationInfo applyTotalCount(PaginationInfo paginationInfo, int totalCount) {
		paginationInfo.setTotalRecordCount(totalCount);
		return paginationInfo;
	}
}
