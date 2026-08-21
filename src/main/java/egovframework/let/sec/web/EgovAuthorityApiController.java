package egovframework.let.sec.web;

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
import egovframework.let.sec.gmt.service.EgovGroupManageService;
import egovframework.let.sec.gmt.service.GroupManage;
import egovframework.let.sec.gmt.service.GroupManageVO;
import egovframework.let.sec.ram.service.AuthorManage;
import egovframework.let.sec.ram.service.AuthorManageVO;
import egovframework.let.sec.ram.service.AuthorRoleManage;
import egovframework.let.sec.ram.service.AuthorRoleManageVO;
import egovframework.let.sec.ram.service.EgovAuthorManageService;
import egovframework.let.sec.ram.service.EgovAuthorRoleManageService;
import egovframework.let.sec.rgm.service.AuthorGroup;
import egovframework.let.sec.rgm.service.AuthorGroupVO;
import egovframework.let.sec.rgm.service.EgovAuthorGroupService;
import egovframework.let.sec.rmt.service.EgovRoleManageService;
import egovframework.let.sec.rmt.service.RoleManage;
import egovframework.let.sec.rmt.service.RoleManageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

/**
 * 보안 설정 API — 권한 · 롤 · 그룹 · 사용자권한 매핑 (모두 관리자 전용).
 *
 * <p>네 가지가 서로 맞물려 동작한다.</p>
 * <ul>
 *   <li><b>권한</b>(ram) — ROLE_ADMIN 같은 권한 자체</li>
 *   <li><b>롤</b>(rmt) — 어떤 URL·메서드를 허용할지</li>
 *   <li><b>권한-롤</b>(ram) — 권한에 롤을 붙이는 매핑</li>
 *   <li><b>그룹</b>(gmt) / <b>권한그룹</b>(rgm) — 사용자 묶음과 그 묶음의 권한</li>
 * </ul>
 * <p>화면이 이들을 오가며 편집하므로 컨트롤러를 하나로 모았다.</p>
 */
@RestController
@Tag(name = "EgovAuthorityApiController", description = "권한 · 롤 · 그룹")
public class EgovAuthorityApiController {

	@Resource(name = "egovAuthorManageService")
	private EgovAuthorManageService authorManageService;

	@Resource(name = "egovAuthorRoleManageService")
	private EgovAuthorRoleManageService authorRoleManageService;

	@Resource(name = "egovAuthorGroupService")
	private EgovAuthorGroupService authorGroupService;

	@Resource(name = "egovGroupManageService")
	private EgovGroupManageService groupManageService;

	@Resource(name = "egovRoleManageService")
	private EgovRoleManageService roleManageService;

	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	// ------------------------------------------------------------------ 권한(ram)

