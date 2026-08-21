package egovframework.let.uss.umt.web;

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

import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.ResponseCode;
import egovframework.com.cmm.service.IntermediateResultVO;
import egovframework.com.cmm.util.EgovPaginationUtil;
import egovframework.let.uss.umt.service.EgovEntrprsMberManageService;
import egovframework.let.uss.umt.service.EgovMberManageService;
import egovframework.let.uss.umt.service.EgovMypageService;
import egovframework.let.uss.umt.service.EntrprsMberManageVO;
import egovframework.let.uss.umt.service.MberManageVO;
import egovframework.let.uss.umt.service.MypageVO;
import egovframework.let.uss.umt.service.UserDefaultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 회원 API — 가입 · 관리(관리자) · 마이페이지.
 *
 * <p>회원 종류가 둘이다: <b>일반회원(GNR)</b>과 <b>기업회원(ENT)</b>. 가입 경로와 저장 테이블이 달라
 * 등록 API 를 나눠 두었고, 목록·상세는 통합 서비스가 함께 다룬다.</p>
 */
@RestController
@Tag(name = "EgovMemberApiController", description = "회원 · 마이페이지")
public class EgovMemberApiController {

	/** 가입 직후 회원상태. 로그인 쿼리가 이 값만 허용한다. */
	private static final String MBER_STTUS_NORMAL = "P";

	@Resource(name = "mberManageService")
	private EgovMberManageService mberManageService;

	@Resource(name = "entrprsMberManageService")
	private EgovEntrprsMberManageService entrprsMberManageService;

	@Resource(name = "mypageService")
	private EgovMypageService mypageService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------------ 가입(공개)

