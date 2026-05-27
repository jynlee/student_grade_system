/* =============================================================================
 * 결과 조회 페이지.
 *  - 학생 select 박스에서 학생을 고른 뒤 [조회] 버튼 → GET /api/reports/{studentId}
 *  - 학생 정보 / 과목별 점수·등급 표 / 평균·전체 등급 표시
 *  - 점수가 0개인 학생은 등급 "-" 로 표시
 * ============================================================================= */

document.addEventListener("DOMContentLoaded", loadStudents);

async function loadStudents() {
  const res = await api.listStudents();
  const sel = document.getElementById("studentSelect");
  if (res.status !== 200) { showMessage("학생 목록을 불러오지 못했습니다."); return; }
  for (const s of res.data) {
    const opt = document.createElement("option");
    opt.value = s.id;
    opt.textContent = `${s.studentNumber} - ${s.name}`;
    sel.appendChild(opt);
  }
}

async function onSearch() {
  clearMessage();
  const studentId = document.getElementById("studentSelect").value;
  const area = document.getElementById("reportArea");
  if (!studentId) { showMessage("학생을 선택하세요."); return; }

  const res = await api.getReport(parseInt(studentId, 10));
  if (res.status !== 200) {
    area.innerHTML = "";
    showMessage("조회 실패: " + (res.data?.message || "서버 오류"));
    return;
  }

  const r = res.data;
  // 입력된 과목 수 / 전체 5과목
  const inputCount = r.subjectScores.length;
  const totalSubjects = 5;

  // 평균·등급 표시 — null 처리
  const avgStr = (r.average === null || r.average === undefined) ? "-" : r.average.toFixed(1);
  const gradeStr = r.overallGrade || "-";
  const gradeBadge = gradeStr === "-" ? "-" : `<span class="grade grade-${gradeStr}">${gradeStr}</span>`;

  area.innerHTML = `
    <div class="card">
      <h3>학생 정보</h3>
      <div class="summary">
        <div>학번: <span>${escapeHtml(r.student.studentNumber)}</span></div>
        <div>이름: <span>${escapeHtml(r.student.name)}</span></div>
        <div>학과: <span>${escapeHtml(r.student.department)}</span></div>
        <div>학년: <span>${r.student.gradeYear}학년</span></div>
      </div>

      <h3>과목별 성적 (입력 ${inputCount}/${totalSubjects})</h3>
      ${inputCount === 0 ? `
        <p class="text-muted">아직 입력된 성적이 없습니다.</p>
      ` : `
        <table>
          <thead><tr><th>과목</th><th>점수</th><th>등급</th></tr></thead>
          <tbody>
            ${r.subjectScores.map(s => `
              <tr>
                <td>${escapeHtml(s.subject)}</td>
                <td>${s.score}</td>
                <td><span class="grade grade-${s.grade}">${s.grade}</span></td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      `}

      <h3>전체 결과</h3>
      <div class="summary">
        <div>평균: <span>${avgStr}</span></div>
        <div>전체 등급: ${gradeBadge}</div>
      </div>
    </div>
  `;
}

function escapeHtml(s) {
  if (s === null || s === undefined) return "";
  return String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}
