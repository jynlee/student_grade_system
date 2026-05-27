/* =============================================================================
 * 학생 등록 페이지 — 등록 폼 + 학생 목록 표시.
 * ============================================================================= */

// 페이지 로드 시 학생 목록 표시
document.addEventListener("DOMContentLoaded", refreshList);

/** 등록 폼의 입력값을 읽어 백엔드에 전송 */
async function onRegister() {
  clearMessage();
  const body = {
    studentNumber: document.getElementById("studentNumber").value.trim(),
    name:          document.getElementById("name").value.trim(),
    department:    document.getElementById("department").value.trim(),
    gradeYear:     parseInt(document.getElementById("gradeYear").value, 10),
  };
  // 프론트단 빠른 검증
  if (!body.studentNumber || !body.name || !body.department) {
    showMessage("학번/이름/학과를 모두 입력하세요.");
    return;
  }

  const res = await api.createStudent(body);
  if (res.status === 201) {
    showMessage("학생이 등록되었습니다.", "success");
    // 입력 폼 초기화
    document.getElementById("studentNumber").value = "";
    document.getElementById("name").value = "";
    document.getElementById("department").value = "";
    document.getElementById("gradeYear").value = "1";
    refreshList();
  } else if (res.status === 409) {
    showMessage("이미 사용 중인 학번입니다.");
  } else if (res.status === 400) {
    showMessage("입력 오류: " + (res.data?.message || "필드를 확인하세요."));
  } else {
    showMessage("등록 실패: " + (res.data?.message || "서버 오류"));
  }
}

/** 학생 목록을 다시 불러와 테이블을 렌더링 */
async function refreshList() {
  const tbody = document.getElementById("studentList");
  const res = await api.listStudents();
  if (res.status !== 200) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">불러오기 실패</td></tr>`;
    return;
  }
  const students = res.data;
  if (!students || students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">등록된 학생이 없습니다.</td></tr>`;
    return;
  }
  tbody.innerHTML = students.map(s => `
    <tr>
      <td>${escapeHtml(s.studentNumber)}</td>
      <td>${escapeHtml(s.name)}</td>
      <td>${escapeHtml(s.department)}</td>
      <td>${s.gradeYear}학년</td>
      <td><button class="danger small" onclick="onDelete(${s.id}, '${escapeHtml(s.name)}')">삭제</button></td>
    </tr>
  `).join("");
}

/** 학생 삭제 — 확인 다이얼로그 후 호출, CASCADE 로 성적도 함께 삭제됨을 안내 */
async function onDelete(id, name) {
  if (!confirm(`'${name}' 학생을 삭제하시겠습니까?\n(해당 학생의 모든 성적도 함께 삭제됩니다)`)) return;
  const res = await api.deleteStudent(id);
  if (res.status === 200) {
    showMessage("삭제되었습니다.", "success");
    refreshList();
  } else {
    showMessage("삭제 실패: " + (res.data?.message || "서버 오류"));
  }
}

/** HTML 인젝션 방지 (XSS 방어) */
function escapeHtml(s) {
  if (s === null || s === undefined) return "";
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