	@Operation(summary = "권한 목록", tags = {"EgovAuthorityApiController"})
	@GetMapping("/authorities")
	public IntermediateResultVO<Map<String, Object>> authorList(@ModelAttribute AuthorManageVO vo) throws Exception {
		PaginationInfo paginationInfo = preparePaging(vo.getPageIndex());
		vo.setPageUnit(propertiesService.getInt("pageUnit"));
		vo.setPageSize(propertiesService.getInt("pageSize"));
		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", authorManageService.selectAuthorList(vo));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, authorManageService.selectAuthorListTotCnt(vo)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "전체 권한 (선택 목록용)", tags = {"EgovAuthorityApiController"})
	@GetMapping("/authorities/all")
	public IntermediateResultVO<Object> authorAll(@ModelAttribute AuthorManageVO vo) throws Exception {
		return IntermediateResultVO.success(authorManageService.selectAuthorAllList(vo));
	}

	@Operation(summary = "권한 상세", tags = {"EgovAuthorityApiController"})
	@GetMapping("/authorities/{authorCode}")
	public IntermediateResultVO<AuthorManageVO> authorDetail(@PathVariable("authorCode") String authorCode)
			throws Exception {
		AuthorManageVO param = new AuthorManageVO();
		param.setAuthorCode(authorCode);
		return IntermediateResultVO.success(authorManageService.selectAuthor(param));
	}

	@Operation(summary = "권한 등록", tags = {"EgovAuthorityApiController"})
	@PostMapping("/authorities")
	public IntermediateResultVO<Object> insertAuthor(@RequestBody AuthorManage authorManage) throws Exception {
		authorManageService.insertAuthor(authorManage);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "권한 수정", tags = {"EgovAuthorityApiController"})
	@PutMapping("/authorities/{authorCode}")
	public IntermediateResultVO<Object> updateAuthor(@PathVariable("authorCode") String authorCode,
			@RequestBody AuthorManage authorManage) throws Exception {
		authorManage.setAuthorCode(authorCode);
		authorManageService.updateAuthor(authorManage);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "권한 삭제", tags = {"EgovAuthorityApiController"})
	@DeleteMapping("/authorities/{authorCode}")
	public IntermediateResultVO<Object> deleteAuthor(@PathVariable("authorCode") String authorCode) throws Exception {
		AuthorManage param = new AuthorManage();
		param.setAuthorCode(authorCode);
		authorManageService.deleteAuthor(param);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 롤(rmt)

	@Operation(summary = "롤 목록", tags = {"EgovAuthorityApiController"})
	@GetMapping("/roles")
	public IntermediateResultVO<Map<String, Object>> roleList(@ModelAttribute RoleManageVO vo) throws Exception {
		PaginationInfo paginationInfo = preparePaging(vo.getPageIndex());
		vo.setPageUnit(propertiesService.getInt("pageUnit"));
		vo.setPageSize(propertiesService.getInt("pageSize"));
		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", roleManageService.selectRoleList(vo));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, roleManageService.selectRoleListTotCnt(vo)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "전체 롤 (선택 목록용)", tags = {"EgovAuthorityApiController"})
	@GetMapping("/roles/all")
	public IntermediateResultVO<Object> roleAll(@ModelAttribute RoleManageVO vo) throws Exception {
		return IntermediateResultVO.success(roleManageService.selectRoleAllList(vo));
	}

	@Operation(summary = "롤 상세", tags = {"EgovAuthorityApiController"})
	@GetMapping("/roles/{roleCode}")
	public IntermediateResultVO<RoleManageVO> roleDetail(@PathVariable("roleCode") String roleCode) throws Exception {
		RoleManageVO param = new RoleManageVO();
		param.setRoleCode(roleCode);
		return IntermediateResultVO.success(roleManageService.selectRole(param));
	}

	@Operation(summary = "롤 등록", tags = {"EgovAuthorityApiController"})
	@PostMapping("/roles")
	public IntermediateResultVO<RoleManageVO> insertRole(@RequestBody RoleManageVO vo) throws Exception {
		return IntermediateResultVO.success(roleManageService.insertRole(vo, vo));
	}

	@Operation(summary = "롤 수정", tags = {"EgovAuthorityApiController"})
	@PutMapping("/roles/{roleCode}")
	public IntermediateResultVO<Object> updateRole(@PathVariable("roleCode") String roleCode,
			@RequestBody RoleManage roleManage) throws Exception {
		roleManage.setRoleCode(roleCode);
		roleManageService.updateRole(roleManage);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "롤 삭제", tags = {"EgovAuthorityApiController"})
	@DeleteMapping("/roles/{roleCode}")
	public IntermediateResultVO<Object> deleteRole(@PathVariable("roleCode") String roleCode) throws Exception {
		RoleManage param = new RoleManage();
		param.setRoleCode(roleCode);
		roleManageService.deleteRole(param);
		return IntermediateResultVO.success(null);
	}

	// -------------------------------------------------------------- 권한-롤(ram)

	@Operation(summary = "권한-롤 매핑 목록", tags = {"EgovAuthorityApiController"})
	@GetMapping("/author-roles")
	public IntermediateResultVO<Map<String, Object>> authorRoleList(@ModelAttribute AuthorRoleManageVO vo)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(vo.getPageIndex());
		vo.setPageUnit(propertiesService.getInt("pageUnit"));
		vo.setPageSize(propertiesService.getInt("pageSize"));
		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", authorRoleManageService.selectAuthorRoleList(vo));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, authorRoleManageService.selectAuthorRoleListTotCnt(vo)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "권한-롤 매핑 등록", tags = {"EgovAuthorityApiController"})
	@PostMapping("/author-roles")
	public IntermediateResultVO<Object> insertAuthorRole(@RequestBody AuthorRoleManage vo) throws Exception {
		authorRoleManageService.insertAuthorRole(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "권한-롤 매핑 삭제", tags = {"EgovAuthorityApiController"})
	@DeleteMapping("/author-roles")
	public IntermediateResultVO<Object> deleteAuthorRole(@RequestBody AuthorRoleManage vo) throws Exception {
		authorRoleManageService.deleteAuthorRole(vo);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 그룹(gmt)

	@Operation(summary = "그룹 목록", tags = {"EgovAuthorityApiController"})
	@GetMapping("/groups")
	public IntermediateResultVO<Map<String, Object>> groupList(@ModelAttribute GroupManageVO vo) throws Exception {
		PaginationInfo paginationInfo = preparePaging(vo.getPageIndex());
		vo.setPageUnit(propertiesService.getInt("pageUnit"));
		vo.setPageSize(propertiesService.getInt("pageSize"));
		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", groupManageService.selectGroupList(vo));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, groupManageService.selectGroupListTotCnt(vo)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "그룹 상세", tags = {"EgovAuthorityApiController"})
	@GetMapping("/groups/{groupId}")
	public IntermediateResultVO<GroupManageVO> groupDetail(@PathVariable("groupId") String groupId) throws Exception {
		GroupManageVO param = new GroupManageVO();
		param.setGroupId(groupId);
		return IntermediateResultVO.success(groupManageService.selectGroup(param));
	}

	@Operation(summary = "그룹 등록", tags = {"EgovAuthorityApiController"})
	@PostMapping("/groups")
	public IntermediateResultVO<GroupManageVO> insertGroup(@RequestBody GroupManageVO vo) throws Exception {
		return IntermediateResultVO.success(groupManageService.insertGroup(vo, vo));
	}

	@Operation(summary = "그룹 수정", tags = {"EgovAuthorityApiController"})
	@PutMapping("/groups/{groupId}")
	public IntermediateResultVO<Object> updateGroup(@PathVariable("groupId") String groupId,
			@RequestBody GroupManage groupManage) throws Exception {
		groupManage.setGroupId(groupId);
		groupManageService.updateGroup(groupManage);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "그룹 삭제", tags = {"EgovAuthorityApiController"})
	@DeleteMapping("/groups/{groupId}")
	public IntermediateResultVO<Object> deleteGroup(@PathVariable("groupId") String groupId) throws Exception {
		GroupManage param = new GroupManage();
		param.setGroupId(groupId);
		groupManageService.deleteGroup(param);
		return IntermediateResultVO.success(null);
	}

	// -------------------------------------------------------------- 권한그룹(rgm)

	@Operation(summary = "사용자별 권한 목록", tags = {"EgovAuthorityApiController"})
	@GetMapping("/author-groups")
	public IntermediateResultVO<Map<String, Object>> authorGroupList(@ModelAttribute AuthorGroupVO vo)
			throws Exception {
		PaginationInfo paginationInfo = preparePaging(vo.getPageIndex());
		vo.setPageUnit(propertiesService.getInt("pageUnit"));
		vo.setPageSize(propertiesService.getInt("pageSize"));
		vo.setFirstIndex(paginationInfo.getFirstRecordIndex());
		vo.setLastIndex(paginationInfo.getLastRecordIndex());
		vo.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		Map<String, Object> result = new HashMap<>();
		result.put("resultList", authorGroupService.selectAuthorGroupList(vo));
		result.put("paginationInfo", EgovPaginationUtil.applyTotalCount(
				paginationInfo, authorGroupService.selectAuthorGroupListTotCnt(vo)));

		return IntermediateResultVO.success(result);
	}

	@Operation(summary = "사용자 권한 부여", tags = {"EgovAuthorityApiController"})
	@PostMapping("/author-groups")
	public IntermediateResultVO<Object> insertAuthorGroup(@RequestBody AuthorGroup vo) throws Exception {
		authorGroupService.insertAuthorGroup(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "사용자 권한 변경", tags = {"EgovAuthorityApiController"})
	@PutMapping("/author-groups")
	public IntermediateResultVO<Object> updateAuthorGroup(@RequestBody AuthorGroup vo) throws Exception {
		authorGroupService.updateAuthorGroup(vo);
		return IntermediateResultVO.success(null);
	}

	@Operation(summary = "사용자 권한 회수", tags = {"EgovAuthorityApiController"})
	@DeleteMapping("/author-groups")
	public IntermediateResultVO<Object> deleteAuthorGroup(@RequestBody AuthorGroup vo) throws Exception {
		authorGroupService.deleteAuthorGroup(vo);
		return IntermediateResultVO.success(null);
	}

	// ------------------------------------------------------------------ 내부 유틸

	private PaginationInfo preparePaging(int pageIndex) {
		return EgovPaginationUtil.create(
				pageIndex, propertiesService.getInt("pageUnit"), propertiesService.getInt("pageSize"));
	}
}