	/**
	 * 아이디 중복 확인.
	 * 사용 가능하면 {@code available=true}. 이미 있으면 false.
	 */
	@Operation(summary = "아이디 중복 확인", tags = {"EgovMemberApiController"})
	@GetMapping("/members/check-id/{checkId}")
	public IntermediateResultVO<Map<String, Object>> checkId(@PathVariable("checkId") String checkId) throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("checkId", checkId);
		result.put("available", mberManageService.checkIdDplct(checkId) == 0);
		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "일반회원 가입", tags = {"EgovMemberApiController"})
	@PostMapping("/members/join/general")
	public IntermediateResultVO<Object> joinGeneral(@RequestBody MberManageVO mberManageVO) throws Exception {
		// 회원상태는 서버가 정한다. 요청 값을 그대로 저장하면 가입자가 자기 상태를 지정할 수 있고,
		// 비워 두면 null 로 저장돼 로그인 조건(상태 'P')에 걸려 가입한 계정으로 영영 로그인하지 못한다.
		mberManageVO.setMberSttus(MBER_STTUS_NORMAL);
		// 가입 시점에는 로그인 사용자가 없다 — 서비스가 고유 ID(uniqId)를 직접 채번한다
		mberManageService.insertMber(mberManageVO);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "기업회원 가입", tags = {"EgovMemberApiController"})
	@PostMapping("/members/join/enterprise")
	public IntermediateResultVO<Object> joinEnterprise(@RequestBody EntrprsMberManageVO vo) throws Exception {
		vo.setEntrprsMberSttus(MBER_STTUS_NORMAL);
		entrprsMberManageService.insertEntrprsMber(vo);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 관리(관리자)

	@Operation(summary = "회원 목록 (관리자)", tags = {"EgovMemberApiController"})
	@GetMapping("/admin/members")
	public IntermediateResultVO<Map<String, Object>> list(@ModelAttribute UserDefaultVO searchVO) throws Exception {
		searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
		searchVO.setPageSize(propertiesService.getInt("pageSize"));

		PaginationInfo paginationInfo = EgovPaginationUtil.create(
				searchVO.getPageIndex(), searchVO.getPageUnit(), searchVO.getPageSize());
		searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
		searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", mberManageService.selectMberList(searchVO));
		result.put("paginationInfo",
				EgovPaginationUtil.applyTotalCount(paginationInfo, mberManageService.selectMberListTotCnt(searchVO)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "회원 상세 (관리자)", tags = {"EgovMemberApiController"})
	@GetMapping("/admin/members/{mberId}")
	public IntermediateResultVO<MberManageVO> detail(@PathVariable("mberId") String mberId) throws Exception {
		return IntermediateResultVO.success(mberManageService.selectMber(mberId));
	}

	@Operation(summary = "회원 수정 (관리자)", tags = {"EgovMemberApiController"})
	@PutMapping("/admin/members/{mberId}")
	public IntermediateResultVO<Object> update(@PathVariable("mberId") String mberId,
			@RequestBody MberManageVO mberManageVO) throws Exception {
		mberManageVO.setMberId(mberId);
		mberManageService.updateMber(mberManageVO);
		return IntermediateResultVO.success(null);
	}

	/**
	 * 회원 삭제 (관리자).
	 * 서비스가 콤마로 구분된 복수 ID 를 받으므로 일괄 삭제도 같은 API 로 처리한다.
	 */
	@Operation(summary = "회원 삭제 (관리자)", tags = {"EgovMemberApiController"})
	@DeleteMapping("/admin/members/{mberIds}")
	public IntermediateResultVO<Object> delete(@PathVariable("mberIds") String mberIds) throws Exception {
		mberManageService.deleteMber(mberIds);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "회원 가입 승인 (관리자)", tags = {"EgovMemberApiController"})
	@PutMapping("/admin/members/{mberIds}/approve")
	public IntermediateResultVO<Object> approve(@PathVariable("mberIds") String mberIds) throws Exception {
		mberManageService.approveMber(mberIds);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 마이페이지

	@Operation(summary = "내 정보 조회", tags = {"EgovMemberApiController"})
	@GetMapping("/mypage")
	public IntermediateResultVO<MypageVO> myInfo() throws Exception {
		LoginVO user = currentUser();
		if (user == null) {
			return authErrorTyped();
		}

		MypageVO param = new MypageVO();
		param.setUniqId(user.getUniqId());
		param.setUserSe(user.getUserSe());
		return IntermediateResultVO.success(mypageService.selectMyInfo(param));
	}

	@Operation(summary = "내 정보 수정", tags = {"EgovMemberApiController"})
	@PutMapping("/mypage")
	public IntermediateResultVO<Object> updateMyInfo(@RequestBody MypageVO mypageVO) throws Exception {
		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		// 남의 정보를 고치지 못하도록, 대상은 항상 로그인 사용자로 덮어쓴다
		mypageVO.setUniqId(user.getUniqId());
		mypageVO.setUserSe(user.getUserSe());

		mypageService.updateMyInfo(mypageVO);
		return IntermediateResultVO.success(null);
	}

	/**
	 * 비밀번호 변경.
	 *
	 * <p>현재 비밀번호가 맞는지 서버에서 확인한 뒤에만 바꾼다. 프론트의 확인만 믿으면
	 * API 를 직접 호출해 남의 비밀번호를 바꾸는 경로가 열린다.</p>
	 */
	@Operation(summary = "비밀번호 변경", tags = {"EgovMemberApiController"})
	@PutMapping("/mypage/password")
	public IntermediateResultVO<Object> updateMyPassword(@RequestBody MypageVO mypageVO) throws Exception {
		LoginVO user = currentUser();
		if (user == null) {
			return authError();
		}

		mypageVO.setUniqId(user.getUniqId());
		mypageVO.setUserSe(user.getUserSe());

		MypageVO stored = mypageService.selectMyPassword(mypageVO);
		if (stored == null || stored.getPassword() == null
				|| !stored.getPassword().equals(mypageVO.getOldPassword())) {
			IntermediateResultVO<Object> error = new IntermediateResultVO<>();
			error.setResultCode(ResponseCode.INPUT_CHECK_ERROR.getCode());
			error.setResultMessage("현재 비밀번호가 일치하지 않습니다.");
			return error;
		}

		mypageService.updateMyPassword(mypageVO);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	private LoginVO currentUser() {
		if (!Boolean.TRUE.equals(EgovUserDetailsHelper.isAuthenticated())) {
			return null;
		}
		Object user = EgovUserDetailsHelper.getAuthenticatedUser();
		return (user instanceof LoginVO loginVO) ? loginVO : null;
	}

	private IntermediateResultVO<Object> authError() {
		IntermediateResultVO<Object> error = new IntermediateResultVO<>();
		error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
		return error;
	}

	private <T> IntermediateResultVO<T> authErrorTyped() {
		IntermediateResultVO<T> error = new IntermediateResultVO<>();
		error.setResultCode(ResponseCode.AUTH_ERROR.getCode());
		error.setResultMessage(ResponseCode.AUTH_ERROR.getMessage());
		return error;
	}
}
