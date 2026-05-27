/* =============================================================================
 * 성적 입력 페이지.
 *  - 페이지 로드 시 학생 목록과 과목 목록을 백엔드에서 가져와 select 박스를 채움
 *  - [입력] 버튼 클릭 시 POST /api/scores
 *  - 같은 학생+과목 중복 입력 시 409 응답 → 확인 다이얼로그 후 PUT 으로 갱신
 *  - 학생을 선택하면 해당 학생의 기존 점수 목록을 표시
 * ============================================================================= */

document.addEventListener("DOMContentLoaded", async () => {
  await loadStudents();
  await loadSubjects();
  document.getElementById("studentSelect").addEventListener("change", refreshScoreList);
});

/** 학생 목록을 가져와 학생 select 박스 채우기 */
async function loadStudents() {
  const res = await api.listStudents();
  const sel = document.getElementById("studentSelect");
  if (res.status !== 200) {
    showMessage("학생 목록을 불러오지 못했습니다.");
    return;
  }
  for (const s of res.data) {
    const opt = document.createElement("option");
    opt.value = s.id;
    opt.textContent = `${s.studentNumber} - ${s.name} (${s.department} ${s.gradeYear}학년)`;
    sel.appendChild(opt);
  }
}

/** 과목 5개를 select 박스에 채움 */
async function loadSubjects() {
  const res = await api.listSubjects();
  const sel = document.getElementById("subjectSelect");
  if (res.status !== 200) {
    showMessage("과목 목록을 불러오지 못했습니다.");
    return;
  }
  for (const sub of res.data) {
    const opt = document.createElement("option");
    opt.value = sub;
    opt.textContent = sub;
    sel.appendChild(opt);
  }
}

/** [입력] 버튼 핸들러 */
async function onSubmit() {
  clearMessage();
  const studentId = document.getElementById("studentSelect").value;
  const subject   = document.getElementById("subjectSelect").value;
  const scoreStr  = document.getElementById("score").value;

  if (!studentId || !subject || scoreStr === "") {
    showMessage("학생/과목/점수를 모두 입력하세요.");
    return;
  }
  const score = parseInt(scoreStr, 10);
  if (isNaN(score) || score < 0 || score > 100) {
    showMessage("점수는 0~100 사이여야 합니다.");
    return;
  }

  const body = { studentId: parseInt(studentId, 10), subject, score };
  const res = await api.createScore(body);

  if (res.status === 201) {
    showMessage("점수가 입력되었습니다.", "success");
    document.getElementById("score").value = "";
    refreshScoreList();
  } else if (res.status === 409) {
    // 중복 입력 → 갱신 다이얼로그
    if (!confirm("이미 입력된 과목입니다. 새 점수로 수정하시겠습니까?")) return;
    // 기존 점수 id 를 알아내기 위해 학생의 점수 목록을 다시 조회
    const scoresRes = await api.getStudentScores(parseInt(studentId, 10));
    if (scoresRes.status !== 200) { showMessage("기존 점수 조회 실패"); return; }
    const existing = scoresRes.data.find(s => s.subject === subject);
    if (!existing) { showMessage("기존 점수를 찾지 못했습니다."); return; }
    const upd = await api.updateScore(existing.id, { score });
    if (upd.status === 200) {
      showMessage("점수가 수정되었습니다.", "success");
      document.getElementById("score").value = "";
      refreshScoreList();
    } else {
      showMessage("수정 실패: " + (upd.data?.message || "서버 오류"));
    }
  } else if (res.status === 400) {
    showMessage("입력 오류: " + (res.data?.message || "필드 확인"));
  } else {
    showMessage("입력 실패: " + (res.data?.message || "서버 오류"));
  }
}

/** 현재 선택된 학생의 점수 목록을 다시 불러와 테이블 갱신 */
async function refreshScoreList() {
  const studentId = document.getElementById("studentSelect").value;
  const tbody = document.getElementById("scoreList");
  if (!studentId) {
    tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">학생을 선택하면 표시됩니다.</td></tr>`;
    return;
  }
  const res = await api.getStudentScores(parseInt(studentId, 10));
  if (res.status !== 200) {
    tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">불러오기 실패</td></tr>`;
    return;
  }
  if (res.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">아직 입력된 점수가 없습니다.</td></tr>`;
    return;
  }
  tbody.innerHTML = res.data.map(s => `
    <tr>
      <td>${escapeHtml(s.subject)}</td>
      <td>${s.score}</td>
      <td><button class="danger small" onclick="onDeleteScore(${s.id})">삭제</button></td>
    </tr>
  `).join("");
}

async function onDeleteScore(id) {
  if (!confirm("이 점수를 삭제하시겠습니까?")) return;
  const res = await api.deleteScore(id);
  if (res.status === 200) {
    showMessage("점수가 삭제되었습니다.", "success");
    refreshScoreList();
  } else {
    showMessage("삭제 실패: " + (res.data?.message || "서버 오류"));
  }
}

function escapeHtml(s) {
  if (s === null || s === undefined) return "";
  return String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}
